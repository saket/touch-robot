package me.saket.touchrobot

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dropbox.dropshots.Dropshots
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ShowTapsOverlayScreenshotTest {
  @get:Rule val composeRule = createAndroidComposeRule<ScreenshotActivity>()
  @get:Rule val dropshots = Dropshots()

  @Test fun show_taps_overlay_misplaced_when_host_view_does_not_fill_screen() {
    val tapIsDown = CountDownLatch(1)

    composeRule.setContent {
      // Use a nested host so rememberTouchRobot() sees a smaller
      // view than the activity's full-screen ComposeView.
      AndroidView(
        modifier = Modifier.size(300.dp),
        factory = { context ->
          ComposeView(context).apply {
            setContent {
              val touchRobot = rememberTouchRobot(showTaps = true)

              LaunchedEffect(Unit) {
                touchRobot.onNode(composeRule, hasTestTag("host")).performGesture {
                  down(center)
                  tapIsDown.countDown()
                  awaitCancellation()
                }
              }

              Box(
                modifier = Modifier
                  .padding(16.dp)
                  .fillMaxWidth(fraction = 0.5f)
                  .height(300.dp)
                  .testTag("host")
                  .background(Color.LightGray)
                  .padding(32.dp),
                contentAlignment = Alignment.Center,
              ) {
                BasicText(
                  text = "The tap indicator should be centered inside this box",
                  style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    lineHeight = 1.5.em,
                  ),
                )
              }
            }
          }
        }
      )
    }

    check(tapIsDown.await(5L, TimeUnit.SECONDS)) {
      "Timed out waiting for TouchRobot's tap to be pressed."
    }

    // Wait for the tap indicator's animation to settle.
    composeRule.mainClock.advanceTimeBy(1_000)

    composeRule.runOnIdle {
      dropshots.assertSnapshot(
        bitmap = composeRule.activity.captureScreenMinusSystemBars()
      )
    }
  }
}

internal fun Activity.captureScreenMinusSystemBars(): Bitmap {
  val activityRoot = window.decorView
  val activityLocation = IntArray(2).also {
    activityRoot.getLocationOnScreen(it)
  }

  val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()!!

  // Crop out system UI.
  val rootInsets = activityRoot.rootWindowInsets!!.getInsets(WindowInsets.Type.systemBars())
  val crop = Rect(
    /* left = */ activityLocation[0] + rootInsets.left,
    /* top = */ activityLocation[1] + rootInsets.top,
    /* right = */ activityLocation[0] + activityRoot.width - rootInsets.right,
    /* bottom = */ activityLocation[1] + activityRoot.height - rootInsets.bottom,
  )
  return Bitmap.createBitmap(
    /* source = */ screenshot,
    /* x = */ crop.left,
    /* y = */ crop.top,
    /* width = */ crop.width(),
    /* height = */ crop.height(),
  )
}
