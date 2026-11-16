
package com.universal.cctv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private val orange = Color.rgb(224, 105, 20)
    private val darkOrange = Color.rgb(183, 77, 8)
    private val cream = Color.rgb(255, 248, 240)
    private val dark = Color.rgb(42, 39, 36)

    private var player: ExoPlayer? = null

    private lateinit var playerView: PlayerView
    private lateinit var rtspInput: EditText
    private lateinit var status: TextView
    private lateinit var deviceList: LinearLayout

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = darkOrange
        window.navigationBarColor = darkOrange

        val scroll = ScrollView(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(16))
            setBackgroundColor(cream)
        }

        // HEADER

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(orange, 18)
        }

        val title = TextView(this).apply {
            text = "UNIVERSAL CCTV"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        val subtitle = TextView(this).apply {
            text = "SMART SECURITY CAMERA VIEWER"
            textSize = 12f
            setTextColor(Color.WHITE)
            letterSpacing = 0.08f
        }

        header.addView(title)
        header.addView(subtitle)

        root.addView(header, matchWrap())

        // CAMERA CONNECTION

        root.addView(sectionTitle("CAMERA CONNECTION"))

        val options = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val onvifCard = createCard(
            "◎",
            "ONVIF SEARCH",
            "Find DVR / NVR"
        ) {
            searchDevices()
        }

        val rtspCard = createCard(
            "↔",
            "MANUAL RTSP",
            "Add Camera Manually"
        ) {
            rtspInput.requestFocus()
        }

        options.addView(
            onvifCard,
            LinearLayout.LayoutParams(0, dp(125), 1f).apply {
                marginEnd = dp(7)
            }
        )

        options.addView(
            rtspCard,
            LinearLayout.LayoutParams(0, dp(125), 1f).apply {
                marginStart = dp(7)
            }
        )

        root.addView(options)

        // LIVE MONITOR BUTTON

        val monitorButton = Button(this).apply {
            text = "OPEN LIVE MONITOR"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(orange, 12)

            setOnClickListener {
                startActivity(
                    Intent(
                        this@MainActivity,
                        MonitorActivity::class.java
                    )
                )
            }
        }

        root.addView(
            monitorButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            ).apply {
                topMargin = dp(12)
            }
        )

        // DEVICE STATUS

        root.addView(sectionTitle("DEVICE STATUS"))

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.WHITE, 14, orange)
        }

        status = TextView(this).apply {
            text = "● READY TO CONNECT"
            textSize = 15f
            setTextColor(darkOrange)
            typeface = Typeface.DEFAULT_BOLD
        }

        val info = TextView(this).apply {
            text = "Connect your DVR / NVR to the same Wi-Fi or LAN."
            textSize = 12f
            setTextColor(dark)
            setPadding(0, dp(5), 0, dp(5))
        }

        deviceList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        statusCard.addView(status)
        statusCard.addView(info)
        statusCard.addView(deviceList)

        root.addView(statusCard, matchWrap())

        // MANUAL RTSP

        root.addView(sectionTitle("MANUAL RTSP CONNECTION"))

        val rtspCardBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(14))
            background = rounded(Color.WHITE, 14, orange)
        }

        rtspInput = EditText(this).apply {
            hint = "rtsp://username:password@IP:554/path"
            textSize = 14f
            setTextColor(dark)
            setHintTextColor(Color.GRAY)
            setSingleLine(true)
            isFocusable = true
        }

        val connectButton = Button(this).apply {
            text = "CONNECT CAMERA"
            textSize = 15f
            setTextColor(Color.WHITE)
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(orange, 12)

            setOnClickListener {

                val url = rtspInput.text.toString().trim()

                if (url.startsWith("rtsp://", true)) {
                    playCamera(url)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Enter a valid RTSP URL",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        rtspCardBox.addView(rtspInput, matchWrap())

        rtspCardBox.addView(
            connectButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply {
                topMargin = dp(10)
            }
        )

        root.addView(rtspCardBox, matchWrap())

        // LIVE CAMERA VIEW

        root.addView(sectionTitle("LIVE CAMERA VIEW"))

        val videoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), dp(7), dp(7), dp(7))
            background = rounded(Color.WHITE, 14, orange)
        }

        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(Color.BLACK)
        }

        videoCard.addView(
            playerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(230)
            )
        )

        val videoInfo = TextView(this).apply {
            text = "CAMERA PREVIEW • WAITING FOR CONNECTION"
            textSize = 11f
            setTextColor(darkOrange)
            gravity = Gravity.CENTER
            setPadding(0, dp(9), 0, dp(5))
        }

        videoCard.addView(videoInfo)

        root.addView(videoCard, matchWrap())

        // FOOTER

        val footer = TextView(this).apply {
            text = "UNIVERSAL CCTV VIEWER"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(darkOrange)
            setPadding(0, dp(18), 0, dp(8))
            typeface = Typeface.DEFAULT_BOLD
        }

        root.addView(footer)

        scroll.addView(root)
        setContentView(scroll)
    }

    // SECTION TITLE

    private fun sectionTitle(value: String): TextView {
        return TextView(this).apply {
            text = value
            textSize = 16f
            setTextColor(darkOrange)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), dp(17), dp(4), dp(9))
        }
    }

    // OPTION CARD

    private fun createCard(
        symbol: String,
        heading: String,
        description: String,
        action: () -> Unit
    ): LinearLayout {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            background = rounded(Color.WHITE, 14, orange)
            setPadding(dp(5), dp(8), dp(5), dp(8))
        }

        val icon = TextView(this).apply {
            text = symbol
            textSize = 31f
            setTextColor(orange)
            gravity = Gravity.CENTER
        }

        val name = TextView(this).apply {
            text = heading
            textSize = 14f
            setTextColor(darkOrange)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val desc = TextView(this).apply {
            text = description
            textSize = 11f
            setTextColor(dark)
            gravity = Gravity.CENTER
        }

        card.addView(icon)
        card.addView(name)
        card.addView(desc)

        card.setOnFocusChangeListener { view, focused ->

            view.background = rounded(
                if (focused) orange else Color.WHITE,
                14,
                if (focused) darkOrange else orange
            )

            name.setTextColor(
                if (focused) Color.WHITE else darkOrange
            )

            icon.setTextColor(
                if (focused) Color.WHITE else orange
            )

            desc.setTextColor(
                if (focused) Color.WHITE else dark
            )
        }

        card.setOnClickListener { action() }

        return card
    }

    // ONVIF SEARCH

    private fun searchDevices() {

        status.text = "● SEARCHING ONVIF DEVICES..."
        deviceList.removeAllViews()

        executor.execute {

            val devices = discoverOnvif()

            runOnUiThread {

                deviceList.removeAllViews()

                if (devices.isEmpty()) {

                    status.text = "● NO DEVICE FOUND"

                } else {

                    status.text = "● FOUND ${devices.size} DEVICE(S)"

                    devices.forEachIndexed { index, device ->

                        val item = TextView(this).apply {
                            text = "DEVICE ${index + 1}\n${device.first}\n${device.second}"
                            textSize = 12f
                            setTextColor(dark)
                            setPadding(dp(8), dp(10), dp(8), dp(10))
                            background = rounded(cream, 8, orange)
                        }

                        deviceList.addView(
                            item,
                            matchWrap().apply {
                                bottomMargin = dp(6)
                            }
                        )
                    }
                }
            }
        }
    }

    // ONVIF DISCOVERY

    private fun discoverOnvif(): List<Pair<String, String>> {

        val results = linkedMapOf<String, String>()
        var lock: WifiManager.MulticastLock? = null

        try {

            val wifi = applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

            lock = wifi.createMulticastLock("CCTV_ONVIF")
            lock.setReferenceCounted(false)
            lock.acquire()

            DatagramSocket().use { socket ->

                socket.soTimeout = 700

                val probe = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <e:Envelope
                    xmlns:e="http://www.w3.org/2003/05/soap-envelope"
                    xmlns:w="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                    xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery"
                    xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
                    <e:Header>
                    <w:MessageID>urn:uuid:${UUID.randomUUID()}</w:MessageID>
                    <w:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To>
                    <w:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action>
                    </e:Header>
                    <e:Body>
                    <d:Probe>
                    <d:Types>dn:NetworkVideoTransmitter</d:Types>
                    </d:Probe>
                    </e:Body>
                    </e:Envelope>
                """.trimIndent()

                val data = probe.toByteArray(Charsets.UTF_8)

                socket.send(
                    DatagramPacket(
                        data,
                        data.size,
                        InetAddress.getByName("239.255.255.250"),
                        3702
                    )
                )

                val endTime = System.currentTimeMillis() + 8000

                while (System.currentTimeMillis() < endTime) {

                    try {

                        val buffer = ByteArray(16384)
                        val response = DatagramPacket(buffer, buffer.size)

                        socket.receive(response)

                        val xml = String(
                            response.data,
                            response.offset,
                            response.length,
                            Charsets.UTF_8
                        )

                        val xaddr = Regex(
                            "<(?:\\w+:)?XAddrs[^>]*>(.*?)</(?:\\w+:)?XAddrs>",
                            setOf(
                                RegexOption.IGNORE_CASE,
                                RegexOption.DOT_MATCHES_ALL
                            )
                        ).find(xml)?.groupValues?.get(1)?.trim()

                        val scopes = Regex(
                            "<(?:\\w+:)?Scopes[^>]*>(.*?)</(?:\\w+:)?Scopes>",
                            setOf(
                                RegexOption.IGNORE_CASE,
                                RegexOption.DOT_MATCHES_ALL
                            )
                        ).find(xml)?.groupValues?.get(1)?.trim()

                        if (!xaddr.isNullOrBlank()) {
                            results[xaddr] = scopes ?: ""
                        }

                    } catch (_: SocketTimeoutException) {
                    }
                }
            }

        } catch (e: Exception) {

            android.util.Log.e("CCTV", "ONVIF Error", e)

        } finally {

            try {
                if (lock?.isHeld == true) lock.release()
            } catch (_: Exception) {
            }
        }

        return results.map { it.key to it.value }
    }

    // RTSP PLAYER

    private fun playCamera(url: String) {

        try {

            player?.release()

            player = ExoPlayer.Builder(this).build()

            playerView.player = player

            player?.setMediaItem(MediaItem.fromUri(url))
            player?.prepare()
            player?.play()

            status.text = "● CONNECTING TO CAMERA"

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Player Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // DESIGN

    private fun rounded(
        color: Int,
        radius: Int,
        strokeColor: Int = color
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), strokeColor)
        }
    }

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {

        if (::playerView.isInitialized) {
            playerView.player = null
        }

        player?.release()
        player = null

        executor.shutdownNow()

        super.onDestroy()
    }
}
