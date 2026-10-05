package me.saket.touchrobot

import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class PaparazziWindowRootsTest {
  @Test fun `missing window method preserves host-only lookup`() {
    assertTrue(paparazziWindowRoots { throw NoSuchMethodError("getWindowViews") }.isEmpty())
  }

  @Test fun `missing window class preserves host-only lookup`() {
    assertTrue(paparazziWindowRoots { throw NoClassDefFoundError("WindowManagerGlobal") }.isEmpty())
  }

  @Test fun `window enumeration failures are not hidden`() {
    val failure = IllegalStateException("Window enumeration failed")
    assertSame(failure, assertThrows(IllegalStateException::class.java) {
      paparazziWindowRoots { throw failure }
    })
  }
}
