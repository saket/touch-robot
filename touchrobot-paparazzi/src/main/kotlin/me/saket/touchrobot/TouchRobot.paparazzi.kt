package me.saket.touchrobot

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.view.WindowManagerGlobal
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.roundToIntRect
import androidx.core.view.children
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Simulate touch gestures on a Compose UI node in paparazzi tests.
 *
 * ```
 * val touchRobot = rememberTouchRobot()
 * touchRobot.onNode(hasTestTag("submit-button")).performGesture {
 *   click()
 * }
 * ```
 *
 * @param useUnmergedTree If `true`, searches the unmerged semantics tree instead of the merged
 *   semantics tree. This allows you to search for individual nodes that would otherwise be part of
 *   a larger semantic unit, for example a text and an image forming a button together.
 *
 * @see rememberTouchRobot
 */
fun TouchRobot.onNode(matcher: SemanticsMatcher, useUnmergedTree: Boolean = false): TouchRobotTarget {
  return onTargetBounds { hostView ->
    hostView.awaitNodeBounds(matcher, useUnmergedTree)
  }
}

private suspend fun View.awaitNodeBounds(matcher: SemanticsMatcher, useUnmergedTree: Boolean): TouchRobotTarget.Bounds {
  val target = withTimeoutOrNull(200.milliseconds) {
    while (true) {
      for (candidate in findAllWindowRoots(hostWindowRoot = rootView)) {
        val node = candidate.findFirstSemanticNode(matcher, useUnmergedTree)
        if (node != null) {
          return@withTimeoutOrNull TouchRobotTarget.Bounds(
            windowRootView = candidate,
            bounds = node.boundsInWindow.roundToIntRect(),
          )
        }
      }
      delay(1.milliseconds)
    }
    @Suppress("KotlinUnreachableCode")
    error("unreachable code")
  }
  return checkNotNull(target) {
    "Timed out waiting for node that matches: ${matcher.description}"
  }
}

private fun findAllWindowRoots(hostWindowRoot: View): List<View> {
  // Manually include the host window because layoutlib attaches paparazzi's
  //  render root directly without registering it in its WindowManager.
  val windows = WindowManagerGlobal.getInstance().windowViews
  val roots = if (hostWindowRoot in windows) windows else listOf(hostWindowRoot) + windows
  return roots.asReversed() // Return search newer windows first.
}

@SuppressLint("VisibleForTests")
private fun View.findFirstSemanticNode(matcher: SemanticsMatcher, useUnmergedTree: Boolean): SemanticsNode? {
  // The view hierarchy might have multiple ViewRootForTest. Each interop point between
  // Compose and Views (through AbstractComposeView) will have its own ViewRootForTest.
  // Find them all before running the semantics matcher.
  val viewRootForTests = mutableListOf<ViewRootForTest>()
  this.walkTree { child ->
    if (child is ViewRootForTest) {
      viewRootForTests.add(child)
    }
  }
  return viewRootForTests.firstNotNullOfOrNull {
    val root = if (useUnmergedTree) it.semanticsOwner.unmergedRootSemanticsNode else it.semanticsOwner.rootSemanticsNode
    root.findFirst(matcher)
  }
}

private fun View.walkTree(block: (View) -> Unit) {
  block(this)
  if (this is ViewGroup) {
    for (child in children) {
      child.walkTree(block)
    }
  }
}

private fun SemanticsNode.findFirst(matcher: SemanticsMatcher): SemanticsNode? {
  if (matcher.matches(this)) {
    return this
  }
  for (child in children) {
    child.findFirst(matcher)?.let { return it }
  }
  return null
}
