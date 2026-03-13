package com.school.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.school.manager.ui.screens.*
import com.school.manager.ui.theme.*
import com.school.manager.viewmodel.AppViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchoolManagerTheme {
                SchoolManagerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolManagerApp() {
    val vm: AppViewModel = viewModel()
    val navController   = rememberNavController()
    val drawerState     = rememberDrawerState(DrawerValue.Closed)
    val scope           = rememberCoroutineScope()
    val navBackStack    by navController.currentBackStackEntryAsState()
    val currentRoute    = navBackStack?.destination?.route ?: Screen.Schedule.route

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                // ── Drawer header ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Brush.linearGradient(listOf(FluentBlue, FluentBlueDark)))
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text("🏫", fontSize = 32.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("智慧课务管理", color = Color.White,
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Smart School Manager", color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Nav items ──
                ALL_SCREENS.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationDrawerItem(
                        icon   = { Icon(screen.icon, contentDescription = screen.title) },
                        label  = { Text(screen.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        shape  = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor   = FluentBlueLight,
                            selectedIconColor        = FluentBlue,
                            selectedTextColor        = FluentBlue,
                            unselectedIconColor      = FluentMuted,
                            unselectedTextColor      = FluentMuted
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    ) {
        // Each screen now manages its own FAB that merges navigation + add actions.
        // The standalone menu FAB shown here previously has been removed.
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { _ ->
            NavHost(
                navController    = navController,
                startDestination = Screen.Schedule.route,
                modifier         = Modifier.fillMaxSize().animateContentSize(),
                enterTransition  = { slideInHorizontally { it / 6 } + fadeIn() },
                exitTransition   = { slideOutHorizontally { -it / 6 } + fadeOut() },
                popEnterTransition  = { slideInHorizontally { -it / 6 } + fadeIn() },
                popExitTransition   = { slideOutHorizontally { it / 6 } + fadeOut() },
            ) {
                composable(Screen.Schedule.route)   { ScheduleScreen(vm, onOpenDrawer = openDrawer) }
                composable(Screen.Attendance.route) { AttendanceScreen(vm, onOpenDrawer = openDrawer) }
                composable(Screen.Classes.route)    { ClassesScreen(vm, onOpenDrawer = openDrawer) }
                composable(Screen.Teachers.route)   { TeachersScreen(vm, onOpenDrawer = openDrawer) }
                composable(Screen.Students.route)   { StudentsScreen(vm, onOpenDrawer = openDrawer) }
                composable(Screen.Stats.route)      { StatsScreen(vm, onOpenDrawer = openDrawer) }
                composable(Screen.Export.route)     { ExportScreen(vm, onOpenDrawer = openDrawer) }
            }
        }
    }
}
