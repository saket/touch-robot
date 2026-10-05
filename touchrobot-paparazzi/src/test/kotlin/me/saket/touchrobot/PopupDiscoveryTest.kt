package me.saket.touchrobot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams.RenderingMode.NORMAL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class PopupDiscoveryTest {
  @get:Rule val paparazzi = Paparazzi(
    renderingMode = NORMAL,
  )

  @Test fun `discovers merged popup semantics across its lifecycle`() = discoversPopup(false)

  @Test fun `discovers unmerged popup semantics across its lifecycle`() = discoversPopup(true)

  private fun discoversPopup(useUnmergedTree: Boolean) {
    var completed = false
    paparazzi.gif(end = 2500, fps = 10) {
      var showing by remember { mutableStateOf(false) }
      Box(Modifier.size(100.dp)) {
        BasicText("Host")
        if (showing) {
          Popup {
            Box(Modifier.clickable {}) {
              BasicText("Popup item")
            }
          }
        }
      }
      val robot = rememberTouchRobot(showTaps = false)
      LaunchedEffect(Unit) {
        delay(300.milliseconds)
        robot.onNode(hasText("Host")).performGesture {}
        assertMissing(robot, useUnmergedTree)
        showing = true
        delay(300.milliseconds)
        robot.onNode(hasText("Popup item"), useUnmergedTree).performGesture {}
        // Searching another window must retain the host's semantics.
        robot.onNode(hasText("Host")).performGesture {}
        showing = false
        delay(300.milliseconds)
        assertMissing(robot, useUnmergedTree)
        showing = true
        delay(300.milliseconds)
        robot.onNode(hasText("Popup item"), useUnmergedTree).performGesture {}
        completed = true
      }
    }
    assertTrue("The entire discovery script completed", completed)
  }

  private suspend fun assertMissing(robot: TouchRobot, useUnmergedTree: Boolean) {
    val failure = try {
      robot.onNode(hasText("Popup item"), useUnmergedTree).performGesture {}
      null
    } catch (e: CancellationException) {
      throw e
    } catch (e: IllegalStateException) {
      e
    }
    assertEquals(
      "Timed out waiting for node that matches: ${hasText("Popup item").description}",
      failure?.message,
    )
  }
}
