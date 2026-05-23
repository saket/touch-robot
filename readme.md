# touch-robot

[Paparazzi](https://github.com/cashapp/paparazzi/) screenshot tests are great, but they don't catch broken UI interactions before your users do. Anything that lives *between* a layout and a tap (animations, gestures, ripples, scroll-driven UI) goes untested. `touch-robot` fixes that by generating fake touch events to exercise your UI.

```gradle
implementation("me.saket.touchrobot:touchrobot-paparazzi:0.2.0")
```

```kotlin
class InteractionTest {
  @get:Rule val paparazzi = Paparazzi()
  
  @Test fun test() {
    paparazzi.gif(end = 2_500) {
      Box {
        Text("foo")
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        touchRobot.onRoot().performGesture {
          click()
          longClick()
        }

        touchRobot.onNode(hasText("foo")).performGesture {
          swipe(…)
          draw(…)
        }
      }
    }   
  }
}
```

For those unaware, think of `paparazzi.gif()` as recording a video of your UI, except the output is an APNG (Animated PNG) instead. Under the hood, it takes one snapshot per frame and stitches them together. And because APNGs are lossless, each frame can be deterministically compared to a golden value, so an animated test is still a real regression test.

**Show taps**

By default, `touch-robot` renders the pointer location so that you can visually see where the touch events are landing. This is similar to the `Show taps` option in Android's developer settings. You can disable it if needed:

```kotlin
rememberTouchRobot(showTaps = false)
```

**Don't use Paparazzi?**

While `touch-robot` was designed with Paparazzi in mind, its core can also be used with any other screenshot testing library of your choice:

```diff
- implementation("me.saket.touchrobot:touchrobot-paparazzi:0.2.0")
+ implementation("me.saket.touchrobot:touchrobot-core:0.2.0")
```

You'll likely need to copy [TouchRobot.paparazzi.kt](https://github.com/saket/touch-robot/blob/trunk/touchrobot-paparazzi/src/main/kotlin/me/saket/touchrobot/TouchRobot.paparazzi.kt) into your project and adapt it for your chosen library.

### Supported gestures

#### Swipe
<img align="right" width="30%" src="/touchrobot-paparazzi/src/test/snapshots/videos/me.saket.touchrobot_TouchRobotPaparazziTest_swipes.png">

```kotlin
paparazzi.gif(end = 2_500) {
  Column(…) {
    repeat(5) { index ->
      Carousel(Modifier.testTag("page$index"))
    }  
  }
  
  val touchRobot = rememberTouchRobot()
  LaunchedEffect(Unit) {
    touchRobot.onNode(hasTestTag("page2")).performGesture {
      repeat(3) {
        swipe(
          start = center,
          stop = centerLeft,
          duration = 300.milliseconds,
        )
        delay(300)
      }
    }
  }
}
```
<br clear="all"/>

#### Draw
<img align="right" width="30%" src="/touchrobot-paparazzi/src/test/snapshots/videos/me.saket.touchrobot_TouchRobotPaparazziTest_custom_path.png">

```kotlin
paparazzi.gif(end = 3_000) {
  DebitCard(
    Modifier.testTag("card")
  )

  val touchRobot = rememberTouchRobot()
  LaunchedEffect(Unit) {
    touchRobot.onNode(hasTestTag("card")).performGesture {
      draw(
        path = createAndroidHeadPath(),
        duration = 3.seconds,
      )
    }
  }
}

/** A path drawing the Android head. */
fun createAndroidHeadPath(bounds: Rect): Path = TODO()
```

<br clear="all"/>

#### Click
<img align="right" width="30%" src="/touchrobot-paparazzi/src/test/snapshots/videos/me.saket.touchrobot_TouchRobotPaparazziTest_clicks.png">

```kotlin
paparazzi.gif(end = 1000) {
  Row {
    Button {
      Text("Left")
    }
    Button {
      Text("Center")
    }
  }

  val touchRobot = rememberTouchRobot()
  LaunchedEffect(Unit) {
    touchRobot.onNode(hasText("Left")).performGesture {
      click()
    }
    delay(500)
    touchRobot.onNode(hasText("Center")).performGesture {
      longClick()
    }
  }
}
```
<br clear="all"/>

#### Pinch

<img align="right" width="30%" src="/touchrobot-paparazzi/src/test/snapshots/videos/me.saket.touchrobot_TouchRobotPaparazziTest_pinch_to_zoom.png">

```kotlin
paparazzi.gif(end = 2000) {
  ZoomableAsyncImage(
    modifier = Modifier.testTag("image"),
    model = "https://dog.ceo/image.jpg",
    contentDescription = null,
  )

  val touchRobot = rememberTouchRobot()
  LaunchedEffect(Unit) {
    touchRobot.onNode(hasTestTag("image")).performGesture {
      val startOffset = IntOffset(100, 100)
      val endOffset = IntOffset(300, 300)

      pinch(
        start0 = center - startOffset,
        start1 = center + startOffset,
        end0 = center - endOffset,
        end1 = center + endOffset,
        duration = 1.seconds,
      )
    }
  }
}
```
