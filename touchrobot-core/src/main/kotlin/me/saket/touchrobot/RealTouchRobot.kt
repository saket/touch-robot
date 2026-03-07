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

  private val hostView: View by lazy(NONE) {
    // Find the root view so that overlays can also be targeted.
    hostView.rootView
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

  override suspend fun performGesture(block: suspend TouchRobotGestureScope.() -> Unit) {
    hostView.awaitLayout()

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
      dispatcher = touchDispatcher,
    )
    block(scope)
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
