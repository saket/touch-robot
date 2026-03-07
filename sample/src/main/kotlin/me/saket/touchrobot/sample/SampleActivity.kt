package me.saket.touchrobot.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.saket.touchrobot.rememberTouchRobot
import kotlinx.coroutines.delay

class SampleActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      AppTheme {
        Text(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
            .padding(16.dp),
          text = "TODO"
        )
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        while (true) {
          delay(500)
          touchRobot.onRoot().performGesture {
            click(center)
          }
        }
      }
    }
  }
}
