package com.example.health.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * 可滚动的下拉菜单 —— 替代 Material3 的 DropdownMenu（不支持滚动）。
 * 当匹配项超过 maxHeight 时自动出现滚动条。
 */
@Composable
fun ScrollableDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Int = 250,
    items: List<DropdownItem>,
) {
    if (!expanded || items.isEmpty()) return

    Popup(
        onDismissRequest = onDismiss,
        offset = IntOffset.Zero,
        properties = PopupProperties(focusable = false),
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.88f)
                .heightIn(max = maxHeight.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            LazyColumn {
                itemsIndexed(items) { index, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                item.onClick()
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        item.content()
                    }
                    if (index < items.size - 1) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

data class DropdownItem(
    val key: String,
    val content: @Composable () -> Unit,
    val onClick: () -> Unit
)
