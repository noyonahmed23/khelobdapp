package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.repository.TournamentRepository
import com.example.ui.components.KheloTopBar
import com.example.ui.components.LoadingOverlay
import com.example.ui.components.RoleSwitchDialog
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.SuperAdminScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.challenges.UserChallengeScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.WelcomePopupDialog
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.matches.MatchesScreen
import com.example.ui.screens.notifications.NotificationsDialog
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.PublicProfileDialog
import com.example.ui.screens.teams.TeamsScreen
import com.example.ui.screens.tournaments.TournamentDetailsDialog
import com.example.ui.screens.tournaments.TournamentRegisterDialog
import com.example.ui.screens.tournaments.TournamentsScreen
import com.example.ui.theme.*

enum class AppNavDestination(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    TOURNAMENTS("Tournaments", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    MATCHES("Matches", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    CHALLENGES("1v1", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    TEAMS("Squads", Icons.Filled.Groups, Icons.Outlined.Groups),
    LEADERBOARD("Rankings", Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                KheloBDApp()
            }
        }
    }
}

@Composable
fun KheloBDApp() {
    val isAuthenticated by TournamentRepository.isAuthenticated.collectAsStateWithLifecycle()
    val currentUser by TournamentRepository.currentUser.collectAsStateWithLifecycle()
    val tournaments by TournamentRepository.tournaments.collectAsStateWithLifecycle()
    val matches by TournamentRepository.matches.collectAsStateWithLifecycle()
    val standings by TournamentRepository.standings.collectAsStateWithLifecycle()
    val teams by TournamentRepository.teams.collectAsStateWithLifecycle()
    val challenges by TournamentRepository.challenges.collectAsStateWithLifecycle()
    val payments by TournamentRepository.payments.collectAsStateWithLifecycle()
    val notifications by TournamentRepository.notifications.collectAsStateWithLifecycle()
    val activityLogs by TournamentRepository.activityLogs.collectAsStateWithLifecycle()
    val settings by TournamentRepository.settings.collectAsStateWithLifecycle()
    val allUsers by TournamentRepository.users.collectAsStateWithLifecycle()
    val registrations by TournamentRepository.registrations.collectAsStateWithLifecycle()
    val workerStatus by TournamentRepository.workerStatus.collectAsStateWithLifecycle()
    val userChallenges by TournamentRepository.userChallenges.collectAsStateWithLifecycle()
    val welcomePopup by TournamentRepository.welcomePopup.collectAsStateWithLifecycle()
    val isLoading by TournamentRepository.isLoading.collectAsStateWithLifecycle()

    var currentDestination by remember { mutableStateOf(AppNavDestination.HOME) }
    var showAdminDashboard by remember { mutableStateOf(false) }
    var showSuperAdminScreen by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showRoleSwitchDialog by remember { mutableStateOf(false) }
    var dismissedWelcomePopup by remember { mutableStateOf(false) }
    var viewingProfile by remember { mutableStateOf<UserProfile?>(null) }

    var selectedTournamentFromHome by remember { mutableStateOf<Tournament?>(null) }
    var showRegisterDialogFromHome by remember { mutableStateOf(false) }

    if (!isAuthenticated) {
        AuthScreen(
            onLoginSuccess = { user ->
                if (BuildConfig.ADMIN_APP && (user.role == UserRole.ADMIN || user.role == UserRole.SUPER_ADMIN)) {
                    showAdminDashboard = true
                }
            }
        )
        return
    }

    BackHandler(
        enabled = viewingProfile != null ||
            showSuperAdminScreen ||
            showAdminDashboard ||
            selectedTournamentFromHome != null ||
            currentDestination != AppNavDestination.HOME
    ) {
        when {
            viewingProfile != null -> viewingProfile = null
            showSuperAdminScreen -> showSuperAdminScreen = false
            showAdminDashboard -> showAdminDashboard = false
            selectedTournamentFromHome != null -> selectedTournamentFromHome = null
            currentDestination != AppNavDestination.HOME -> currentDestination = AppNavDestination.HOME
        }
    }

    val unreadNotificationsCount =
        notifications.count { it.userId == currentUser.uid && !it.isRead }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            if (!showAdminDashboard && !showSuperAdminScreen) {
                KheloTopBar(
                    currentUser = currentUser,
                    unreadNotificationsCount = unreadNotificationsCount,
                    onNotificationsClick = {
                        showNotificationsDialog = true
                        TournamentRepository.markNotificationsAsRead()
                    },
                    onRoleSwitchClick = {
                        if (currentUser.role != UserRole.USER) {
                            showRoleSwitchDialog = true
                        }
                    },
                    onAdminClick = {
                        if (currentUser.role != UserRole.USER) {
                            showAdminDashboard = true
                        }
                    },
                    onSuperAdminClick = {
                        if (currentUser.role == UserRole.SUPER_ADMIN) {
                            showSuperAdminScreen = true
                        }
                    },
                    onLogoutClick = { TournamentRepository.logout() },
                    showAdminControls = BuildConfig.ADMIN_APP
                )
            }
        },
        bottomBar = {
            if (!showAdminDashboard && !showSuperAdminScreen) {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    AppNavDestination.entries.forEach { destination ->
                        val isSelected = currentDestination == destination

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) {
                                        destination.selectedIcon
                                    } else {
                                        destination.unselectedIcon
                                    },
                                    contentDescription = destination.title,
                                    tint = if (isSelected) KheloGreenBright else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) {
                                        FontWeight.Black
                                    } else {
                                        FontWeight.Bold
                                    },
                                    color = if (isSelected) {
                                        KheloGreenBright
                                    } else {
                                        TextMuted
                                    }
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = KheloGreenContainer
                            ),
                            modifier = Modifier.testTag(
                                "nav_item_${destination.name.lowercase()}"
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                showSuperAdminScreen -> {
                    SuperAdminScreen(
                        currentUser = currentUser,
                        allUsers = allUsers,
                        settings = settings,
                        onClose = { showSuperAdminScreen = false },
                        onUpdateSettings = {
                            TournamentRepository.updateSystemSettings(it)
                        },
                        onSetUserRole = { uid, role ->
                            TournamentRepository.setUserRole(uid, role)
                        }
                    )
                }

                showAdminDashboard -> {
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
                        onClose = { showAdminDashboard = false },
                        onCreateTournament = {
                            TournamentRepository.createTournament(it)
                        },
                        onAutoGenerateFixtures = {
                            TournamentRepository.automateGenerateGroupsAndFixtures(it)
                        },
                        onSetRoomCredentials = { matchId, roomId, pass ->
                            TournamentRepository.setRoomCredentials(
                                matchId,
                                roomId,
                                pass
                            )
                        },
                        onVerifyResult = { matchId, winnerId ->
                            TournamentRepository.verifyMatchResult(
                                matchId,
                                winnerId
                            )
                        },
                        onApprovePayment = {
                            TournamentRepository.approvePayment(it)
                        },
                        onRejectPayment = { id, reason ->
                            TournamentRepository.rejectPayment(
                                id,
                                reason
                            )
                        },
                        onTriggerCron = {
                            TournamentRepository.triggerWorkerNow()
                        },
                        onUpdateTournamentStatus = { id, status ->
                            TournamentRepository.updateTournamentStatus(
                                id,
                                status
                            )
                        },
                        onSetTournamentRoom = { id, roomId, pass, visible ->
                            TournamentRepository.setTournamentRoom(
                                id,
                                roomId,
                                pass,
                                visible
                            )
                        },
                        onDeleteTournament = {
                            TournamentRepository.deleteTournament(it)
                        },
                        onDeclareChallengeWinner = { id, winnerUid ->
                            TournamentRepository.declareUserChallengeWinner(
                                id,
                                winnerUid
                            )
                        },
                        onDeleteUserChallenge = {
                            TournamentRepository.deleteUserChallenge(it)
                        },
                        onUpdateWelcomePopup = {
                            TournamentRepository.updateWelcomePopup(it)
                        },
                        onAdjustUserWallet = { uid, delta ->
                            TournamentRepository.adjustUserWallet(
                                uid,
                                delta
                            )
                        },
                        onSetUserBanned = { uid, banned ->
                            TournamentRepository.setUserBanned(
                                uid,
                                banned
                            )
                        },
                        onDeleteUser = {
                            TournamentRepository.deleteUser(it)
                        },
                        onDeleteTeam = {
                            TournamentRepository.deleteTeam(it)
                        }
                    )
                }

                else -> {
                    when (currentDestination) {
                        AppNavDestination.HOME -> {
                            HomeScreen(
                                currentUser = currentUser,
                                tournaments = tournaments,
                                matches = matches,
                                onSelectTournament = {
                                    selectedTournamentFromHome = it
                                },
                                onNavigateTournaments = {
                                    currentDestination = AppNavDestination.TOURNAMENTS
                                },
                                onNavigateMatches = {
                                    currentDestination = AppNavDestination.MATCHES
                                },
                                onNavigateTeams = {
                                    currentDestination = AppNavDestination.TEAMS
                                },
                                onNavigateLeaderboard = {
                                    currentDestination = AppNavDestination.LEADERBOARD
                                },
                                onNavigateProfile = {
                                    currentDestination = AppNavDestination.PROFILE
                                },
                                onMatchClick = {
                                    currentDestination = AppNavDestination.MATCHES
                                }
                            )
                        }

                        AppNavDestination.TOURNAMENTS -> {
                            TournamentsScreen(
                                tournaments = tournaments,
                                standings = standings,
                                matches = matches,
                                registrations = registrations,
                                currentUser = currentUser,
                                onRegister = { tour, gameUid, method, trxId ->
                                    TournamentRepository.registerForTournament(
                                        tour,
                                        gameUid,
                                        method,
                                        trxId
                                    )
                                }
                            )
                        }

                        AppNavDestination.MATCHES -> {
                            MatchesScreen(
                                matches = matches,
                                currentUser = currentUser,
                                onSubmitScore = { matchId, sA, sB, winId, proof ->
                                    TournamentRepository.submitMatchScore(
                                        matchId,
                                        sA,
                                        sB,
                                        winId,
                                        proof
                                    )
                                }
                            )
                        }

                        AppNavDestination.CHALLENGES -> {
                            UserChallengeScreen(
                                currentUser = currentUser,
                                allUsers = allUsers,
                                userChallenges = userChallenges,
                                onSendChallenge = { uid, name ->
                                    TournamentRepository.sendUserChallenge(uid, name)
                                },
                                onAcceptChallenge = {
                                    TournamentRepository.acceptUserChallenge(it)
                                },
                                onRejectChallenge = {
                                    TournamentRepository.rejectUserChallenge(it)
                                },
                                onSetRoomCredentials = { id, roomId, pass ->
                                    TournamentRepository.setUserChallengeRoom(
                                        id,
                                        roomId,
                                        pass
                                    )
                                },
                                onSubmitProof = { id, proof ->
                                    TournamentRepository.submitUserChallengeProof(
                                        id,
                                        proof
                                    )
                                },
                                onViewProfile = {
                                    user -> viewingProfile = user
                                }
                            )
                        }

                        AppNavDestination.TEAMS -> {
                            TeamsScreen(
                                teams = teams,
                                challenges = challenges,
                                currentUser = currentUser,
                                allUsers = allUsers,
                                onCreateTeam = {
                                    name,
                                    tag,
                                    profileImageUrl,
                                    bannerUrl
                                    ->
                                    TournamentRepository.createTeam(
                                        name,
                                        tag,
                                        profileImageUrl,
                                        bannerUrl
                                    )
                                },
                                onChallengeTeam = { targetId, game, stake ->
                                    TournamentRepository.challengeTeam(
                                        targetId,
                                        game,
                                        stake
                                    )
                                },
                                onAcceptChallenge = {
                                    TournamentRepository.acceptChallenge(it)
                                },
                                onSendJoinRequest = {
                                    TournamentRepository.sendJoinRequest(it)
                                },
                                onRespondJoinRequest = {
                                    teamId,
                                    userId,
                                    accept
                                    ->
                                    TournamentRepository.respondJoinRequest(
                                        teamId,
                                        userId,
                                        accept
                                    )
                                },
                                onRemoveMember = {
                                    teamId,
                                    userId
                                    ->
                                    TournamentRepository.removeTeamMember(
                                        teamId,
                                        userId
                                    )
                                }
                            )
                        }

                        AppNavDestination.LEADERBOARD -> {
                            LeaderboardScreen(
                                users = allUsers,
                                teams = teams
                            )
                        }

                        AppNavDestination.PROFILE -> {
                            ProfileScreen(
                                currentUser = currentUser,
                                payments = payments,
                                settings = settings,
                                allUsersRanking = allUsers,
                                onUpdateProfile = {
                                    name,
                                    gameUid,
                                    game
                                    ->
                                    TournamentRepository.updateProfile(
                                        name,
                                        gameUid,
                                        game
                                    )
                                },
                                onProfileImagePicked = { url ->
                                    TournamentRepository.updateProfileImages(
                                        url,
                                        null
                                    )
                                },
                                onBannerImagePicked = { url ->
                                    TournamentRepository.updateProfileImages(
                                        null,
                                        url
                                    )
                                },
                                onRequestDeposit = {
                                    amt,
                                    method,
                                    senderNumber,
                                    trxId
                                    ->
                                    TournamentRepository.requestDeposit(
                                        amt,
                                        method,
                                        senderNumber,
                                        trxId
                                    )
                                },
                                onLogout = {
                                    TournamentRepository.logout()
                                }
                            )
                        }
                    }
                }
            }

            LoadingOverlay(visible = isLoading)
        }
    }

    if (
        welcomePopup.isVisible &&
        !dismissedWelcomePopup &&
        !showAdminDashboard &&
        !showSuperAdminScreen
    ) {
        WelcomePopupDialog(
            config = welcomePopup,
            onDismiss = {
                dismissedWelcomePopup = true
            }
        )
    }

    viewingProfile?.let { profile ->
        PublicProfileDialog(
            user = profile,
            currentUserUid = currentUser.uid,
            allUsersRanking = allUsers,
            onDismiss = {
                viewingProfile = null
            },
            onChallengeClick = { opponentUid, opponentName ->
                viewingProfile = null
                TournamentRepository.sendUserChallenge(
                    opponentUid,
                    opponentName
                )
                currentDestination = AppNavDestination.CHALLENGES
            }
        )
    }

    selectedTournamentFromHome?.let { tour ->
        TournamentDetailsDialog(
            tournament = tour,
            standings = standings.filter {
                it.tournamentId == tour.id
            },
            matches = matches.filter {
                it.tournamentId == tour.id
            },
            registrations = registrations.filter {
                it.tournamentId == tour.id
            },
            isAlreadyRegistered = registrations.any {
                it.tournamentId == tour.id &&
                    it.userId == currentUser.uid
            },
            onDismiss = {
                selectedTournamentFromHome = null
            },
            onJoinClick = {
                showRegisterDialogFromHome = true
            }
        )
    }

    if (
        showRegisterDialogFromHome &&
        selectedTournamentFromHome != null
    ) {
        TournamentRegisterDialog(
            tournament = selectedTournamentFromHome!!,
            currentUser = currentUser,
            onDismiss = {
                showRegisterDialogFromHome = false
            },
            onConfirmRegistration = { gameUid, method, trxId ->
                val result =
                    TournamentRepository.registerForTournament(
                        selectedTournamentFromHome!!,
                        gameUid,
                        method,
                        trxId
                    )

                if (result.isSuccess) {
                    showRegisterDialogFromHome = false
                    selectedTournamentFromHome = null
                }

                result
            }
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            notifications = notifications.filter {
                it.userId == currentUser.uid
            },
            onDismiss = {
                showNotificationsDialog = false
            }
        )
    }

    if (showRoleSwitchDialog) {
        RoleSwitchDialog(
            currentRole = currentUser.role,
            onDismiss = {
                showRoleSwitchDialog = false
            },
            onSelectRole = { role ->
                TournamentRepository.switchUserRole(role)

                if (role == UserRole.ADMIN) {
                    showAdminDashboard = true
                } else if (role == UserRole.SUPER_ADMIN) {
                    showSuperAdminScreen = true
                }
            }
        )
    }
}
