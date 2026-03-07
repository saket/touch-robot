package me.saket.touchrobot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach

@Composable
internal fun FreehandDraw(
  strokeColor: Color,
  modifier: Modifier = Modifier,
) {
  val state = remember { FreehandDrawState() }
  Canvas(
    modifier.pointerInput(Unit) {
      detectDragGesturesWithoutSlop(
        onDrag = state::recordPoint,
        onDragEnd = state::completePath,
      )
    }
  ) {
    drawPath(
      path = state.path,
      color = strokeColor,
      style = Stroke(
        width = 8.dp.toPx(),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
        pathEffect = PathEffect.cornerPathEffect(8.dp.toPx()),
      )
    )
  }
}

/**
 * Like [androidx.compose.foundation.gestures.detectDragGestures],
 * but doesn't wait for the gesture to pass the slop threshold.
 */
private suspend fun PointerInputScope.detectDragGesturesWithoutSlop(
  onDrag: (Offset) -> Unit,
  onDragEnd: () -> Unit,
) {
  awaitEachGesture {
    val initialDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    initialDown.consume()
    onDrag(initialDown.position)

    drag(initialDown.id) { change ->
      onDrag(change.position)
      change.consume()
    }
    onDragEnd()
  }
}

@Stable
internal class FreehandDrawState {
  private val activePath = ActivePath()
  private val previousPaths = mutableStateListOf<Path>()

  val path by derivedStateOf {
    Path().also {
      it.addPath(activePath.toPath())
      previousPaths.fastForEach(it::addPath)
    }
  }

  fun recordPoint(newPoint: Offset) {
    activePath.points.add(newPoint)
  }

  fun completePath() {
    previousPaths.add(activePath.toPath())
    activePath.points.clear()
  }

  private data class ActivePath(
    val points: SnapshotStateList<Offset> = mutableStateListOf(),
  ) {
    fun toPath(): Path = Path().apply {
      if (points.size < 2) {
        return@apply
      }
      moveTo(points[0].x, points[0].y)

      if (points.size == 2) {
        lineTo(points[1].x, points[1].y)
        return@apply
      }

      for (i in 1 until points.size) {
        val current = points[i]
        val previous = points[i - 1]
        val midPoint = Offset(
          x = (current.x + previous.x) / 2,
          y = (current.y + previous.y) / 2
        )
        quadraticTo(
          previous.x, previous.y,  // Control point.
          midPoint.x, midPoint.y   // End point.
        )
      }

      lineTo(points.last().x, points.last().y)
    }
  }
}
