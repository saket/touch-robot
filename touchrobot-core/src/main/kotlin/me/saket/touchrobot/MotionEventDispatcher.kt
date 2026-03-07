package me.saket.touchrobot

import android.view.MotionEvent

fun interface MotionEventDispatcher {
  fun dispatch(event: MotionEvent): Boolean
}
