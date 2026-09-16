package com.neopulsar.cardlens.core.ui.components

import com.neopulsar.cardlens.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neopulsar.cardlens.core.navigation.Tab

data class BottomNavItem(val tab: Tab, val labelResId: Int, val filledIcon: ImageVector, val outlineIcon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Tab.Home, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Tab.Contacts, R.string.nav_contacts, Icons.Filled.Badge, Icons.Outlined.Badge),
    BottomNavItem(Tab.Scan, R.string.nav_scan, Icons.Filled.CameraAlt, Icons.Outlined.CameraAlt),
    BottomNavItem(Tab.FollowUps, R.string.nav_followups, Icons.Filled.Check, Icons.Outlined.Check),
    BottomNavItem(Tab.Settings, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun CardLensBottomBar(activeTab: Tab, onTabClick: (Tab) -> Unit, notificationsBadge: Boolean = false) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().height(76.dp),
    ) {
        bottomNavItems.forEach { item ->
            val label = stringResource(item.labelResId)
            val selected = activeTab == item.tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabClick(item.tab) },
                icon = {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (selected) item.filledIcon else item.outlineIcon,
                            contentDescription = label,
                            modifier = Modifier.size(24.dp),
                        )
                        if (item.tab == Tab.FollowUps && notificationsBadge) {
                            Box(
                                Modifier.size(9.dp).align(Alignment.TopEnd)
                                    .clip(CircleShape).background(MaterialTheme.colorScheme.error)
                                    .clip(CircleShape),
                            )
                        }
                    }
                },
                label = {
                    Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ),
            )
        }
    }
}
