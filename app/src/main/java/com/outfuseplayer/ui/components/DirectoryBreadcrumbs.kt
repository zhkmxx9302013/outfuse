package com.outfuseplayer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.outfuseplayer.ui.theme.PrimaryOrange

data class DirectoryCrumb(
    val label: String,
    val target: String
)

@Composable
fun DirectoryBreadcrumbs(
    crumbs: List<DirectoryCrumb>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(crumbs, key = { index, crumb -> "$index:${crumb.target}" }) { index, crumb ->
            if (index > 0) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
                    modifier = Modifier.size(16.dp)
                )
            }
            val current = index == crumbs.lastIndex
            Surface(
                onClick = { onNavigate(crumb.target) },
                shape = RoundedCornerShape(8.dp),
                color = if (current) PrimaryOrange.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                border = BorderStroke(
                    1.dp,
                    if (current) PrimaryOrange.copy(alpha = 0.36f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (index == 0) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = if (current) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = crumb.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (current) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
