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
  return onBounds { hostView ->
    hostView.awaitNodeBounds(matcher)
  }
}

private suspend fun View.awaitNodeBounds(matcher: SemanticsMatcher): IntRect {
  val bounds = withTimeoutOrNull(200.milliseconds) {
    var node: SemanticsNode?
    while (true) {
      node = findFirstSemanticNode(matcher)
      if (node == null) delay(1) else break
    }
    node.boundsInRoot.roundToIntRect()
  }
  return checkNotNull(bounds) {
    "Timed out waiting for node that matches: $matcher"
  }
}

@SuppressLint("VisibleForTests")
private fun View.findFirstSemanticNode(matcher: SemanticsMatcher): SemanticsNode? {
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
    it.semanticsOwner.rootSemanticsNode.findFirst(matcher)
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
