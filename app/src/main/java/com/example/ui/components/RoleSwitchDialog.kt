package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import com.example.data.model.UserRole
import com.example.ui.theme.*

@Composable
fun RoleSwitchDialog(
    currentRole: UserRole,
    onDismiss: () -> Unit,
    onSelectRole: (UserRole) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Active Role / Mode", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Allows testing and reviewing Player App, Admin Panel, and Super Admin features.",
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                RoleOptionCard(
                    title = "Player / Team Captain",
                    subtitle = "Join tournaments, view private match rooms, submit scores, manage team & wallet",
                    icon = Icons.Default.SportsEsports,
                    role = UserRole.USER,
                    isSelected = currentRole == UserRole.USER,
                    color = KheloGreenBright,
                    onClick = {
                        onSelectRole(UserRole.USER)
                        onDismiss()
                    }
                )

                RoleOptionCard(
                    title = "Admin (adminnoyon)",
                    subtitle = "Manage tournaments, auto-generate fixtures, assign custom rooms, verify results & payments",
                    icon = Icons.Default.AdminPanelSettings,
                    role = UserRole.ADMIN,
                    isSelected = currentRole == UserRole.ADMIN,
                    color = EsportsCyan,
                    onClick = {
                        onSelectRole(UserRole.ADMIN)
                        onDismiss()
                    }
                )

                RoleOptionCard(
                    title = "Super Admin (/LfyBdQzjhr...)",
                    subtitle = "Global settings, emergency controls, admin role assignments & security logs",
                    icon = Icons.Default.Security,
                    role = UserRole.SUPER_ADMIN,
                    isSelected = currentRole == UserRole.SUPER_ADMIN,
                    color = EsportsGold,
                    onClick = {
                        onSelectRole(UserRole.SUPER_ADMIN)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun RoleOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    role: UserRole,
    isSelected: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RectangleShape,
        color = if (isSelected) DarkSurfaceCard else DarkSurface,
        border = borderStroke(1.dp, if (isSelected) color else DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (isSelected) color else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(subtitle, color = TextSecondary, fontSize = 9.sp, lineHeight = 13.sp)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
        }
    }
}
