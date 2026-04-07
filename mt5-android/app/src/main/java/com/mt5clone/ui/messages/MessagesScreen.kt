package com.mt5clone.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mt5clone.ui.common.theme.MT5ThemeProvider

@Composable
fun MessagesScreen() {
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
                text = "Messages",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Mailbox tabs
        TabRow(
            selectedTabIndex = 0,
            containerColor = colors.backgroundSecondary,
            contentColor = colors.tabSelected,
            divider = { HorizontalDivider(color = colors.divider, thickness = 0.5.dp) }
        ) {
            Tab(
                selected = true,
                onClick = { },
                text = { Text("Inbox", fontSize = 12.sp, color = colors.tabSelected) }
            )
            Tab(
                selected = false,
                onClick = { },
                text = { Text("Notifications", fontSize = 12.sp, color = colors.textSecondary) }
            )
        }

        // Empty state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No messages",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "System messages and notifications will appear here",
                    color = colors.textTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
