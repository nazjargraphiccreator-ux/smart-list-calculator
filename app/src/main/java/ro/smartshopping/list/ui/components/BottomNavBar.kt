package ro.smartshopping.list.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import ro.smartshopping.list.ui.navigation.Screen

sealed class BottomNavItem(val screen: Screen, val icon: ImageVector) {
    data object Lists : BottomNavItem(Screen.Lists, Icons.Default.ShoppingCart)
    data object SmartAgent : BottomNavItem(Screen.SmartAgent, Icons.Default.AutoAwesome)
    data object PriceTrends : BottomNavItem(Screen.PriceTrends, Icons.Default.Analytics)
    data object History : BottomNavItem(Screen.History, Icons.Default.History)
    data object Profile : BottomNavItem(Screen.Profile, Icons.Default.Person)
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Lists,
        BottomNavItem.SmartAgent,
        BottomNavItem.PriceTrends,
        BottomNavItem.History,
        BottomNavItem.Profile
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.screen.titleRo) },
                label = { Text(item.screen.titleRo, style = MaterialTheme.typography.labelSmall) },
                selected = selected,
                onClick = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Lists.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
