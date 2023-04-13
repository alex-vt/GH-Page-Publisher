package com.alexvt.publisher.viewui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun EditorTitleView(
    title: String,
    isNewLabelVisible: Boolean,
    hasLink: Boolean,
    onLinkClicked: () -> Unit,
) = Row(
    Modifier.fillMaxWidth().background(MaterialTheme.colors.background).padding(6.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    if (isNewLabelVisible) {
        Text(
            "New",
            fontSize = MaterialTheme.typography.body2.fontSize,
            modifier = Modifier
                .clip(shape = RoundedCornerShape(10.dp))
                .background(MaterialTheme.colors.secondaryVariant)
                .padding(horizontal = 6.dp)
                .align(Alignment.CenterVertically)
        )
        Spacer(Modifier.width(4.dp))
    }
    Text(
        title,
        fontSize = MaterialTheme.typography.body2.fontSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
    if (hasLink) {
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Default.OpenInBrowser,
            contentDescription = "Browse",
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .size(18.dp)
                .clickable {
                    onLinkClicked()
                }
                .align(Alignment.CenterVertically)
        )
    }
}
