package com.example.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.NotificationItem
import com.example.ui.components.borderStroke
import com.example.ui.theme.*

@Composable
fun NotificationsDialog(
    notifications: List<NotificationItem>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .testTag("notifications_dialog"),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Notifications", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notifications yet.", color = TextMuted, fontSize = 11.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                                shape = RectangleShape
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(item.title, color = KheloGreenBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(item.message, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
