@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.saket.touchrobot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** TODO: replace this with https://github.com/saket/fake-system-ui */
@Composable
internal fun FakeSystemUi(
  darkTheme: Boolean = false,
  content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
  val backgroundColor = if (darkTheme) Color.DarkGray else Color.LightGray
  val contentColor = if (darkTheme) Color.White else Color.Black

  val statusBarHeight = 62.dp
  val navigationBarHeight = 24.dp

  Box(
    Modifier
      .fillMaxSize()
      .background(backgroundColor)
  ) {
    content(
      PaddingValues(top = statusBarHeight, bottom = navigationBarHeight)
    )

    // Status bar.
    MatchParentSizePopup {
      Row(
        modifier = Modifier
          .wrapContentHeight(align = Alignment.Top)
          .height(62.dp)
          .padding(horizontal = 16.dp)
          .clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        BasicText(
          text = "9:00",
          color = { contentColor },
        )
        Spacer(Modifier.weight(1f))
        Box(
          Modifier
            .size(16.dp)
            .background(contentColor, CircleShape),
        )
        Box(
          Modifier
            .size(8.dp, 16.dp)
            .background(contentColor),
        )
      }
    }

    // Navigation bar.
    MatchParentSizePopup {
      Box(
        Modifier
          .wrapContentSize(Alignment.BottomCenter)
          .height(navigationBarHeight)
          .wrapContentSize()
          .size(width = 100.dp, height = 4.dp)
          .background(contentColor, RoundedCornerShape(4.dp)),
      )
    }
  }
}
