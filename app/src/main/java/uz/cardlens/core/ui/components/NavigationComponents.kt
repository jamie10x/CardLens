package uz.cardlens.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import uz.cardlens.core.navigation.Tab

data class BottomNavItem(
    val tab: Tab,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Tab.Home, "Home", Icons.Default.Home),
    BottomNavItem(Tab.Contacts, "Contacts", Icons.Default.Badge),
    BottomNavItem(Tab.Scan, "Scan", Icons.Default.CameraAlt),
    BottomNavItem(Tab.FollowUps, "Tasks", Icons.Default.Check),
    BottomNavItem(Tab.Settings, "Settings", Icons.Default.Settings),
)

@Composable
fun CardLensBottomBar(
    activeTab: Tab,
    onTabClick: (Tab) -> Unit,
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = activeTab == item.tab,
                onClick = { onTabClick(item.tab) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}
