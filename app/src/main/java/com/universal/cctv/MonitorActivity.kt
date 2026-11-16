
package com.universal.cctv

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MonitorActivity : Activity() {

    private var selectedCamera = 1
    private var cameraCount = 16
    private var fullScreen = false

    private lateinit var cameraArea: LinearLayout
    private lateinit var status: TextView

    private val orange = Color.rgb(224, 105, 20)
    private val cream = Color.rgb(255, 248, 240)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = orange
        window.navigationBarColor = orange

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            setBackgroundColor(cream)
        }

        val header = TextView(this).apply {
            text = "UNIVERSAL CCTV | LIVE MONITOR"
            textSize = 19f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(8, 15, 8, 15)
            setBackgroundColor(orange)
        }

        root.addView(header)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        listOf(1, 4, 9, 16).forEach { count ->

            val button = Button(this).apply {
                text = "$count CAM"
                textSize = 11f
                isAllCaps = false

                setOnClickListener {
                    cameraCount = count
                    selectedCamera = 1
                    fullScreen = false
                    drawCameras()
                }
            }

            controls.addView(
                button,
                LinearLayout.LayoutParams(0, 48, 1f)
            )
        }

        root.addView(controls)

        status = TextView(this).apply {
            text = "SELECT CAMERA TO ENLARGE"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(orange)
            setPadding(5, 8, 5, 8)
        }

        root.addView(status)

        cameraArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(
            cameraArea,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val bottom = Button(this).apply {
            text = "FULL SCREEN / BACK TO GRID"
            isAllCaps = false

            setOnClickListener {
                fullScreen = !fullScreen
                drawCameras()
            }
        }

        root.addView(bottom)

        setContentView(root)

        drawCameras()
    }

    private fun drawCameras() {

        cameraArea.removeAllViews()

        if (fullScreen) {

            cameraArea.addView(
                cameraTile(selectedCamera, true),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            status.text = "CAMERA $selectedCamera FULL SCREEN"
            return
        }

        if (cameraCount == 1) {

            cameraArea.addView(
                cameraTile(1, true),
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            status.text = "CAMERA 1"
            return
        }

        val selected = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        selected.addView(
            cameraTile(selectedCamera, true),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                3f
            )
        )

        val thumbnails = HorizontalScrollView(this)

        val thumbRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        for (i in 1..cameraCount) {

            if (i != selectedCamera) {

                thumbRow.addView(
                    cameraTile(i, false),
                    LinearLayout.LayoutParams(
                        dp(125),
                        dp(90)
                    ).apply {
                        setMargins(4, 4, 4, 4)
                    }
                )
            }
        }

        thumbnails.addView(thumbRow)

        selected.addView(
            thumbnails,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        cameraArea.addView(
            selected,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        status.text = "$cameraCount CAMERAS | SELECTED: $selectedCamera"
    }

    private fun cameraTile(
        number: Int,
        large: Boolean
    ): TextView {

        return TextView(this).apply {

            text = "CAMERA $number\n\nNO SIGNAL"
            textSize = if (large) 19f else 11f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD

            background = GradientDrawable().apply {
                setColor(Color.rgb(32, 32, 32))
                cornerRadius = dp(5).toFloat()

                setStroke(
                    dp(2),
                    if (number == selectedCamera)
                        orange else Color.DKGRAY
                )
            }

            isFocusable = true
            isClickable = true

            setOnClickListener {
                selectedCamera = number
                fullScreen = false
                drawCameras()
            }

            setOnLongClickListener {
                selectedCamera = number
                fullScreen = true
                drawCameras()
                true
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (fullScreen) {
            fullScreen = false
            drawCameras()
        } else {
            super.onBackPressed()
        }
    }
}
