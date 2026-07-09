package me.saket.touchrobot

import android.view.ViewGroup.LayoutParams
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.util.lerp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import me.saket.touchrobot.paparazzi.R
import com.android.ide.common.rendering.api.SessionParams
import kotlinx.coroutines.delay
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TouchRobotPaparazziTest {
  @get:Rule val paparazzi = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_6_PRO,
    renderingMode = SessionParams.RenderingMode.SHRINK,
    maxPercentDifference = 0.025,
  )

  @Test fun clicks() {
    paparazzi.gif(end = 2400) {
      FakeSystemUi(darkTheme = true) { contentPadding ->
        Box(
          Modifier
            .fillMaxSize()
            .background(Color(0xFF4B00BA))
            .padding(contentPadding)
            .padding(24.dp)
            .wrapContentHeight()
            .height(200.dp)
        ) {
          FakeButton(
            modifier = Modifier.align(AbsoluteAlignment.TopLeft),
            text = "Left",
          )
          FakeButton(
            modifier = Modifier.align(Alignment.Center),
            text = "Center",
          )
          FakeButton(
            modifier = Modifier.align(AbsoluteAlignment.BottomRight),
            text = "Right",
          )
        }
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        delay(500)
        touchRobot.onNode(hasText("Left")).performGesture {
          click(centerRight)
        }
        delay(500)
        touchRobot.onNode(hasText("Center")).performGesture {
          longClick(center)
        }
        delay(500)
        touchRobot.onNode(hasText("Right")).performGesture {
          click(centerLeft)
        }
      }
    }
  }

  @Test fun swipes() {
    paparazzi.gif(end = 5000, fps = 60) {
      FakeSystemUi { contentPadding ->
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .height(1000.dp)
            .background(Color(0xFFE6EE9C))
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          val numOfItems = 5
          repeat(numOfItems) { index ->
            val pageAlpha = lerp(
              start = 0.25f,
              stop = 1f,
              fraction = index / (numOfItems - 1f),
            )
            FakeCarousel(
              modifier = Modifier.testTag("carousel_$index"),
              pageColor = Color(0xFF9E9D24).copy(alpha = pageAlpha),
            )
          }
        }
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        delay(500)
        touchRobot.onRoot().performGesture {
          swipe(
            start = (center + bottomCenter) / 2f,
            stop = center,
            duration = 600.milliseconds,
          )
        }
        delay(500)
        touchRobot.onNode(hasTestTag("carousel_2")).performGesture {
          repeat(3) {
            swipe(
              start = center,
              stop = centerLeft,
              duration = 300.milliseconds,
            )
            delay(300)
          }
        }
        delay(500)
        touchRobot.onRoot().performGesture {
          swipe(
            start = center,
            stop = (center + bottomCenter) / 2f,
            duration = 600.milliseconds,
          )
        }
      }
    }
  }

  @Test fun `custom path`() {
    paparazzi.gif(end = 3500) {
      FakeSystemUi { contentPadding ->
        Box(
          Modifier
            .align(Alignment.Center)
            .padding(contentPadding)
            .padding(16.dp)
        ) {
          Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(R.drawable.cash_card),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
          )
          FreehandDraw(
            modifier = Modifier
              .matchParentSize()
              .padding(start = 120.dp, end = 20.dp, top = 40.dp)
              .testTag("canvas"),
            strokeColor = Color(0xFF3DDC84),
          )
        }
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        touchRobot.onNode(hasTestTag("canvas")).performGesture {
          draw(
            path = createAndroidHeadPath(bounds.toRect()),
            duration = 3.seconds,
          )
        }
      }
    }
  }

  @Test fun `pinch to zoom`() {
    paparazzi.gif(end = 3200) {
      FakeSystemUi {
        Image(
          modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .height(400.dp)
            .padding(16.dp)
            .zoomable(rememberZoomableState(ZoomSpec(maxZoomFactor = 2f)), clipToBounds = false)
            .testTag("image"),
          // Illustration from https://cash.app/press/cash-app-strava-team-up-challenge.
          painter = painterResource(R.drawable.illustration),
          contentDescription = null,
        )
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        delay(500)

        touchRobot.onNode(hasTestTag("image")).performGesture {
          val startOffset = IntOffset(100, 100)
          val endOffset = IntOffset(250, 250)
          pinch(
            start0 = center - startOffset,
            start1 = center + startOffset,
            end0 = center - endOffset,
            end1 = center + endOffset,
            duration = 1.seconds,
          )

          delay(500)

          // Zoom out.
          pinch(
            start0 = center - endOffset,
            start1 = center + endOffset,
            end0 = center - startOffset,
            end1 = center + startOffset,
            duration = 1.seconds,
          )
        }
      }
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Test fun overlays() {
    paparazzi.gif(end = 2000, fps = 60) {
      FakeSystemUi { contentPadding ->
        Box(
          Modifier
            .fillMaxSize()
            .background(Color(0xFF7A1FFF))
            .padding(contentPadding),
        )

        ModalBottomSheet(
          modifier = Modifier.testTag("sheet"),
          onDismissRequest = {},
        ) {
          Box(
            Modifier
              .fillMaxWidth()
              .height(300.dp)
              .padding(16.dp)
              .testTag("sheet-content"),
          ) {
            BasicText(
              text = "Sheeeittt",
              style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
            )
          }
        }
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        delay(500)

        touchRobot.onNode(hasTestTag("sheet-content")).performGesture {
          swipe(
            start = topCenter,
            stop = center,
            duration = 1.seconds,
          )
        }
      }
    }
  }

  @Test fun `multiple pointers`() {
    paparazzi.gif(end = 2500) {
      var pointerCount by remember { mutableIntStateOf(0) }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(300.dp)
          .background(Color(0xFFA64D79))
          .pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                pointerCount = event.changes.count { it.pressed }
              }
            }
          }
          .testTag("content"),
        contentAlignment = Alignment.Center,
      ) {
        BasicText(
          text = if (pointerCount == 1) "1 pointer" else "$pointerCount pointers",
          style = TextStyle(fontSize = 24.sp, color = Color.White),
        )
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        delay(200)

        touchRobot.onNode(hasTestTag("content")).performGesture {
          down(PointerId(0), center.minus(IntOffset(0, 200)))
          delay(500)

          down(PointerId(1), center.plus(IntOffset(0, 200)))
          delay(500)

          up(PointerId(1))
          delay(500)

          up(PointerId(0))
          delay(500)
        }
      }
    }
  }

  @Test fun `taps overlay works with shrink render mode`() {
    paparazzi.gif(end = 1000) {
      Box(
        Modifier
          .size(100.dp, 100.dp)
          .background(Color(0xFF3B3B3B))
          .testTag("content"),
      )

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        touchRobot.onNode(hasTestTag("content")).performGesture {
          longClick()
        }
      }
    }
  }
}

// todo: upstream this to paparazzi
private fun Paparazzi.gif(
  start: Long = 0L,
  end: Long = 500L,
  fps: Int = 30,
  composable: @Composable () -> Unit,
) {
  val hostView = ComposeView(context).apply {
    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
  }
  hostView.setContent(composable)
  gif(
    view = hostView,
    start = start,
    end = end,
    fps = fps,
  )
}

@Composable
private fun FakeButton(
  text: String,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val cornerPercent = remember { Animatable(50f) }
  LaunchedEffect(Unit) {
    // This uses collect() instead of collectLatest() so that the shape animation
    // can play fully even if the tap duration was tiny, which is the case for clicks.
    snapshotFlow { isPressed }.collect {
      cornerPercent.animateTo(if (it) 40f else 50f)
    }
  }

  Box(
    modifier = modifier
      .size(96.dp)
      .clip(RoundedCornerShape(percent = cornerPercent.value.roundToInt()))
      .background(Color.White)
      .clickable(
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        onClick = {},
      )
      .padding(horizontal = 16.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    BasicText(
      text = text,
      style = TextStyle(color = Color.Black, textAlign = TextAlign.Center),
    )
  }
}

@Composable
private fun FakeCarousel(
  pageColor: Color,
  modifier: Modifier = Modifier,
) {
  HorizontalPager(
    modifier = modifier.fillMaxWidth(),
    state = rememberPagerState { 5 },
    pageSpacing = 16.dp,
    contentPadding = PaddingValues(horizontal = 16.dp),
  ) {
    Box(
      Modifier
        .fillMaxWidth()
        .height(240.dp)
        .background(pageColor, RoundedCornerShape(8.dp)),
    )
  }
}
