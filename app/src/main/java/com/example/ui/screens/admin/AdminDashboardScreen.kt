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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.Avatar
import com.example.ui.components.MatchStatusBadge
import com.example.ui.components.TournamentStatusBadge
import com.example.ui.components.borderStroke
import com.example.ui.components.rememberImagePicker
import com.example.data.firebase.StorageManager
import com.example.ui.screens.profile.ApkDownloadDialog
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen(
    tournaments: List<Tournament>,
    matches: List<MatchFixture>,
    payments: List<PaymentTransaction>,
    activityLogs: List<AdminActivityLog>,
    allUsers: List<UserProfile> = emptyList(),
    teams: List<Team> = emptyList(),
    userChallenges: List<UserChallenge> = emptyList(),
    welcomePopup: WelcomePopupConfig = WelcomePopupConfig(),
    workerStatus: WorkerStatus = WorkerStatus(),
    onClose: () -> Unit,
    onCreateTournament: (Tournament) -> Unit,
    onAutoGenerateFixtures: (tournamentId: String) -> Unit,
    onSetRoomCredentials: (matchId: String, roomId: String, pass: String) -> Unit,
    onVerifyResult: (matchId: String, overrideWinnerId: String?) -> Unit,
    onApprovePayment: (paymentId: String) -> Unit,
    onRejectPayment: (paymentId: String, reason: String) -> Unit,
    onTriggerCron: () -> String = { "" },
    onUpdateTournamentStatus: (tournamentId: String, status: TournamentStatus) -> Unit = { _, _ -> },
    onSetTournamentRoom: (tournamentId: String, roomId: String, password: String, visible: Boolean) -> Unit = { _, _, _, _ -> },
    onDeleteTournament: (tournamentId: String) -> Unit = {},
    onDeclareChallengeWinner: (challengeId: String, winnerUid: String) -> Unit = { _, _ -> },
    onDeleteUserChallenge: (challengeId: String) -> Unit = {},
    onUpdateWelcomePopup: (WelcomePopupConfig) -> Unit = {},
    onAdjustUserWallet: (uid: String, delta: Double) -> Unit = { _, _ -> },
    onSetUserBanned: (uid: String, banned: Boolean) -> Unit = { _, _ -> },
    onDeleteUser: (uid: String) -> Unit = {},
    onDeleteTeam: (teamId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Tournaments", "Matches", "Challenge Proof", "Results Review", "Payments", "Welcome Popup", "Manage DB", "Audit Log")

    var showCreateTournamentDialog by remember { mutableStateOf(false) }
    var showApkDownloadDialog by remember { mutableStateOf(false) }
    var selectedMatchForRoom by remember { mutableStateOf<MatchFixture?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Admin Top Header
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
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ADMIN CONTROL PANEL", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showApkDownloadDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Download APKs", tint = EsportsGold)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurfaceCard,
            contentColor = KheloGreenBright,
            edgePadding = 10.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                val badgeCount = when (index) {
                    3 -> userChallenges.count { it.status == UserChallengeStatus.UNDER_REVIEW }
                    4 -> matches.count { it.status == MatchStatus.UNDER_REVIEW }
                    5 -> payments.count { it.status == PaymentStatus.PENDING }
                    else -> 0
                }
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) KheloGreenBright else TextSecondary
                            )
                            if (badgeCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    color = EsportsRed,
                                    shape = RectangleShape
                                ) {
                                    Text(
                                        text = "$badgeCount",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp)
        ) {
            when (selectedTab) {
                0 -> AdminOverviewTab(tournaments, matches, payments, workerStatus, onTriggerCron)
                1 -> AdminTournamentsTab(
                    tournaments = tournaments,
                    onCreateClick = { showCreateTournamentDialog = true },
                    onAutoGenerate = onAutoGenerateFixtures,
                    onUpdateStatus = onUpdateTournamentStatus,
                    onSetRoom = onSetTournamentRoom,
                    onDelete = onDeleteTournament
                )
                2 -> AdminMatchesTab(
                    matches = matches,
                    onAssignRoom = { selectedMatchForRoom = it }
                )
                3 -> AdminChallengeProofTab(
                    challenges = userChallenges,
                    onDeclareWinner = onDeclareChallengeWinner,
                    onDelete = onDeleteUserChallenge
                )
                4 -> AdminResultsReviewTab(
                    matches = matches.filter { it.status == MatchStatus.UNDER_REVIEW || it.status == MatchStatus.DISPUTED },
                    onVerify = onVerifyResult
                )
                5 -> AdminPaymentsTab(
                    payments = payments,
                    onApprove = onApprovePayment,
                    onReject = onRejectPayment
                )
                6 -> AdminWelcomePopupTab(
                    config = welcomePopup,
                    onSave = onUpdateWelcomePopup
                )
                7 -> AdminManageDbTab(
                    users = allUsers,
                    teams = teams,
                    onAdjustWallet = onAdjustUserWallet,
                    onSetBanned = onSetUserBanned,
                    onDeleteUser = onDeleteUser,
                    onDeleteTeam = onDeleteTeam
                )
                8 -> AdminAuditLogTab(logs = activityLogs)
            }
        }
    }

    if (showCreateTournamentDialog) {
        CreateTournamentDialog(
            onDismiss = { showCreateTournamentDialog = false },
            onCreate = {
                onCreateTournament(it)
                showCreateTournamentDialog = false
            }
        )
    }

    selectedMatchForRoom?.let { match ->
        AdminSetRoomDialog(
            match = match,
            onDismiss = { selectedMatchForRoom = null },
            onSet = { rId, pass ->
                onSetRoomCredentials(match.id, rId, pass)
                selectedMatchForRoom = null
            }
        )
    }

    if (showApkDownloadDialog) {
        ApkDownloadDialog(
            onDismiss = { showApkDownloadDialog = false }
        )
    }
}

@Composable
fun AdminOverviewTab(
    tournaments: List<Tournament>,
    matches: List<MatchFixture>,
    payments: List<PaymentTransaction>,
    workerStatus: WorkerStatus = WorkerStatus(),
    onTriggerCron: () -> String = { "" }
) {
    val totalRevenue = payments.filter { it.status == PaymentStatus.PAID }.sumOf { it.amount }
    val pendingResults = matches.count { it.status == MatchStatus.UNDER_REVIEW }
    val pendingPayments = payments.count { it.status == PaymentStatus.PENDING }
    var cronFeedback by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("OPERATIONAL OVERVIEW", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("ACTIVE TOURNAMENTS", "${tournaments.count { it.status != TournamentStatus.COMPLETED }}", KheloGreenBright, modifier = Modifier.weight(1f))
                MetricCard("LIVE / READY MATCHES", "${matches.count { it.status == MatchStatus.LIVE || it.status == MatchStatus.ROOM_READY }}", EsportsRed, modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("PENDING RESULTS", "$pendingResults", EsportsOrange, modifier = Modifier.weight(1f))
                MetricCard("PENDING PAYMENTS", "$pendingPayments", EsportsCyan, modifier = Modifier.weight(1f))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = borderStroke(1.dp, EsportsGold),
                shape = RectangleShape
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("TOTAL PLATFORM VOLUME (VERIFIED)", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("৳${totalRevenue.toInt()} BDT", color = EsportsGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Includes tournament registration fees & player deposits", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        // Automated Background Worker / Cron System
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = borderStroke(1.dp, KheloGreen),
                shape = RectangleShape
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BACKGROUND WORKER & CRON ENGINE", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Surface(
                            color = if (workerStatus.isRunning) Color(0xFF1B3B1B) else Color(0xFF3B1B1B),
                            shape = RectangleShape
                        ) {
                            Text(
                                text = if (workerStatus.isRunning) "ACTIVE (60s LOOP)" else "STANDBY",
                                color = if (workerStatus.isRunning) KheloGreenBright else EsportsRed,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Automated Cycles Completed: ${workerStatus.totalRunsCount} | Monitored Fixtures: ${workerStatus.activeJobsCount}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = workerStatus.lastSummary,
                        color = TextMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    if (cronFeedback != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = cronFeedback!!,
                            color = EsportsGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            cronFeedback = onTriggerCron()
                        },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("admin_trigger_cron_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger Automation Cron Cycle Now", color = DarkBg, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RectangleShape,
        color = DarkSurfaceElevated,
        border = borderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun AdminTournamentsTab(
    tournaments: List<Tournament>,
    onCreateClick: () -> Unit,
    onAutoGenerate: (tournamentId: String) -> Unit,
    onUpdateStatus: (tournamentId: String, status: TournamentStatus) -> Unit = { _, _ -> },
    onSetRoom: (tournamentId: String, roomId: String, password: String, visible: Boolean) -> Unit = { _, _, _, _ -> },
    onDelete: (tournamentId: String) -> Unit = {}
) {
    var roomDialogTournament by remember { mutableStateOf<Tournament?>(null) }
    var expandedStatus by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TOURNAMENT OPERATIONS", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Button(
                    onClick = onCreateClick,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("admin_create_tournament_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = DarkBg, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Tournament", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }

        items(tournaments) { tour ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tour.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        TournamentStatusBadge(status = tour.status)
                    }

                    Text("Game: ${tour.game} • Reg: ${tour.registeredCount}/${tour.maxParticipants} slots", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status selector
                    ExposedDropdownMenuBox(
                        expanded = expandedStatus == tour.id,
                        onExpandedChange = { expandedStatus = if (it) tour.id else null }
                    ) {
                        OutlinedTextField(
                            value = tour.status.name.replace("_", " "),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Status", fontSize = 9.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus == tour.id) },
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KheloGreen,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .height(46.dp)
                                .testTag("admin_tournament_status_${tour.id}")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStatus == tour.id,
                            onDismissRequest = { expandedStatus = null }
                        ) {
                            TournamentStatus.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.name.replace("_", " "), fontSize = 11.sp) },
                                    onClick = {
                                        onUpdateStatus(tour.id, status)
                                        expandedStatus = null
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Room credentials control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (tour.roomId.isNullOrBlank()) "Room: not set" else "Room: ${tour.roomId} / ${tour.roomPassword}",
                                color = if (tour.roomId.isNullOrBlank()) TextMuted else KheloGreenBright,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (tour.roomVisible) "Visible to participants" else "Hidden from participants",
                                color = if (tour.roomVisible) KheloGreenBright else TextMuted,
                                fontSize = 9.sp
                            )
                        }
                        Button(
                            onClick = { roomDialogTournament = tour },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp).testTag("admin_tour_room_btn_${tour.id}")
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = EsportsGold, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Room", color = TextPrimary, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (tour.status == TournamentStatus.REGISTRATION) {
                        Button(
                            onClick = { onAutoGenerate(tour.id) },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreenContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .testTag("auto_generate_fixtures_btn_${tour.id}")
                        ) {
                            Icon(Icons.Default.AutoMode, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Reg & Auto-Generate Fixtures", color = KheloGreenBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { onDelete(tour.id) },
                        shape = RectangleShape,
                        border = borderStroke(1.dp, EsportsRed),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth().height(30.dp).testTag("admin_delete_tournament_${tour.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = EsportsRed, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Tournament", color = EsportsRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    roomDialogTournament?.let { tour ->
        AdminTournamentRoomDialog(
            tournament = tour,
            onDismiss = { roomDialogTournament = null },
            onSave = { roomId, password, visible ->
                onSetRoom(tour.id, roomId, password, visible)
                roomDialogTournament = null
            }
        )
    }
}

@Composable
fun AdminTournamentRoomDialog(
    tournament: Tournament,
    onDismiss: () -> Unit,
    onSave: (roomId: String, password: String, visible: Boolean) -> Unit
) {
    var roomId by remember { mutableStateOf(tournament.roomId ?: "") }
    var password by remember { mutableStateOf(tournament.roomPassword ?: "") }
    var visible by remember { mutableStateOf(tournament.roomVisible) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, EsportsGold)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tournament Room Credentials", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(tournament.title, color = TextSecondary, fontSize = 11.sp)

                OutlinedTextField(
                    value = roomId,
                    onValueChange = { roomId = it },
                    label = { Text("Room ID") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Room Password") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show to registered participants", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Reveal Room ID & Password in-app", color = TextMuted, fontSize = 9.sp)
                    }
                    Switch(
                        checked = visible,
                        onCheckedChange = { visible = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBg, checkedTrackColor = KheloGreen)
                    )
                }

                Button(
                    onClick = { onSave(roomId, password, visible) },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = EsportsGold),
                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("admin_save_tour_room_btn")
                ) {
                    Text("Save Room Settings", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminMatchesTab(
    matches: List<MatchFixture>,
    onAssignRoom: (MatchFixture) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("MATCH ROOM & FIXTURE MANAGER", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }

        items(matches) { match ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${match.participantAName} vs ${match.participantBName}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        MatchStatusBadge(status = match.status)
                    }

                    Text("${match.tournamentTitle} (${match.groupName ?: match.round})", color = TextSecondary, fontSize = 10.sp)

                    if (match.roomId != null) {
                        Text("Room: ${match.roomId} | Pass: ${match.roomPassword}", color = KheloGreenBright, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { onAssignRoom(match) },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .testTag("admin_set_room_btn_${match.id}")
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = EsportsGold, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (match.roomId != null) "Update Room Credentials" else "Assign Custom Room ID & Password", color = TextPrimary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminResultsReviewTab(
    matches: List<MatchFixture>,
    onVerify: (matchId: String, overrideWinnerId: String?) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("RESULT VERIFICATION & DISPUTES", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }

        if (matches.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard), shape = RectangleShape) {
                    Text("No match results pending review.", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(14.dp))
                }
            }
        } else {
            items(matches) { match ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RectangleShape,
                    border = borderStroke(1.dp, EsportsOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${match.participantAName} vs ${match.participantBName}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(color = Color(0xFF382A0E), shape = RectangleShape) {
                                Text("NEEDS VERIFICATION", color = EsportsOrange, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Submitted Score: ${match.scoreA} - ${match.scoreB} (Winner: ${match.winnerName})", color = EsportsGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onVerify(match.id, null) },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .testTag("admin_approve_result_btn_${match.id}")
                        ) {
                            Text("Approve & Advance Standings", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPaymentsTab(
    payments: List<PaymentTransaction>,
    onApprove: (String) -> Unit,
    onReject: (String, String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("PAYMENT VERIFICATION QUEUE", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }

        items(payments) { trx ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RectangleShape,
                border = borderStroke(1.dp, if (trx.status == PaymentStatus.PENDING) EsportsCyan else DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${trx.userName} (${trx.method})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("৳${trx.amount.toInt()} BDT", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    if (trx.senderNumber.isNotBlank()) {
                        Text("Sender Mobile: ${trx.senderNumber}", color = KheloGreenBright, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    }

                    Text("TrxID: ${trx.transactionId} • ${trx.note}", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))

                    if (trx.status == PaymentStatus.PENDING) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { onApprove(trx.id) },
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .testTag("admin_approve_payment_btn_${trx.id}")
                            ) {
                                Text("Verify & Credit Wallet", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }

                            OutlinedButton(
                                onClick = { onReject(trx.id, "Invalid TrxID") },
                                shape = RectangleShape,
                                border = borderStroke(1.dp, EsportsRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                            ) {
                                Text("Reject", color = EsportsRed, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminChallengeProofTab(
    challenges: List<UserChallenge>,
    onDeclareWinner: (challengeId: String, winnerUid: String) -> Unit,
    onDelete: (challengeId: String) -> Unit
) {
    // Newest first; challenges needing a decision appear at the top
    val sorted = challenges.sortedWith(
        compareByDescending<UserChallenge> { it.status == UserChallengeStatus.UNDER_REVIEW }
            .thenByDescending { it.timestamp }
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("1v1 CHALLENGE PROOF REVIEW", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(
                "Winner receives ৳85 (platform fee ৳15 from the ৳100 pool). Review both screenshots before deciding.",
                color = TextMuted,
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (sorted.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard), shape = RectangleShape) {
                    Text("No 1v1 challenges yet.", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(14.dp))
                }
            }
        } else {
            items(sorted) { ch ->
                val needsReview = ch.status == UserChallengeStatus.UNDER_REVIEW
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RectangleShape,
                    border = borderStroke(1.dp, if (needsReview) EsportsOrange else DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${ch.challengerName}  vs  ${ch.opponentName}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = if (needsReview) Color(0xFF3B2A0F) else if (ch.status == UserChallengeStatus.COMPLETED) Color(0xFF132B1A) else DarkSurfaceCard,
                                shape = RectangleShape
                            ) {
                                Text(
                                    ch.status.name.replace("_", " "),
                                    color = if (needsReview) EsportsOrange else if (ch.status == UserChallengeStatus.COMPLETED) KheloGreenBright else TextSecondary,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            "${ch.game} • Pool ৳${(ch.stakeAmount * 2).toInt()} • ${if (ch.roomId != null) "Room ${ch.roomId}/${ch.roomPassword}" else "No room set"}",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Side-by-side proof screenshots
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProofColumn(
                                label = ch.challengerName,
                                url = ch.challengerProofUrl,
                                modifier = Modifier.weight(1f)
                            )
                            ProofColumn(
                                label = ch.opponentName,
                                url = ch.opponentProofUrl,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (ch.status == UserChallengeStatus.COMPLETED) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Winner: ${ch.winnerName ?: "—"} (credited ৳85)",
                                color = EsportsGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (needsReview) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Declare Winner:", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onDeclareWinner(ch.id, ch.challengerUid) },
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                                    modifier = Modifier.weight(1f).height(34.dp).testTag("admin_win_challenger_${ch.id}")
                                ) {
                                    Text(ch.challengerName, color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                }
                                Button(
                                    onClick = { onDeclareWinner(ch.id, ch.opponentUid) },
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = EsportsGold),
                                    modifier = Modifier.weight(1f).height(34.dp).testTag("admin_win_opponent_${ch.id}")
                                ) {
                                    Text(ch.opponentName, color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
                                }
                            }
                        }

                        if (ch.status != UserChallengeStatus.COMPLETED) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onDelete(ch.id) },
                                shape = RectangleShape,
                                border = borderStroke(1.dp, EsportsRed),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.fillMaxWidth().height(30.dp)
                            ) {
                                Text("Cancel / Void Challenge (refund not automatic)", color = EsportsRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProofColumn(label: String, url: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(DarkSurfaceCard, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = "$label proof",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HideImage, contentDescription = null, tint = TextMuted, modifier = Modifier.size(24.dp))
                    Text("No proof", color = TextMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
fun AdminWelcomePopupTab(
    config: WelcomePopupConfig,
    onSave: (WelcomePopupConfig) -> Unit
) {
    var title by remember(config.title) { mutableStateOf(config.title) }
    var message by remember(config.message) { mutableStateOf(config.message) }
    var buttonText by remember(config.buttonText) { mutableStateOf(config.buttonText) }
    var imageUrl by remember(config.imageUrl) { mutableStateOf(config.imageUrl) }
    var visible by remember(config.isVisible) { mutableStateOf(config.isVisible) }
    var uploading by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    val pickImage = rememberImagePicker(
        storagePath = StorageManager.getWelcomePopupPath(),
        onUploaded = { url -> imageUrl = url; saved = false },
        onError = {},
        onLoading = { uploading = it }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("WELCOME POPUP (USER APP)", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text("Controls the popup every user sees on app open.", color = TextMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RectangleShape,
                    border = borderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show Welcome Popup", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(if (visible) "Currently ON — visible to users" else "Currently OFF — hidden", color = if (visible) KheloGreenBright else TextMuted, fontSize = 9.sp)
                            }
                            Switch(
                                checked = visible,
                                onCheckedChange = { visible = it; saved = false },
                                colors = SwitchDefaults.colors(checkedThumbColor = DarkBg, checkedTrackColor = KheloGreen),
                                modifier = Modifier.testTag("welcome_popup_toggle")
                            )
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it; saved = false },
                            label = { Text("Title") },
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it; saved = false },
                            label = { Text("Message") },
                            minLines = 3,
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = buttonText,
                            onValueChange = { buttonText = it; saved = false },
                            label = { Text("Button Text") },
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Image picker + preview
                        Text("Popup Image (optional)", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(DarkSurfaceCard, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Welcome popup image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("No image selected", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = pickImage,
                                enabled = !uploading,
                                shape = RectangleShape,
                                border = borderStroke(1.dp, KheloGreen),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (uploading) "Uploading..." else "Upload Image", color = KheloGreenBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            if (imageUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { imageUrl = ""; saved = false },
                                    shape = RectangleShape,
                                    border = borderStroke(1.dp, EsportsRed),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Remove", color = EsportsRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onSave(
                                    WelcomePopupConfig(
                                        isVisible = visible,
                                        title = title,
                                        message = message,
                                        imageUrl = imageUrl,
                                        buttonText = buttonText
                                    )
                                )
                                saved = true
                            },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("save_welcome_popup_btn")
                        ) {
                            Text("Save Welcome Popup", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }

                        if (saved) {
                            Text("Saved! Users will see this on next app open.", color = KheloGreenBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (uploading) {
            com.example.ui.components.LoadingOverlay(visible = true, message = "Uploading image...")
        }
    }
}

@Composable
fun AdminManageDbTab(
    users: List<UserProfile>,
    teams: List<Team>,
    onAdjustWallet: (uid: String, delta: Double) -> Unit,
    onSetBanned: (uid: String, banned: Boolean) -> Unit,
    onDeleteUser: (uid: String) -> Unit,
    onDeleteTeam: (teamId: String) -> Unit
) {
    var walletTarget by remember { mutableStateOf<UserProfile?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("DATABASE MANAGEMENT", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text("Full CRUD over users, wallets and teams.", color = TextMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
        }

        item {
            Text("USERS (${users.size})", color = KheloGreenBright, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }

        items(users) { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RectangleShape,
                border = borderStroke(1.dp, if (user.isBanned) EsportsRed else DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(imageUrl = user.profilePictureUrl, label = user.username, size = 36.dp, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("৳${user.walletBalance.toInt()} • ${user.role.name}${if (user.isBanned) " • BANNED" else ""}", color = if (user.isBanned) EsportsRed else TextSecondary, fontSize = 9.sp)
                    }
                    Column(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { walletTarget = user },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreenContainer),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp).testTag("admin_wallet_${user.uid}")
                        ) {
                            Text("Wallet", color = KheloGreenBright, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onSetBanned(user.uid, !user.isBanned) },
                            shape = RectangleShape,
                            border = borderStroke(1.dp, EsportsOrange),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(if (user.isBanned) "Unban" else "Ban", color = EsportsOrange, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onDeleteUser(user.uid) },
                            shape = RectangleShape,
                            border = borderStroke(1.dp, EsportsRed),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp).testTag("admin_delete_user_${user.uid}")
                        ) {
                            Text("Delete", color = EsportsRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("TEAMS (${teams.size})", color = EsportsCyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
        }

        items(teams) { team ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${team.name} [${team.tag}]", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${team.members.size} members • Captain: ${team.captainName}", color = TextSecondary, fontSize = 9.sp)
                    }
                    OutlinedButton(
                        onClick = { onDeleteTeam(team.id) },
                        shape = RectangleShape,
                        border = borderStroke(1.dp, EsportsRed),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp).testTag("admin_delete_team_${team.id}")
                    ) {
                        Text("Delete Team", color = EsportsRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    walletTarget?.let { user ->
        AdminWalletDialog(
            user = user,
            onDismiss = { walletTarget = null },
            onApply = { delta ->
                onAdjustWallet(user.uid, delta)
                walletTarget = null
            }
        )
    }
}

@Composable
fun AdminWalletDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onApply: (delta: Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("credit") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, KheloGreen)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Adjust Wallet — ${user.username}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Current balance: ৳${user.walletBalance.toInt()}", color = EsportsGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = mode == "credit",
                        onClick = { mode = "credit" },
                        shape = RectangleShape,
                        label = { Text("Credit (+)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KheloGreen, selectedLabelColor = DarkBg)
                    )
                    FilterChip(
                        selected = mode == "debit",
                        onClick = { mode = "debit" },
                        shape = RectangleShape,
                        label = { Text("Debit (−)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EsportsRed, selectedLabelColor = Color.White)
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount (৳)") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (amt != 0.0) onApply(if (mode == "credit") amt else -amt)
                    },
                    enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("admin_apply_wallet_btn")
                ) {
                    Text("Apply", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminAuditLogTab(logs: List<AdminActivityLog>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Text("IMMUTABLE SYSTEM ACTIVITY LOG", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }

        items(logs) { log ->
            Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard), shape = RectangleShape) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.action, color = KheloGreenBright, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Text(log.adminName, color = TextMuted, fontSize = 9.sp)
                    }
                    Text(log.details, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
fun CreateTournamentDialog(
    onDismiss: () -> Unit,
    onCreate: (Tournament) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var game by remember { mutableStateOf("Free Fire") }
    var entryFee by remember { mutableStateOf("50") }
    var prizePool by remember { mutableStateOf("2500") }
    var maxSlots by remember { mutableStateOf("8") }
    var numGroups by remember { mutableStateOf("2") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create New Esports Tournament", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tournament Title") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("admin_tournament_title_input")
                )

                Text("Game:", color = TextSecondary, fontSize = 10.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Free Fire", "PUBG Mobile", "eFootball", "COD Mobile").forEach { g ->
                        FilterChip(
                            selected = game == g,
                            onClick = { game = g },
                            shape = RectangleShape,
                            label = { Text(g, fontSize = 8.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KheloGreen, selectedLabelColor = DarkBg),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = entryFee,
                        onValueChange = { entryFee = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Fee (৳)") },
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = prizePool,
                        onValueChange = { prizePool = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Prize (৳)") },
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = maxSlots,
                        onValueChange = { maxSlots = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Slots") },
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = numGroups,
                        onValueChange = { numGroups = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Groups") },
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val fee = entryFee.toDoubleOrNull() ?: 0.0
                        val prize = prizePool.toDoubleOrNull() ?: 1000.0
                        val slots = maxSlots.toIntOrNull() ?: 8
                        val grp = numGroups.toIntOrNull() ?: 2
                        val tour = Tournament(
                            title = title,
                            game = game,
                            description = "Official Khelo BD competitive championship.",
                            entryFee = fee,
                            prizePool = prize,
                            maxParticipants = slots,
                            numGroups = grp,
                            firstPrize = prize * 0.6,
                            secondPrize = prize * 0.3,
                            thirdPrize = prize * 0.1
                        )
                        onCreate(tour)
                    },
                    enabled = title.isNotBlank(),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("admin_save_tournament_btn")
                ) {
                    Text("Publish Tournament", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminSetRoomDialog(
    match: MatchFixture,
    onDismiss: () -> Unit,
    onSet: (roomId: String, password: String) -> Unit
) {
    var roomId by remember { mutableStateOf(match.roomId ?: "") }
    var password by remember { mutableStateOf(match.roomPassword ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, KheloGreen)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Assign Room Credentials", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                OutlinedTextField(
                    value = roomId,
                    onValueChange = { roomId = it },
                    label = { Text("Room ID") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("admin_room_id_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Room Password") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("admin_room_password_input")
                )

                Button(
                    onClick = { onSet(roomId, password) },
                    enabled = roomId.isNotBlank() && password.isNotBlank(),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("admin_save_room_btn")
                ) {
                    Text("Push Room to Participants", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
