package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.repository.TournamentRepository
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.SuperAdminScreen
import com.example.ui.theme.*

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                KheloBDAdminApp()
            }
        }
    }
}

@Composable
fun KheloBDAdminApp() {
    val currentUser by TournamentRepository.currentUser.collectAsStateWithLifecycle()
    val isAuthenticated by TournamentRepository.isAuthenticated.collectAsStateWithLifecycle()
    val tournaments by TournamentRepository.tournaments.collectAsStateWithLifecycle()
    val matches by TournamentRepository.matches.collectAsStateWithLifecycle()
    val payments by TournamentRepository.payments.collectAsStateWithLifecycle()
    val activityLogs by TournamentRepository.activityLogs.collectAsStateWithLifecycle()
    val workerStatus by TournamentRepository.workerStatus.collectAsStateWithLifecycle()
    val allUsers by TournamentRepository.users.collectAsStateWithLifecycle()
    val settings by TournamentRepository.settings.collectAsStateWithLifecycle()
    val teams by TournamentRepository.teams.collectAsStateWithLifecycle()
    val userChallenges by TournamentRepository.userChallenges.collectAsStateWithLifecycle()
    val welcomePopup by TournamentRepository.welcomePopup.collectAsStateWithLifecycle()
    val isLoading by TournamentRepository.isLoading.collectAsStateWithLifecycle()

    var showSuperAdminScreen by remember { mutableStateOf(false) }

    // If authenticated as ADMIN or SUPER_ADMIN, show admin control panel
    val isAdminLoggedIn = isAuthenticated && (currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.SUPER_ADMIN)

    BackHandler(enabled = showSuperAdminScreen) {
        showSuperAdminScreen = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBg,
        shape = RectangleShape
    ) {
        if (!isAdminLoggedIn) {
            AdminLoginScreen(
                onLoginSuccess = { }
            )
        } else if (showSuperAdminScreen) {
            SuperAdminScreen(
                currentUser = currentUser,
                allUsers = allUsers,
                settings = settings,
                onClose = { showSuperAdminScreen = false },
                onUpdateSettings = { TournamentRepository.updateSystemSettings(it) },
                onSetUserRole = { uid, role -> TournamentRepository.setUserRole(uid, role) }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Admin App Top Bar
                Surface(
                    color = DarkSurface,
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
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(KheloGreen, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("KHELO BD ADMIN APP", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                                Text("Logged in as ${currentUser.username} (${currentUser.role.name})", color = TextMuted, fontSize = 11.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (currentUser.role == UserRole.SUPER_ADMIN) {
                                OutlinedButton(
                                    onClick = { showSuperAdminScreen = true },
                                    shape = RectangleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EsportsGold),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = EsportsGold, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Super Admin", color = EsportsGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            IconButton(
                                onClick = { TournamentRepository.logout() },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = EsportsRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                AdminDashboardScreen(
                    tournaments = tournaments,
                    matches = matches,
                    payments = payments,
                    activityLogs = activityLogs,
                    allUsers = allUsers,
                    teams = teams,
                    userChallenges = userChallenges,
                    welcomePopup = welcomePopup,
                    workerStatus = workerStatus,
                    onClose = { TournamentRepository.logout() },
                    onCreateTournament = { TournamentRepository.createTournament(it) },
                    onAutoGenerateFixtures = { TournamentRepository.automateGenerateGroupsAndFixtures(it) },
                    onSetRoomCredentials = { matchId, roomId, pass -> TournamentRepository.setRoomCredentials(matchId, roomId, pass) },
                    onVerifyResult = { matchId, winnerId -> TournamentRepository.verifyMatchResult(matchId, winnerId) },
                    onApprovePayment = { TournamentRepository.approvePayment(it) },
                    onRejectPayment = { id, reason -> TournamentRepository.rejectPayment(id, reason) },
                    onTriggerCron = { TournamentRepository.triggerWorkerNow() },
                    onUpdateTournamentStatus = { id, status -> TournamentRepository.updateTournamentStatus(id, status) },
                    onSetTournamentRoom = { id, roomId, pass, visible -> TournamentRepository.setTournamentRoom(id, roomId, pass, visible) },
                    onDeleteTournament = { TournamentRepository.deleteTournament(it) },
                    onDeclareChallengeWinner = { id, winnerUid -> TournamentRepository.declareUserChallengeWinner(id, winnerUid) },
                    onDeleteUserChallenge = { TournamentRepository.deleteUserChallenge(it) },
                    onUpdateWelcomePopup = { TournamentRepository.updateWelcomePopup(it) },
                    onAdjustUserWallet = { uid, delta -> TournamentRepository.adjustUserWallet(uid, delta) },
                    onSetUserBanned = { uid, banned -> TournamentRepository.setUserBanned(uid, banned) },
                    onDeleteUser = { TournamentRepository.deleteUser(it) },
                    onDeleteTeam = { TournamentRepository.deleteTeam(it) }
                )
            }
        }
    }

    com.example.ui.components.LoadingOverlay(visible = isLoading)
    }
}

@Composable
fun AdminLoginScreen(
    onLoginSuccess: (UserProfile) -> Unit
) {
    var adminUsername by remember { mutableStateOf("adminnoyon") }
    var adminPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFF221703), RectangleShape)
                .border(1.dp, EsportsGold, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = EsportsGold, modifier = Modifier.size(34.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "KHELO BD ADMIN PORTAL",
            color = EsportsGold,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            letterSpacing = 1.sp
        )
        Text(
            text = "RESTRICTED TO PLATFORM ADMINISTRATORS",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ADMINISTRATOR AUTHENTICATION",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )

                OutlinedTextField(
                    value = adminUsername,
                    onValueChange = {
                        adminUsername = it
                        errorMessage = null
                    },
                    label = { Text("Admin Username") },
                    singleLine = true,
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EsportsGold,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = EsportsGold,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_portal_username")
                )

                OutlinedTextField(
                    value = adminPassword,
                    onValueChange = {
                        adminPassword = it
                        errorMessage = null
                    },
                    label = { Text("Admin Password") },
                    placeholder = { Text("Enter admin secret") },
                    singleLine = true,
                    shape = RectangleShape,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password",
                                tint = TextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EsportsGold,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = EsportsGold,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("admin_portal_password")
                )

                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFF2E1212),
                        shape = RectangleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = EsportsRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (adminUsername.isBlank() || adminPassword.isBlank()) {
                            errorMessage = "Please enter admin username and password."
                            return@Button
                        }
                        isLoading = true
                        val result = TournamentRepository.authenticate(adminUsername, adminPassword)
                        isLoading = false
                        if (result.isSuccess) {
                            val user = result.getOrThrow()
                            if (user.role == UserRole.ADMIN || user.role == UserRole.SUPER_ADMIN) {
                                onLoginSuccess(user)
                            } else {
                                errorMessage = "Access denied. Only authorized administrators can access this portal."
                                TournamentRepository.logout()
                            }
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Authentication failed."
                        }
                    },
                    enabled = !isLoading,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = EsportsGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("admin_portal_login_button")
                ) {
                    Text(
                        text = "Access Admin Control Panel",
                        color = DarkBg,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                // Quick credential fill helper for convenience
                OutlinedButton(
                    onClick = {
                        adminUsername = TournamentRepository.ADMIN_USERNAME
                        adminPassword = TournamentRepository.ADMIN_PASSWORD
                    },
                    shape = RectangleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("Auto-fill admin credentials", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}
