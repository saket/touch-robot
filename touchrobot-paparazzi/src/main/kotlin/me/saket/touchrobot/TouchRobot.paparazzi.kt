package me.saket.touchrobot

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.unit.IntRect
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
 * @see rememberTouchRobot
 */
fun TouchRobot.onNode(matcher: SemanticsMatcher): TouchRobotTarget {
  return onNode(matcher, useUnmergedTree = false)
}

/**
 * @param useUnmergedTree If `true`, searches the unmerged semantics tree instead of the merged
 *   semantics tree. This allows you to search for individual nodes that would otherwise be part of
 *   a larger semantic unit, for example a text and an image forming a button together.
 */
fun TouchRobot.onNode(matcher: SemanticsMatcher, useUnmergedTree: Boolean): TouchRobotTarget {
  return onBounds { hostView ->
    hostView.awaitNodeBounds(matcher, useUnmergedTree)
  }
}

private suspend fun View.awaitNodeBounds(matcher: SemanticsMatcher, useUnmergedTree: Boolean): IntRect {
  val bounds = withTimeoutOrNull(200.milliseconds) {
    var node: SemanticsNode?
    while (true) {
      node = findFirstSemanticNode(matcher, useUnmergedTree)
      if (node == null) delay(1) else break
    }
    node.boundsInRoot.roundToIntRect()
  }
  return checkNotNull(bounds) {
    "Timed out waiting for node that matches: $matcher"
  }
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
