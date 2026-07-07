package me.saket.touchrobot

import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.milliseconds

/**
 * Reproducer: the "show taps" overlay window is misplaced when the host view does not fill
 * the screen.
 *
 * rememberTouchRobot() draws taps in a separate window added via WindowManager.addView().
 * MatchParentSizePopup sizes that window to the host view but leaves
 * WindowManager.LayoutParams.gravity at its default (0). With gravity == 0, WindowManager centers
 * the window inside the screen, so an overlay smaller than the screen is drawn offset from the
 * host view (and therefore offset from the touches, which are injected at the host's top-left).
 *
 * Robolectric does not run WindowManager's window-placement math, so View.getLocationOnScreen()
 * reports (0,0) for the overlay and hides the bug. This test instead reconstructs the on-screen
 * rect with the exact call WindowManager makes to place a window - Gravity.apply(...) - and asserts
 * the overlay lands at the host view's position.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// 480x799 hdpi screen; the 300.dp host is 450x450 px, i.e. smaller than the screen.
@Config(sdk = [35], qualifiers = "w320dp-h533dp-port-hdpi")
class ShowTapsOverlayPositionTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun showTapsOverlayIsAnchoredToHostView() {
    lateinit var hostView: View

    composeTestRule.setContent {
      val view = LocalView.current
      SideEffect { hostView = view }

      val touchRobot = rememberTouchRobot(showTaps = true)
      LaunchedEffect(Unit) {
        touchRobot.onRoot().performGesture {
          swipe(start = topLeft, stop = center, duration = 200.milliseconds)
        }
      }

      // A host view that is smaller than the screen.
      Box(
        Modifier
          .size(300.dp)
          .background(Color.LightGray)
      )
    }
    composeTestRule.waitForIdle()

    val decorView = hostView.rootView
    val (overlayView, overlayParams) = topLevelWindows().first { (view, _) ->
      view !== decorView && view.javaClass.name.contains("ComposeView")
    }

    val screenRect = Rect(0, 0, decorView.width, decorView.height)
    val overlayRect = Rect()
    Gravity.apply(
      overlayParams.gravity,
      if (overlayParams.width >= 0) overlayParams.width else overlayView.width,
      if (overlayParams.height >= 0) overlayParams.height else overlayView.height,
      screenRect,
      overlayParams.x,
      overlayParams.y,
      overlayRect,
    )

    val hostLocation = IntArray(2).also { hostView.getLocationOnScreen(it) }

    assertEquals(
      "Show-taps overlay window is not anchored to the host view. " +
        "Overlay window at (${overlayRect.left}, ${overlayRect.top}) " +
        "size ${overlayRect.width()}x${overlayRect.height()}, " +
        "but host view is at (${hostLocation[0]}, ${hostLocation[1]}) " +
        "size ${hostView.width}x${hostView.height} on a ${screenRect.width()}x${screenRect.height()} screen. " +
        "The overlay LayoutParams gravity is ${overlayParams.gravity} (0 = centered by WindowManager).",
      hostLocation[0] to hostLocation[1],
      overlayRect.left to overlayRect.top,
    )
  }

  /** Reads WindowManagerGlobal's top-level windows via reflection (the API is @hide). */
  private fun topLevelWindows(): List<Pair<View, WindowManager.LayoutParams>> {
    val cls = Class.forName("android.view.WindowManagerGlobal")
    val instance = cls.getMethod("getInstance").invoke(null)
    @Suppress("UNCHECKED_CAST")
    val views = cls.getDeclaredField("mViews")
      .apply { isAccessible = true }.get(instance) as List<View>
    @Suppress("UNCHECKED_CAST")
    val params = cls.getDeclaredField("mParams")
      .apply { isAccessible = true }.get(instance) as List<WindowManager.LayoutParams>
    return views.indices.map { views[it] to params[it] }
  }
}
