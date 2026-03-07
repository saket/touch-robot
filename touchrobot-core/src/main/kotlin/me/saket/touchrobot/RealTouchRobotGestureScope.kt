package me.saket.touchrobot

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import android.graphics.PathMeasure as AndroidPathMeasure

internal class RealTouchRobotGestureScope(
  private val dispatcher: MotionEventDispatcher,
  override val bounds: IntRect,
  override val viewConfiguration: ViewConfiguration,
  density: Density,
) : TouchRobotGestureScope, Density by density {

  private var ongoingGesture: OngoingGesture? = null

  override fun down(pointerId: PointerId, position: IntOffset) {
    val ongoingGesture = ongoingGesture
      ?: OngoingGesture(downTime = SystemClock.uptimeMillis())
        .also { this.ongoingGesture = it }

    require(!ongoingGesture.downPositions.containsKey(pointerId)) {
      "Cannot send a down event when a gesture is already in progress for pointer $pointerId"
    }
    ongoingGesture.downPositions[pointerId] = position

    dispatchTouchEvent(
      action = when (ongoingGesture.downPositions.size) {
        1 -> MotionEvent.ACTION_DOWN
        else -> MotionEvent.ACTION_POINTER_DOWN
      },
      downTime = ongoingGesture.downTime,
      eventTime = SystemClock.uptimeMillis(),
      allPointers = ongoingGesture.downPositions.toList(),
      actionPointerId = pointerId,
    )
  }

  override fun up(pointerId: PointerId) {
    val ongoingGesture = ongoingGesture ?: error("Cannot send an up event when no gesture is in progress")

    require(pointerId in ongoingGesture.downPositions) {
      "Cannot send an up event for pointer $pointerId as it is not active in the current gesture."
    }

    val action = when (ongoingGesture.downPositions.size) {
      1 -> MotionEvent.ACTION_UP
      else -> MotionEvent.ACTION_POINTER_UP
    }

    dispatchTouchEvent(
      action = action,
      downTime = ongoingGesture.downTime,
      eventTime = SystemClock.uptimeMillis(),
      allPointers = ongoingGesture.downPositions.toList(),
      actionPointerId = pointerId,
    )

    ongoingGesture.downPositions.remove(pointerId)
    if (ongoingGesture.downPositions.isEmpty()) {
      this.ongoingGesture = null
    }
  }

  override suspend fun click(position: IntOffset) {
    click(
      position = position,
      duration = 16.milliseconds,
    )
  }

  override suspend fun longClick(position: IntOffset) {
    click(
      position = position,
      duration = (viewConfiguration.longPressTimeoutMillis + 100).milliseconds,
    )
  }

  private suspend fun click(
    position: IntOffset,
    duration: Duration = 16.milliseconds,
  ) {
    down(position)
    delay(duration)
    up()
  }

  override suspend fun swipe(
    start: IntOffset,
    stop: IntOffset,
    duration: Duration,
  ) {
    draw(
      path = Path().apply {
        moveTo(start.x.toFloat(), start.y.toFloat())
        lineTo(stop.x.toFloat(), stop.y.toFloat())
      },
      duration = duration,
    )
  }

  // Note to self: the naming of these params follows the naming in TouchInjectionScope.
  override suspend fun pinch(
    start0: IntOffset,
    end0: IntOffset,
    start1: IntOffset,
    end1: IntOffset,
    duration: Duration
  ) {
    check(start0 != start1 && end0 != end1) {
      "pinch() requires different positions for both fingers. " +
        "Physics says they can't occupy the same point (unless you're paper). Plus, Android doesn't like it either."
    }

    draw(
      paths = listOf(
        Path().apply {
          moveTo(start0.x.toFloat(), start0.y.toFloat())
          lineTo(end0.x.toFloat(), end0.y.toFloat())
        },
        Path().apply {
          moveTo(start1.x.toFloat(), start1.y.toFloat())
          lineTo(end1.x.toFloat(), end1.y.toFloat())
        },
      ),
      duration = duration,
    )
  }

  override suspend fun draw(
    path: Path,
    duration: Duration,
  ) {
    draw(
      paths = listOf(path),
      duration = duration,
    )
  }

  // Not public because TouchRobot doesn't support different movement
  // patterns per pointer yet (for simplicity).
  private suspend fun draw(
    paths: List<Path>,
    duration: Duration,
  ) {
    require(paths.isNotEmpty()) { "paths must not be empty" }
    require(ongoingGesture == null) {
      "Cannot draw a new gesture when another gesture is already in progress. " +
        "Call up() for all active pointers first."
    }

    // Use a non-linear easing function to mimic a human's finger movement.
    // We can may consider making customizable in the future.
    val easing = EaseInOutSine

    // A single Path can contain multiple "contours" (also called sub-paths). Each contour is one
    // continuous sequence of drawing commands and should be treated as a separate gesture.
    val pathMeasures = paths.fastMap { it.measure() }

    // When multiple paths are provided (e.g., for pinch gestures), their contours are traversed
    // in parallel for simplicity. That is, all fingers move simultaneously.
    val contourLengthsPerPath = paths.fastMap { it.calculateContourLengths() }
    if (paths.size > 1) {
      require(contourLengthsPerPath.fastDistinctBy { it.size }.size == 1) {
        "Different movement patterns per finger aren't supported yet. When drawing with multiple fingers, " +
          "each finger's path must have the same number of separate strokes (created by moveTo() calls). " +
          "This is an intentional limitation for simplicity."
      }
    }

    // Use the first path's contour lengths to determine duration proportions,
    // assuming that all contours have the same length, which is true for now.
    val primaryContourLengths = contourLengthsPerPath.first()
    val totalContourLength = primaryContourLengths.sum()

    for (contourLength in primaryContourLengths) {
      if (contourLength == 0f) {
        pathMeasures.forEach { it.nextContour() }
        continue
      }
      val contourDuration = duration * (contourLength / totalContourLength.toDouble())

      val downTime = SystemClock.uptimeMillis()
      pathMeasures.fastForEachIndexed { index, pathMeasure ->
        down(PointerId(index.toLong()), pathMeasure.getIntPosition(0f))
      }

      var frameDurationMs = 0L
      var progress = 0f

      while (progress < 1f) {
        // delay() gets queued behind layoutlib's frame clock, which Paparazzi drives.
        // By using 1 no matter Paparazzi's FPS, we will be called on the next frame.
        delay(1)

        val eventTime = SystemClock.uptimeMillis()
        if (frameDurationMs == 0L) {
          frameDurationMs = eventTime - downTime
        }

        progress = easing.transform(
          ((eventTime - downTime).toFloat() / contourDuration.inWholeMilliseconds).coerceIn(0f, 1f)
        )
        val historicalTimeDelta = -frameDurationMs / 2f
        val historicalProgress = (progress + historicalTimeDelta / contourDuration.inWholeMilliseconds).coerceIn(0f, 1f)

        val positions = pathMeasures.fastMap { it.getIntPosition(progress) }
        dispatchMoveEventWithHistory(
          downTime = downTime,
          eventTime = eventTime,
          pointerIds = pathMeasures.indices.map { PointerId(it.toLong()) },
          positions = positions,
          historicalTimeDelta = historicalTimeDelta.roundToLong(),
          historicalPositions = pathMeasures.fastMap { it.getIntPosition(historicalProgress) },
        )
        positions.fastForEachIndexed { index, position ->
          ongoingGesture!!.downPositions[PointerId(index.toLong())] = position
        }
      }

      pathMeasures.fastForEachIndexed { index, _ ->
        up(PointerId(index.toLong()))
      }

      // Advance all path measures to the next contour for the next iteration.
      pathMeasures.forEach { it.nextContour() }
    }
  }

  /**
   * Calculate the length of each contour in this path.
   *
   * A path can contain multiple disconnected contours (e.g., drawing
   * separate strokes without lifting). This returns a length for each.
   */
  private fun Path.calculateContourLengths(): List<Float> {
    val lengths = mutableListOf<Float>()
    val measure = this.measure()
    do {
      lengths.add(measure.length)
    } while (measure.nextContour())
    return lengths
  }

  private fun Path.measure(): AndroidPathMeasure {
    return AndroidPathMeasure(
      /* path = */ this.asAndroidPath(),
      /* forceClosed = */ false,
    )
  }

  private fun AndroidPathMeasure.getIntPosition(fraction: Float): IntOffset {
    getPosTan(fraction * length, twoFloatBuffer, null)
    return IntOffset(twoFloatBuffer[0].roundToInt(), twoFloatBuffer[1].roundToInt())
  }

  private fun dispatchTouchEvent(
    action: Int,
    downTime: Long,
    eventTime: Long,
    allPointers: List<Pair<PointerId, IntOffset>>,
    actionPointerId: PointerId,
  ) {
    val allPointers = allPointers.sortedBy { (pointerId) -> pointerId.value }
    val actionIndex = allPointers.indexOfFirst { it.first == actionPointerId }
    check(actionIndex != -1) { "actionPointerId $actionPointerId not found in allPointers" }

    // For down/up, the action must encode which pointer index is affected.
    val action = when (action) {
      MotionEvent.ACTION_POINTER_DOWN,
      MotionEvent.ACTION_POINTER_UP -> action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
      else -> action
    }

    val event = MotionEvent.obtain(
      /* downTime = */ downTime,
      /* eventTime = */ eventTime,
      /* action = */ action,
      /* pointerCount = */ allPointers.size,
      /* pointerProperties = */
      allPointers.map { (pointerId, _) ->
        MotionEvent.PointerProperties().also {
          it.id = pointerId.value.toInt()
          it.toolType = MotionEvent.TOOL_TYPE_FINGER
        }
      }.toTypedArray(),
      /* pointerCoords = */
      allPointers.map { (_, position) ->
        MotionEvent.PointerCoords().also {
          it.x = position.x.toFloat()
          it.y = position.y.toFloat()
        }
      }.toTypedArray(),
      /* metaState = */ 0,
      /* buttonState = */ 0,
      /* xPrecision = */ 1f,
      /* yPrecision = */ 1f,
      /* deviceId = */ 0,
      /* edgeFlags = */ 0,
      /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
      /* flags = */ 0,
    )

    dispatcher.dispatch(event)
    event.recycle()
  }

  private fun dispatchMoveEventWithHistory(
    downTime: Long,
    eventTime: Long,
    pointerIds: List<PointerId>,
    positions: List<IntOffset>,
    historicalPositions: List<IntOffset>,
    historicalTimeDelta: Long,
  ) {
    check(positions.size == historicalPositions.size) {
      "historicalPositions.size != positions.size"
    }
    check(positions.size == pointerIds.size) {
      "positions.size != pointerIds.size"
    }
    val pointerProperties = pointerIds.fastMap { pointerId ->
      MotionEvent.PointerProperties().also {
        it.id = pointerId.value.toInt()
        it.toolType = MotionEvent.TOOL_TYPE_FINGER
      }
    }
    val pointerCoordinates = positions.fastMap { position ->
      MotionEvent.PointerCoords().also {
        it.x = position.x.toFloat()
        it.y = position.y.toFloat()
      }
    }
    val event = MotionEvent.obtain(
      /* downTime = */ downTime,
      /* eventTime = */ eventTime,
      /* action = */ MotionEvent.ACTION_MOVE,
      /* pointerCount = */ positions.size,
      /* pointerProperties = */ pointerProperties.toTypedArray(),
      /* pointerCoords = */ pointerCoordinates.toTypedArray(),
      /* metaState = */ 0,
      /* buttonState = */ 0,
      /* xPrecision = */ 1f,
      /* yPrecision = */ 1f,
      /* deviceId = */ 0,
      /* edgeFlags = */ 0,
      /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
      /* flags = */ 0,
    )

    val historicalPointerCoordinates = historicalPositions.fastMap { position ->
      MotionEvent.PointerCoords().also {
        it.x = position.x.toFloat()
        it.y = position.y.toFloat()
      }
    }
    event.addBatch(
      /* eventTime = */ eventTime + historicalTimeDelta,
      /* pointerCoords = */ historicalPointerCoordinates.toTypedArray(),
      /* metaState = */ 0,
    )

    dispatcher.dispatch(event)
    event.recycle()
  }

  companion object {
    private val twoFloatBuffer = FloatArray(2)
  }
}

internal class OngoingGesture(val downTime: Long) {
  val downPositions = mutableMapOf<PointerId, IntOffset>()
}
