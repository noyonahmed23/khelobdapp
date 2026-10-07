package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.borderStroke
import com.example.ui.theme.*

@Composable
fun SuperAdminScreen(
    currentUser: UserProfile,
    allUsers: List<UserProfile>,
    settings: SystemSettings,
    onClose: () -> Unit,
    onUpdateSettings: (SystemSettings) -> Unit,
    onSetUserRole: (userId: String, newRole: UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAuthorized = currentUser.role == UserRole.SUPER_ADMIN

    var bkashNum by remember { mutableStateOf(settings.bkashNumber) }
    var nagadNum by remember { mutableStateOf(settings.nagadNumber) }
    var refBonus by remember { mutableStateOf(settings.referralBonus.toInt().toString()) }
    var maintenanceMode by remember { mutableStateOf(settings.maintenanceMode) }
    var savedMessage by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Surface(
            color = Color(0xFF221703),
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EsportsGold, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("SUPER ADMIN CONSOLE", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                        Text("Protected Route: /LfyBdQzjhrRxKyfVum1YeCcssk23", color = TextMuted, fontSize = 11.sp)
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        }

        if (!isAuthorized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = borderStroke(1.dp, EsportsRed),
                    shape = RectangleShape
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.GppBad, contentDescription = null, tint = EsportsRed, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("ACCESS RESTRICTED", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text(
                            "This protected route requires SUPER_ADMIN role with server-side authorization claims.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onClose,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard)
                        ) {
                            Text("Return to App", color = TextPrimary)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("PLATFORM FINANCIAL & BONUS CONFIGURATION", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        shape = RectangleShape,
                        border = borderStroke(1.dp, DarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = bkashNum,
                                onValueChange = { bkashNum = it },
                                label = { Text("bKash Merchant / Personal Account") },
                                shape = RectangleShape,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("super_admin_bkash_input")
                            )

                            OutlinedTextField(
                                value = nagadNum,
                                onValueChange = { nagadNum = it },
                                label = { Text("Nagad Merchant / Personal Account") },
                                shape = RectangleShape,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                                modifier = Modifier.fillMaxWidth().testTag("super_admin_nagad_input")
                            )

                            OutlinedTextField(
                                value = refBonus,
                                onValueChange = { refBonus = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Referral Bonus Reward (৳ BDT)") },
                                shape = RectangleShape,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Emergency Maintenance Mode", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Blocks registrations and tournaments", color = TextMuted, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = maintenanceMode,
                                    onCheckedChange = { maintenanceMode = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = EsportsRed, checkedTrackColor = Color(0xFF421515))
                                )
                            }

                            Button(
                                onClick = {
                                    val newSettings = settings.copy(
                                        bkashNumber = bkashNum,
                                        nagadNumber = nagadNum,
                                        referralBonus = refBonus.toDoubleOrNull() ?: 50.0,
                                        maintenanceMode = maintenanceMode
                                    )
                                    onUpdateSettings(newSettings)
                                    savedMessage = true
                                },
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = EsportsGold),
                                modifier = Modifier.fillMaxWidth().height(36.dp).testTag("save_super_admin_settings_btn")
                            ) {
                                Text("Save Global Configuration", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            if (savedMessage) {
                                Text("Platform settings updated successfully!", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("ADMINISTRATOR ROLE & ACCESS MANAGEMENT", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                }

                items(allUsers) { user ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        shape = RectangleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${user.email} • ${user.role.name}", color = if (user.role == UserRole.ADMIN) EsportsCyan else if (user.role == UserRole.SUPER_ADMIN) EsportsGold else TextSecondary, fontSize = 11.sp)
                            }

                            if (user.uid != currentUser.uid) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (user.role != UserRole.ADMIN) {
                                        Button(
                                            onClick = { onSetUserRole(user.uid, UserRole.ADMIN) },
                                            shape = RectangleShape,
                                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreenContainer),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Make Admin", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { onSetUserRole(user.uid, UserRole.USER) },
                                            shape = RectangleShape,
                                            border = borderStroke(1.dp, EsportsRed),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("Demote", color = EsportsRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
