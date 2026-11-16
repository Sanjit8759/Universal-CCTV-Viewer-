
package com.universal.cctv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MonitorActivity : Activity() {

    private val orange = Color.rgb(224, 105, 20)
    private val darkOrange = Color.rgb(183, 77, 8)
    private val dark = Color.rgb(19, 23, 27)

    private var selectedCamera = 1
    private var cameraCount = 16
    private var fullScreen = false

    private lateinit var mainArea: FrameLayout
    private lateinit var thumbnailList: LinearLayout
    private lateinit var thumbPanel: LinearLayout
    private lateinit var optionPanel: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = dark
        window.navigationBarColor = dark

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(dark)
        }

        // MAIN CAMERA AREA
        val mainPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(6), dp(4), dp(6))
        }

        val header = TextView(this).apply {
            text = "UNIVERSAL CCTV  |  LIVE MONITOR"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, 0, 0)
            background = rounded(orange, 8)
        }

        mainPanel.addView(
            header,
            LinearLayout.LayoutParams(
                -1, dp(48)
            )
        )

        status = TextView(this).apply {
            text = "CAMERA $selectedCamera  |  NO SIGNAL"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, 0, 0)
        }

        mainPanel.addView(
            status,
            LinearLayout.LayoutParams(-1, dp(34))
        )

        mainArea = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        mainPanel.addView(
            mainArea,
            LinearLayout.LayoutParams(
                -1, 0, 1f
            )
        )

        // CAMERA THUMBNAILS
        thumbPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), dp(5), dp(3), dp(5))
            setBackgroundColor(Color.rgb(35, 38, 42))
        }

        val thumbTitle = TextView(this).apply {
            text = "CAMERAS"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        thumbPanel.addView(
            thumbTitle,
            LinearLayout.LayoutParams(-1, dp(35))
        )

        val thumbScroll = ScrollView(this)

        thumbnailList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        thumbScroll.addView(thumbnailList)

        thumbPanel.addView(
            thumbScroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        // RIGHT OPTIONS PANEL
        optionPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(6), dp(5), dp(6))
            setBackgroundColor(Color.rgb(42, 42, 42))
        }

        val optionTitle = TextView(this).apply {
            text = "OPTIONS"
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        optionPanel.addView(
            optionTitle,
            LinearLayout.LayoutParams(-1, dp(38))
        )

        addOption("ONVIF\nSEARCH") {
            Toast.makeText(
                this,
                "ONVIF Search will be connected next.",
                Toast.LENGTH_SHORT
            ).show()
        }

        addOption("ADD RTSP") {
            Toast.makeText(
                this,
                "RTSP connection will be added next.",
                Toast.LENGTH_SHORT
            ).show()
        }

        addOption("1 CAMERA") {
            changeLayout(1)
        }

        addOption("4 CAMERAS") {
            changeLayout(4)
        }

        addOption("9 CAMERAS") {
            changeLayout(9)
        }

        addOption("16 CAMERAS") {
            changeLayout(16)
        }

        addOption("FULL SCREEN") {
            fullScreen = !fullScreen
            updateScreen()
        }

        addOption("BACK") {
            finish()
        }

        // BUILD SCREEN
        root.addView(
            mainPanel,
            LinearLayout.LayoutParams(0, -1, 1f)
        )

        root.addView(
            thumbPanel,
            LinearLayout.LayoutParams(dp(105), -1)
        )

        root.addView(
            optionPanel,
            LinearLayout.LayoutParams(dp(112), -1)
        )

        setContentView(root)

        updateScreen()
    }

    private fun addOption(
        name: String,
        action: () -> Unit
    ) {

        val button = TextView(this).apply {
            text = name
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            isFocusable = true
            isClickable = true

            background = rounded(orange, 7)

            setOnFocusChangeListener { view, focused ->
                view.background = rounded(
                    if (focused) Color.WHITE else orange,
                    7
                )

                setTextColor(
                    if (focused) darkOrange else Color.WHITE
                )
            }

            setOnClickListener {
                action()
            }
        }

        optionPanel.addView(
            button,
            LinearLayout.LayoutParams(
                -1, dp(55)
            ).apply {
                setMargins(0, dp(4), 0, dp(4))
            }
        )
    }

    private fun changeLayout(count: Int) {
        cameraCount = count

        if (selectedCamera > count) {
            selectedCamera = 1
        }

        fullScreen = false
        updateScreen()
    }

    private fun updateScreen() {

        mainArea.removeAllViews()
        thumbnailList.removeAllViews()

        thumbPanel.visibility =
            if (fullScreen) View.GONE else View.VISIBLE

        optionPanel.visibility =
            if (fullScreen) View.GONE else View.VISIBLE

        // LARGE CAMERA DISPLAY
        val preview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
        }

        val cameraIcon = TextView(this).apply {
            text = "◉"
            textSize = 48f
            gravity = Gravity.CENTER
            setTextColor(orange)
        }

        val cameraText = TextView(this).apply {
            text = "CAMERA $selectedCamera"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        val noSignal = TextView(this).apply {
            text = "NO CAMERA CONNECTED"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
        }

        preview.addView(cameraIcon)
        preview.addView(cameraText)
        preview.addView(noSignal)

        mainArea.addView(
            preview,
            FrameLayout.LayoutParams(-1, -1)
        )

        status.text =
            "CAMERA $selectedCamera  |  NO SIGNAL  |  $cameraCount CH"

        // SMALL CAMERA THUMBNAILS
        if (!fullScreen && cameraCount > 1) {

            for (i in 1..cameraCount) {

                val tile = TextView(this).apply {
                    text = if (i == selectedCamera) {
                        "▶ CAM $i"
                    } else {
                        "CAM $i"
                    }

                    textSize = 12f
                    gravity = Gravity.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)

                    background = rounded(
                        if (i == selectedCamera) orange
                        else Color.rgb(65, 65, 65),
                        6
                    )

                    isFocusable = true
                    isClickable = true

                    setOnClickListener {
                        selectedCamera = i
                        updateScreen()
                    }

                    setOnLongClickListener {
                        selectedCamera = i
                        fullScreen = true
                        updateScreen()
                        true
                    }
                }

                thumbnailList.addView(
                    tile,
                    LinearLayout.LayoutParams(
                        -1, dp(48)
                    ).apply {
                        setMargins(0, dp(3), 0, dp(3))
                    }
                )
            }
        }
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
