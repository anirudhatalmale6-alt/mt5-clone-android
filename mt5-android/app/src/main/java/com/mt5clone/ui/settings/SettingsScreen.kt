package com.mt5clone.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mt5clone.ui.common.theme.MT5ThemeProvider

@Composable
fun SettingsScreen() {
    val colors = MT5ThemeProvider.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.backgroundSecondary)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Settings",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Account section
            item {
                SettingsSectionHeader("Account", colors)
            }
            item {
                AccountInfoCard(colors)
            }

            // Charts section
            item {
                SettingsSectionHeader("Charts", colors)
            }
            item {
                SettingsItem(Icons.Default.Palette, "Color Scheme", "Dark", colors)
            }
            item {
                SettingsItem(Icons.Default.GridOn, "Grid", "Enabled", colors)
            }
            item {
                SettingsItem(Icons.Default.Timeline, "Chart Type", "Candles", colors)
            }
            item {
                SettingsItem(Icons.Default.ZoomIn, "Scale", "Auto", colors)
            }

            // Trading section
            item {
                SettingsSectionHeader("Trading", colors)
            }
            item {
                SettingsItem(Icons.Default.Speed, "Trade Execution", "Instant", colors)
            }
            item {
                SettingsItem(Icons.Default.Notifications, "Notifications", "Enabled", colors)
            }
            item {
                SettingsItem(Icons.Default.Vibration, "Vibration", "On trade", colors)
            }

            // General section
            item {
                SettingsSectionHeader("General", colors)
            }
            item {
                SettingsItem(Icons.Default.Language, "Language", "English", colors)
            }
            item {
                SettingsItem(Icons.Default.Info, "About", "MT5 Clone v1.0.0", colors)
            }

            // Padding at bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    Text(
        text = title,
        color = colors.accentBlue,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun AccountInfoCard(colors: com.mt5clone.ui.common.theme.MT5ColorScheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Demo Account",
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Server: MT5Clone-Demo",
            color = colors.textSecondary,
            fontSize = 11.sp
        )
        Text(
            text = "Account: #12345678",
            color = colors.textSecondary,
            fontSize = 11.sp
        )
        Text(
            text = "Leverage: 1:100",
            color = colors.textSecondary,
            fontSize = 11.sp
        )
        Text(
            text = "Balance: 10,000.00 USD",
            color = colors.textSecondary,
            fontSize = 11.sp
        )
    }
    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    value: String,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = colors.textSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
}
