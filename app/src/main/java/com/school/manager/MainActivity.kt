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
    val currentScreen   = ALL_SCREENS.find { it.route == currentRoute } ?: Screen.Schedule

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
                        .background(
                            Brush.linearGradient(listOf(FluentBlue, FluentBlueDark))
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text("🏫", fontSize = 32.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "智慧课务管理",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Smart School Manager",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
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
        // No TopAppBar — content fills the full screen from the top.
        // The ScheduleScreen's CalendarGrid day-header row is now visible at the very top.
        // Navigation drawer is accessible via the "导航菜单" entry in the ScheduleScreen speed-dial FAB,
        // and via a persistent menu FAB shown on every non-schedule screen.
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { inner ->
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController    = navController,
                    startDestination = Screen.Schedule.route,
                    modifier         = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    enterTransition  = { slideInHorizontally { it / 6 } + fadeIn() },
                    exitTransition   = { slideOutHorizontally { -it / 6 } + fadeOut() },
                    popEnterTransition  = { slideInHorizontally { -it / 6 } + fadeIn() },
                    popExitTransition   = { slideOutHorizontally { it / 6 } + fadeOut() },
                ) {
                    composable(Screen.Schedule.route)   {
                        // Pass drawer-open callback so ScheduleScreen's speed-dial FAB can open it
                        ScheduleScreen(vm, onOpenDrawer = { scope.launch { drawerState.open() } })
                    }
                    composable(Screen.Attendance.route) { AttendanceScreen(vm) }
                    composable(Screen.Classes.route)    { ClassesScreen(vm) }
                    composable(Screen.Teachers.route)   { TeachersScreen(vm) }
                    composable(Screen.Students.route)   { StudentsScreen(vm) }
                    composable(Screen.Stats.route)      { StatsScreen(vm) }
                    composable(Screen.Export.route)     { ExportScreen(vm) }
                }

                // Persistent menu FAB shown on every screen except Schedule
                // (Schedule has its own speed-dial FAB that already includes "导航菜单").
                if (currentScreen != Screen.Schedule) {
                    FloatingActionButton(
                        onClick        = { scope.launch { drawerState.open() } },
                        modifier       = Modifier
                            .align(Alignment.BottomStart)
                            .padding(
                                start  = 16.dp,
                                bottom = inner.calculateBottomPadding() + 88.dp  // above screen's own FAB
                            ),
                        containerColor = FluentBlue,
                        contentColor   = Color.White,
                        shape          = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "导航菜单")
                    }
                }
            }
        }
    }
}
