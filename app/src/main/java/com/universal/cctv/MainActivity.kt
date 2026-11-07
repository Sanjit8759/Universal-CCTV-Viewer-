
package com.universal.cctv

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : Activity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var urlInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        urlInput = EditText(this).apply {
            hint = "Enter RTSP Camera URL"
            singleLine = true
            textSize = 16f
        }

        val playButton = Button(this).apply {
            text = "CONNECT CAMERA"
            setOnClickListener {
                val url = urlInput.text.toString().trim()
                if (url.startsWith("rtsp://")) {
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

        playerView = PlayerView(this).apply {
            useController = true
        }

        layout.addView(urlInput)
        layout.addView(playButton)
        layout.addView(
            playerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(layout)
    }

    private fun playCamera(url: String) {
        player?.release()

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
