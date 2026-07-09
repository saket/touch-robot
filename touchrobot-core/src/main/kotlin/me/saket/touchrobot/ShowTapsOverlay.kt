package me.saket.touchrobot

import android.content.Context
import android.graphics.PixelFormat
import android.view.MotionEvent
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.map

/**
 * Mimics the "Show taps" settings in Android's developer settings by drawing a visual feedback
 * for touch events. Used automatically by [TouchRobot].
 */
@Composable
internal fun ShowTapsOverlay(touchRobot: TouchRobot) {
  val state by remember(touchRobot) {
    touchRobot.events.map {
      if (it == null) {
        TapState(isPressed = false, positions = emptyList())
      } else {
        TapState(
          isPressed = it.isPressed(),
          positions = (0 until it.pointerCount)
            .filterNot { pointerIndex ->
              // If this is a pointer up, exclude the lifted pointer.
              it.actionMasked == MotionEvent.ACTION_POINTER_UP && pointerIndex == it.actionIndex
            }
            .map { pointerIndex ->
              Offset(it.getX(pointerIndex), it.getY(pointerIndex))
            }
        )
      }
    }
  }.collectAsState(initial = TapState(isPressed = false, positions = emptyList()))

  // This assumes that all pointers start at the same time,
  // which works for now. Can change this later if needed.
  val radius = animateDpAsState(
    targetValue = if (state.isPressed) 20.dp else 24.dp,
    animationSpec = tween(200),
  )
  val alpha = animateFloatAsState(
    targetValue = if (state.isPressed) 1f else 0f,
    animationSpec = if (state.isPressed) snap() else tween(100, delayMillis = 100),
  )
  val strokeWidth = animateDpAsState(
    targetValue = if (state.isPressed) 4.dp else 2.dp,
    animationSpec = tween(200),
  )

  // todo: how do we ensure this is always on top of everything else?
  //  when Modifier.zoomablePeekOverlay() from telephoto is used, it hides taps.
  MatchParentSizePopup {
    // When paparazzi uses RenderingMode.SHRINK, the incoming layout constraint
    // doesn't wrap the content width. Instead, it uses the width of the device
    // config. Modifier.matchSize() works around this.
    Canvas(Modifier.matchSize(LocalView.current)) {
      state.positions.forEach { position ->
        drawCircle(
          color = Color.White.copy(alpha = alpha.value * 0.75f),
          center = position,
          radius = radius.value.toPx(),
        )
        drawCircle(
          color = Color(0xFF0099FF).copy(alpha = alpha.value),
          center = position,
          radius = radius.value.toPx(),
          style = Stroke(width = strokeWidth.value.toPx()),
        )
      }
    }
  }
}

@Composable
private fun Modifier.matchSize(view: View): Modifier {
  var hostViewSize: IntSize by remember {
    mutableStateOf(IntSize(view.width, view.height))
  }
  LaunchedEffect(Unit) {
    view.doOnEveryLayout {
      hostViewSize = IntSize(it.width, it.height)
    }
  }

  return with(LocalDensity.current) {
    size(
      width = hostViewSize.width.toDp(),
      height = hostViewSize.height.toDp(),
    )
  }
}

private data class TapState(
  /**
   * Note to self: `isPressed` must be separate from `positions.isNotEmpty()` because on `ACTION_UP`,
   * the lifted finger's position is still present in the event (and in `positions`). We need
   * `positions` to know _where_ to animate the disappearance, but `isPressed` to know _whether_
   * to show the pressed vs released state.
   */
  val isPressed: Boolean,
  val positions: List<Offset>,
)

private fun MotionEvent.isPressed(): Boolean {
  return actionMasked != MotionEvent.ACTION_UP && actionMasked != MotionEvent.ACTION_CANCEL
}

/**
 * `doOnLayout` runs only once at the beginning; `doOnEveryLayout` runs every time.
 */
private inline fun View.doOnEveryLayout(crossinline action: (view: View) -> Unit) {
  val listener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ -> action(view) }

  if (isAttachedToWindow) {
    addOnLayoutChangeListener(listener)
  }

  addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
    override fun onViewDetachedFromWindow(v: View) {
      removeOnLayoutChangeListener(listener)
    }

    override fun onViewAttachedToWindow(v: View) {
      addOnLayoutChangeListener(listener)
    }
  })
}

/**
 * An overlay that matches the host view's size. Additionally, this was also written because neither
 * Popup() nor Dialog() were working as expected with paparazzi at the time of writing.
 */
@Composable
internal fun MatchParentSizePopup(
  content: @Composable () -> Unit,
) {
  val hostView = LocalView.current
  val compositionContext = rememberCompositionContext()

  DisposableEffect(Unit) {
    val popupLayout = ComposeView(hostView.context).apply {
      id = android.R.id.content
      setViewTreeLifecycleOwner(hostView.findViewTreeLifecycleOwner())
      setViewTreeSavedStateRegistryOwner(hostView.findViewTreeSavedStateRegistryOwner())
      setParentCompositionContext(compositionContext)
      setContent(content)
    }

    val windowManager = hostView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val layoutParams = WindowManager.LayoutParams().also {
      it.width = if (hostView.width == 0) ViewGroup.LayoutParams.MATCH_PARENT else hostView.width
      it.height = if (hostView.height == 0) ViewGroup.LayoutParams.WRAP_CONTENT else hostView.height
      it.format = PixelFormat.TRANSLUCENT
    }
    windowManager.addView(popupLayout, layoutParams)

    hostView.doOnEveryLayout {
      layoutParams.width = it.width
      layoutParams.height = it.height
      windowManager.updateViewLayout(popupLayout, layoutParams)
    }

    onDispose {
      popupLayout.disposeComposition()
      windowManager.removeViewImmediate(popupLayout)
    }
  }
}
