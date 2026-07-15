package me.saket.touchrobot

import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * A robot that simulates touch gestures for UI demonstrations and tests.
 *
 * Example Usage:
 *
 * ```kotlin
 * paparazzi.gif(end = 2_500) {
 *   Content(…)
 *
 *   val touchRobot = rememberTouchRobot()
 *   LaunchedEffect(Unit) {
 *     touchRobot.onRoot().performGesture {
 *       swipe(
 *         start = center,
 *         end = topCenter,
 *         duration = 2.seconds,
 *       )
 *     }
 *   }
 * }
 * ```
 */
@Composable
fun rememberTouchRobot(
  showTaps: Boolean = true,
): TouchRobot {
  val hostView = LocalView.current

  return remember(hostView) {
    RealTouchRobot(hostView)
  }.also { touchRobot ->
    if (showTaps) {
      // Display touch events on the UI. It's probably terrible that a remember
      // function has a side-effect of displaying UI, but this makes it super
      // easy to see how the touch events interact with the UI.
      ShowTapsOverlay(touchRobot)
    }
  }
}

/**
 * A robot that simulates touch events for UI demonstrations and tests.
 *
 * See [rememberTouchRobot] for more details.
 */
interface TouchRobot {
  val events: Flow<MotionEvent?>

  /**
   * Target the entire UI hierarchy for performing gestures.
   *
   * ```
   * touchRobot.onRoot().performGesture {
   *   swipe(
   *     start = centerRight,
   *     end = center,
   *     duration = 1.seconds,
   *   )
   * }
   * ```
   */
  fun onRoot(): TouchRobotTarget

  /**
   * Target a specific region for performing gestures.
   *
   * ```
   * val target = touchRobot.onBounds {
   *   // Find coordinates of the region, relative to the root.
   * }
   * target.performGesture {
   *   swipe(
   *     start = bottomCenter,
   *     end = center,
   *     duration = 1.seconds,
   *   )
   * }
   * ```
   *
   * If you're writing Compose screenshot tests using Paparazzi, you should instead use
   * `onNode()` from the `touchrobot-paparazzi` artifact:
   *
   * ```
   * touchRobot.onNode(hasTestTag("Nicolas Cage")).performGesture {
   *   swipe(
   *     start = bottomCenter,
   *     end = center,
   *     duration = 1.seconds,
   *   )
   * }
   * ```
   */
  fun onBounds(bounds: suspend (hostView: View) -> IntRect): TouchRobotTarget
}

interface TouchRobotTarget {
  suspend fun performGesture(block: suspend TouchRobotGestureScope.() -> Unit)
}

/** Inspired by [TouchInjectionScope][androidx.compose.ui.test.TouchInjectionScope]. */
interface TouchRobotGestureScope : Density {
  /**
   * Layout bounds of the touch target where
   * [performGesture][TouchRobotTarget.performGesture] was called.
   */
  val bounds: IntRect

  val topLeft: IntOffset get() = bounds.topLeft
  val topCenter: IntOffset get() = bounds.topCenter
  val topRight: IntOffset get() = bounds.topRight

  val centerLeft: IntOffset get() = bounds.centerLeft
  val center: IntOffset get() = bounds.center
  val centerRight: IntOffset get() = bounds.centerRight

  val bottomLeft: IntOffset get() = bounds.bottomLeft
  val bottomCenter: IntOffset get() = bounds.bottomCenter
  val bottomRight: IntOffset get() = bounds.bottomRight

  val viewConfiguration: ViewConfiguration

  fun down(pointerId: PointerId, position: IntOffset)

  fun down(position: IntOffset) {
    down(PointerId(0), position)
  }

  fun up(pointerId: PointerId)

  fun up() {
    up(PointerId(0))
  }

  suspend fun click(position: IntOffset = center)

  suspend fun longClick(position: IntOffset = center)

  suspend fun swipe(
    start: IntOffset,
    stop: IntOffset,
    duration: Duration,
  )

  /**
   * Perform a freehand gesture using a continuous touch that follows the given [path].
   * Useful for handwriting, signatures, or custom shape gestures.
   */
  suspend fun draw(
    path: Path,
    duration: Duration,
  )

  /**
   * Animate [pointerId] from its current position to [position] over [duration], dispatching
   * `ACTION_MOVE` events on each frame (with the same easing as [draw]).
   *
   * Unlike [swipe] and [draw], this does not send `ACTION_DOWN` or `ACTION_UP` events — it
   * continues an already-active gesture. This makes it the building block for composing
   * multi-phase gestures such as long-press → drag → hold → drag → release:
   *
   * ```
   * touchRobot.onRoot().performGesture {
   *   down(start)
   *   delay(viewConfiguration.longPressTimeoutMillis + 100.milliseconds)
   *   moveTo(dwell, 700.milliseconds)
   *   delay(5.seconds)
   *   moveTo(drop, 700.milliseconds)
   *   up()
   * }
   * ```
   *
   * Requires a prior [down] for [pointerId]. Use [kotlinx.coroutines.delay] to keep the
   * pointer stationary between calls.
   */
  suspend fun moveTo(
    position: IntOffset,
    duration: Duration,
    pointerId: PointerId = PointerId(0),
  )

  /**
   * Perform a pinch gesture from [start0, start1] to [end0, end1].
   *
   * ```
   * touchRobot.onRoot().performGesture {
   *   val startOffset = IntOffset(100, 100)
   *   val endOffset = IntOffset(300, 300)
   *
   *   // Pinch out (zoom in) — fingers spread symmetrically from center.
   *   pinch(
   *     start0 = center - startOffset,
   *     start1 = center + startOffset,
   *     end0 = center - endOffset,
   *     end1 = center + endOffset,
   *     duration = 1.seconds,
   *   )
   * }
   * ```
   *
   * Pro-tip: keep a meaningful distance between the two fingers. Just like real fingers can't
   * touch the exact same pixel on a screen, placing them too close produces unrealistic gestures.
   */
  suspend fun pinch(
    start0: IntOffset,
    end0: IntOffset,
    start1: IntOffset,
    end1: IntOffset,
    duration: Duration,
  )
}
