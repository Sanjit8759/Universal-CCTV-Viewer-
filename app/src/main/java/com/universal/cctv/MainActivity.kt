
package com.universal.cctv

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
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

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var rtspInput: EditText
    private lateinit var status: TextView
    private lateinit var deviceList: LinearLayout

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            setBackgroundColor(0xFF101820.toInt())
        }

        val title = TextView(this).apply {
            text = "UNIVERSAL CCTV VIEWER"
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
        }
        root.addView(title)

        val searchButton = Button(this).apply {
            text = "SEARCH ONVIF DEVICES"
            setOnClickListener {
                searchDevices()
            }
        }
        root.addView(searchButton)

        status = TextView(this).apply {
            text = "Connect DVR/NVR and phone to same Wi-Fi or LAN."
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(4, 8, 4, 8)
        }
        root.addView(status)

        val scroll = ScrollView(this)

        deviceList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(deviceList)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                120
            )
        )

        val label = TextView(this).apply {
            text = "MANUAL RTSP CONNECTION"
            textSize = 17f
            setTextColor(0xFF80DEEA.toInt())
            setPadding(0, 8, 0, 4)
        }
        root.addView(label)

        rtspInput = EditText(this).apply {
            hint = "rtsp://username:password@IP:554/path"
            setSingleLine(true)
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFFAAAAAA.toInt())
        }
        root.addView(rtspInput)

        val connectButton = Button(this).apply {
            text = "CONNECT CAMERA"
            setOnClickListener {
                val url = rtspInput.text.toString().trim()

                if (url.startsWith("rtsp://", true)) {
                    playCamera(url)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Enter valid RTSP URL",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        root.addView(connectButton)

        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(0xFF000000.toInt())
        }

        root.addView(
            playerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun searchDevices() {
        status.text = "Searching ONVIF devices..."
        deviceList.removeAllViews()

        executor.execute {
            val devices = discoverOnvif()

            runOnUiThread {
                deviceList.removeAllViews()

                if (devices.isEmpty()) {
                    status.text =
                        "No ONVIF device found. Check Wi-Fi and DVR/NVR ONVIF support."
                } else {
                    status.text = "Found ${devices.size} ONVIF device(s)"

                    devices.forEachIndexed { index, device ->

                        val item = TextView(this).apply {
                            text = "DEVICE ${index + 1}\n${device.first}\n${device.second}"
                            textSize = 13f
                            setTextColor(0xFFFFFFFF.toInt())
                            setPadding(8, 10, 8, 10)
                            isFocusable = true
                        }

                        deviceList.addView(item)
                    }
                }
            }
        }
    }

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
                socket.broadcast = true

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

                val packet = DatagramPacket(
                    data,
                    data.size,
                    InetAddress.getByName("239.255.255.250"),
                    3702
                )

                socket.send(packet)

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
                        // Continue searching
                    }
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("CCTV", "ONVIF Search Error", e)

        } finally {
            try {
                if (lock?.isHeld == true) {
                    lock.release()
                }
            } catch (_: Exception) {
            }
        }

        return results.map { it.key to it.value }
    }

    private fun playCamera(url: String) {
        try {
            player?.release()

            player = ExoPlayer.Builder(this).build()
            playerView.player = player

            player?.setMediaItem(
                MediaItem.fromUri(url)
            )

            player?.prepare()
            player?.play()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Player Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null

        executor.shutdownNow()

        super.onDestroy()
    }
}
