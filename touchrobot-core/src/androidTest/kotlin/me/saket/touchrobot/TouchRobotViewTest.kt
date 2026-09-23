package me.saket.touchrobot

import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.IntOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TouchRobotViewTest {
  @Test fun view_robot_dispatches_directly_and_returns_if_any_event_was_handled() = runTest {
    var parentDispatchCount = 0
    val view = View(InstrumentationRegistry.getInstrumentation().targetContext).apply {
      setOnTouchListener { _, event ->
        event.actionMasked == MotionEvent.ACTION_DOWN
      }
    }
    FrameLayout(view.context).apply {
      layout(0, 0, 100, 100)
      setOnTouchListener { _, _ ->
        parentDispatchCount++
        false
      }
      addView(view)
      view.layout(0, 0, 100, 100)
    }

    val wasHandled = withContext(Dispatchers.Main) {
      TouchRobot(view).onRoot().performGesture {
        click(IntOffset(10, 10))
      }
    }

    assertThat(wasHandled).isTrue()
    assertThat(parentDispatchCount).isEqualTo(0)
  }
}
