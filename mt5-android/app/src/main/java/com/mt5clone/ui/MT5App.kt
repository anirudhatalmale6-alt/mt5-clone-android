package com.mt5clone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mt5clone.ui.chart.ChartScreen
import com.mt5clone.ui.common.theme.MT5ThemeProvider
import com.mt5clone.ui.history.HistoryScreen
import com.mt5clone.ui.messages.MessagesScreen
import com.mt5clone.ui.quotes.QuotesScreen
import com.mt5clone.ui.settings.SettingsScreen
import com.mt5clone.ui.trade.TradeScreen

// Navigation destinations matching MT5 bottom tabs
sealed class MT5Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Quotes : MT5Screen("quotes", "Quotes", Icons.Default.FormatListBulleted)
    data object Chart : MT5Screen("chart", "Chart", Icons.Default.ShowChart)
    data object Trade : MT5Screen("trade", "Trade", Icons.Default.SwapVert)
    data object History : MT5Screen("history", "History", Icons.Default.History)
    data object Messages : MT5Screen("messages", "Messages", Icons.Default.Email)
}

val bottomNavItems = listOf(
    MT5Screen.Quotes,
    MT5Screen.Chart,
    MT5Screen.Trade,
    MT5Screen.History,
    MT5Screen.Messages
)

@Composable
fun MT5App() {
    val navController = rememberNavController()
    val colors = MT5ThemeProvider.colors

    Scaffold(
        bottomBar = {
            MT5BottomNavigation(navController = navController, colors = colors)
        },
        containerColor = colors.backgroundPrimary
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MT5Screen.Quotes.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MT5Screen.Quotes.route) { QuotesScreen(navController) }
            composable(MT5Screen.Chart.route) { ChartScreen() }
            composable(MT5Screen.Trade.route) { TradeScreen() }
            composable(MT5Screen.History.route) { HistoryScreen() }
            composable(MT5Screen.Messages.route) { MessagesScreen() }
        }
    }
}

@Composable
private fun MT5BottomNavigation(
    navController: NavHostController,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = colors.backgroundSecondary,
        tonalElevation = 0.dp,
        modifier = Modifier.height(56.dp)
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                },
                selected = selected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.tabSelected,
                    selectedTextColor = colors.tabSelected,
                    unselectedIconColor = colors.tabUnselected,
                    unselectedTextColor = colors.tabUnselected,
                    indicatorColor = colors.backgroundSecondary
                )
            )
        }
    }
}
