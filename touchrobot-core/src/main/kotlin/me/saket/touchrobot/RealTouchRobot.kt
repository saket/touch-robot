package me.saket.touchrobot

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.ui.platform.AndroidViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.core.view.doOnLayout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

internal class RealTouchRobot(
  hostView: View,
) : TouchRobot {

  private val hostRootView: View by lazy(NONE) {
    hostView.rootView
  }

  override val events = MutableSharedFlow<MotionEvent?>(
    replay = 1,
    extraBufferCapacity = 2,
  )

  override fun onRoot(): TouchRobotTarget {
    return onTargetBounds { hostRootView ->
      TouchRobotTarget.Bounds(
        windowRootView = hostRootView,
        bounds = IntRect(0, 0, hostRootView.width, hostRootView.height),
      )
    }
  }

  override fun onTargetBounds(bounds: suspend (hostView: View) -> TouchRobotTarget.Bounds): TouchRobotTarget {
    return RealTouchRobotTarget(hostRootView, bounds, events)
  }

  @Suppress("OVERRIDE_DEPRECATION")
  override fun onBounds(bounds: suspend (hostView: View) -> IntRect): TouchRobotTarget {
    return onTargetBounds { hostRootView ->
      TouchRobotTarget.Bounds(hostRootView, bounds(hostRootView))
    }
  }
}

private class RealTouchRobotTarget(
  private val hostRootView: View,
  private val targetBounds: suspend (hostView: View) -> TouchRobotTarget.Bounds,
  private val events: MutableSharedFlow<MotionEvent?>,
) : TouchRobotTarget {

  override suspend fun performGesture(block: suspend TouchRobotGestureScope.() -> Unit) {
    hostRootView.awaitLayout()
    val targetBounds = targetBounds(hostRootView)
    val targetRootView = targetBounds.windowRootView
    targetRootView.awaitLayout()

    val locationBuffer = IntArray(2)
    val scope = RealTouchRobotGestureScope(
      // Deflate the bounds by 1px so that touch events always fall _inside_ the touch target.
      bounds = targetBounds.bounds.deflate(1),
      viewConfiguration = AndroidViewConfiguration(
        ViewConfiguration.get(targetRootView.context),
      ),
      density = Density(
        density = targetRootView.resources.displayMetrics.density,
        fontScale = targetRootView.resources.configuration.fontScale,
      ),
      dispatcher = MotionEventDispatcher { event ->
        val eventForTapOverlay = event.copyWithOffsetRelativeTo(
          source = targetRootView,
          destination = hostRootView,
          locationBuffer = locationBuffer,
        )
        events.tryEmit(eventForTapOverlay)
        targetRootView.dispatchTouchEvent(event)
      },
    )
    block(scope)
  }

  private suspend fun View.awaitLayout() {
    if (isLaidOut) {
      return
    }
    try {
      withTimeout(1.seconds) {
        suspendCancellableCoroutine<Unit> { continuation ->
          doOnLayout {
            continuation.resume(Unit)
          }
        }
      }
    } catch (e: TimeoutCancellationException) {
      throw RuntimeException("Timed out waiting for view to be laid out", e)
    }
  }
}

private fun MotionEvent.copyWithOffsetRelativeTo(
  source: View,
  destination: View,
  locationBuffer: IntArray,
): MotionEvent {
  if (source === destination) {
    return this
  }
  source.getLocationOnScreen(locationBuffer)
  val sourceX = locationBuffer[0]
  val sourceY = locationBuffer[1]

  destination.getLocationOnScreen(locationBuffer)
  val feedbackEvent = MotionEvent.obtain(this)
  feedbackEvent.offsetLocation(
    (sourceX - locationBuffer[0]).toFloat(),
    (sourceY - locationBuffer[1]).toFloat(),
  )
  return feedbackEvent
}
