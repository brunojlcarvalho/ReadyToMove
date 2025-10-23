package pt.ipp.estg.readytomove.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import java.util.Locale

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf("login", "map", "leaderboard", "profile")

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                selected = false,
                onClick = { navController.navigate(screen) },
                icon = { Icon(Icons.Default.Place, contentDescription = screen) },
                label = { Text(screen.capitalize(Locale.ROOT)) }
            )
        }
    }
}
