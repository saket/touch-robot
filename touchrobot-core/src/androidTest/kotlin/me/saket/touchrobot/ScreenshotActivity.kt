package me.saket.touchrobot

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.doOnNextLayout

class ScreenshotActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    window.setBackgroundDrawable(ColorDrawable(Color.DarkGray.toArgb()))
    super.onCreate(savedInstanceState)

    window.decorView.doOnNextLayout {
      check(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { "not portrait?" }
      check(Build.VERSION.SDK_INT == 34 && it.width == 1080 && it.height == 2400) {
        "TouchRobot's test screenshots were generated on an API 34 device with a 1080 x 2400 display/window size. " +
          "Current device: API ${Build.VERSION.SDK_INT}, window size = ${it.width} x ${it.height}"
      }
    }
  }
}
