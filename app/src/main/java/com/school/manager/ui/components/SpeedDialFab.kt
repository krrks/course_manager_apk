package com.school.manager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.manager.ui.theme.*

@Composable
fun SpeedDialItem(
    label: String, icon: ImageVector, color: Color,
    selected: Boolean = false, onClick: () -> Unit
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape           = RoundedCornerShape(8.dp),
            color           = if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier        = Modifier.clickable { onClick() }
        ) {
            Text(label,
                modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style      = MaterialTheme.typography.labelMedium,
                color      = if (selected) color else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
        SmallFloatingActionButton(
            onClick        = onClick,
            containerColor = if (selected) color else color.copy(alpha = 0.85f),
            contentColor   = Color.White,
            shape          = CircleShape
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ScreenSpeedDialFab(
    addLabel: String? = null,
    addIcon: ImageVector = Icons.Default.Add,
    onAdd: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit,
    extraItems: @Composable (ColumnScope.() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier         = if (expanded) Modifier.fillMaxSize() else Modifier.wrapContentSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (expanded) {
            Box(
                Modifier.fillMaxSize().clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { expanded = false }
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (expanded) {
                extraItems?.invoke(this)

                if (addLabel != null && onAdd != null) {
                    SpeedDialItem(addLabel, addIcon, FluentBlue) { onAdd(); expanded = false }
                }

                HorizontalDivider(
                    color    = FluentBorder.copy(alpha = 0.4f),
                    modifier = Modifier.width(200.dp)
                )

                SpeedDialItem("导航菜单", Icons.Default.Menu, FluentMuted) {
                    onOpenDrawer(); expanded = false
                }
            }

            FloatingActionButton(
                onClick        = { expanded = !expanded },
                containerColor = FluentBlue,
                contentColor   = Color.White,
                shape          = CircleShape
            ) {
                Icon(
                    if (expanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (expanded) "关闭" else "菜单"
                )
            }
        }
    }
}
