package com.funny.translation.translate.ui.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.funny.translation.strings.ResStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTooltipCloseButton(
    tooltipState: TooltipState
) {
    TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.End),
        onClick = {
            tooltipState.dismiss()
        }
    ) {
        Text(text = ResStrings.close)
    }
}