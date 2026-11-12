package echo.music.iad1tya.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import echo.music.iad1tya.ui.screens.Screens

/**
 * The window has no system title bar: this bar replaces it. Left: menu and back; middle: Home and a search box
 * (opens the Search page); right: full screen, minimise, maximise/restore, close. Empty space drags the window.
 * shortcut: no forward button (the navigation library keeps no forward history); an undecorated window cannot be resized by its edges, use maximise.
 */
@Composable
fun WindowScope.DesktopTopBar(navController: NavController, windowState: WindowState, onClose: () -> Unit) {
  val current by navController.currentBackStackEntryAsState()
  val canGoBack = current != null && navController.previousBackStackEntry != null
  var menuOpen by remember { mutableStateOf(false) }
  WindowDraggableArea {
    Box(Modifier.fillMaxWidth().height(56.dp).background(MaterialTheme.colorScheme.surface)) {
      Row(Modifier.align(Alignment.CenterStart).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
          BarButton("•••") { menuOpen = true }
          DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            listOf("History" to "history", "Stats" to "stats", "Settings" to "settings").forEach { (label, route) ->
              DropdownMenuItem(text = { Text(label) }, onClick = { menuOpen = false; navController.navigate(route) { launchSingleTop = true } })
            }
            DropdownMenuItem(text = { Text("Quit") }, onClick = { menuOpen = false; onClose() })
          }
        }
        BarButton("‹", enabled = canGoBack) { navController.navigateUp() }
      }
      Row(Modifier.align(Alignment.Center).widthIn(max = 560.dp).fillMaxWidth(0.45f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { navController.navigate(Screens.Home.route) { launchSingleTop = true } },
          contentAlignment = Alignment.Center,
        ) { Icon(painterResource(Screens.Home.iconIdActive), "Home") }
        Row(
          Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { navController.navigate(Screens.Search.route) { launchSingleTop = true } }.padding(horizontal = 14.dp),
          verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(painterResource(Screens.Search.iconIdInactive), "Search", Modifier.size(22.dp))
          Text("What do you want to play?", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
      }
      Row(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BarButton("⛶") { windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) WindowPlacement.Floating else WindowPlacement.Fullscreen }
        BarButton("—") { windowState.isMinimized = true }
        BarButton(if (windowState.placement == WindowPlacement.Maximized) "❐" else "☐") {
          windowState.placement = if (windowState.placement == WindowPlacement.Maximized) WindowPlacement.Floating else WindowPlacement.Maximized
        }
        BarButton("✕", onClick = onClose)
      }
    }
  }
}

@Composable
private fun BarButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
  Box(
    Modifier.size(width = 46.dp, height = 40.dp).clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) { Text(label, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f)) }
}
