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
  dispatchToRootView: Boolean,
) : TouchRobot {

  private val hostView: View by lazy(NONE) {
    if (dispatchToRootView) hostView.rootView else hostView
  }

  override val events = MutableSharedFlow<MotionEvent?>(
    replay = 1,
    extraBufferCapacity = 2,
  )

  override fun onRoot(): TouchRobotTarget {
    return onBounds { hostView ->
      IntRect(0, 0, hostView.rootView.width, hostView.rootView.height)
    }
  }

  override fun onBounds(bounds: suspend (hostView: View) -> IntRect): TouchRobotTarget {
    return RealTouchRobotTarget(
      hostView = hostView,
      targetBounds = bounds,
      touchDispatcher = { event ->
        events.tryEmit(event)
        hostView.dispatchTouchEvent(event)
      }
    )
  }
}

private class RealTouchRobotTarget(
  private val hostView: View,
  private val targetBounds: suspend (hostView: View) -> IntRect,
  private val touchDispatcher: MotionEventDispatcher,
) : TouchRobotTarget {

  override suspend fun performGesture(block: suspend TouchRobotGestureScope.() -> Unit): Boolean {
    hostView.awaitLayout()

    var dispatchWasHandled = false
    val scope = RealTouchRobotGestureScope(
      // Deflate the bounds by 1px so that touch events always fall _inside_ the touch target.
      bounds = targetBounds(hostView).deflate(1),
      viewConfiguration = AndroidViewConfiguration(
        ViewConfiguration.get(hostView.context),
      ),
      density = Density(
        density = hostView.resources.displayMetrics.density,
        fontScale = hostView.resources.configuration.fontScale,
      ),
      dispatcher = { event ->
        touchDispatcher.dispatch(event).also { wasHandled ->
          dispatchWasHandled = dispatchWasHandled || wasHandled
        }
      },
    )
    try {
      block(scope)
    } catch (e: Throwable) {
      scope.cancelGesture()
      throw e
    }
    return dispatchWasHandled
  }

  private suspend fun View.awaitLayout() {
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
