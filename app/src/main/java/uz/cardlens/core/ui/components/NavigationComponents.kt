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
import androidx.compose.ui.res.stringResource
import uz.cardlens.R
import uz.cardlens.core.navigation.Tab

data class BottomNavItem(
    val tab: Tab,
    val labelResId: Int,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Tab.Home, R.string.nav_home, Icons.Default.Home),
    BottomNavItem(Tab.Contacts, R.string.nav_contacts, Icons.Default.Badge),
    BottomNavItem(Tab.Scan, R.string.nav_scan, Icons.Default.CameraAlt),
    BottomNavItem(Tab.FollowUps, R.string.nav_followups, Icons.Default.Check),
    BottomNavItem(Tab.Settings, R.string.nav_settings, Icons.Default.Settings),
)

@Composable
fun CardLensBottomBar(
    activeTab: Tab,
    onTabClick: (Tab) -> Unit,
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val label = stringResource(item.labelResId)
            NavigationBarItem(
                selected = activeTab == item.tab,
                onClick = { onTabClick(item.tab) },
                icon = { Icon(item.icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}