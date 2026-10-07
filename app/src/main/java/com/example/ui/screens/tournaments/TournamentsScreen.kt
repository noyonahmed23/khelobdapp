package com.example.ui.screens.tournaments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.screens.home.HomeTournamentCard
import com.example.ui.components.BanglaAlertDialog
import com.example.ui.components.banglaErrorMessage
import com.example.ui.components.MatchStatusBadge
import com.example.ui.components.TournamentStatusBadge
import com.example.ui.components.borderStroke
import com.example.ui.theme.*

@Composable
fun TournamentsScreen(
    tournaments: List<Tournament>,
    standings: List<GroupStanding>,
    matches: List<MatchFixture>,
    registrations: List<TournamentRegistration>,
    currentUser: UserProfile,
    onRegister: (Tournament, String, String, String) -> Result<String>,
    onOpenDeposit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedGame by remember { mutableStateOf("All") }
    var selectedTournament by remember { mutableStateOf<Tournament?>(null) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var alertDeposit by remember { mutableStateOf(false) }

    val games = listOf("All", "Free Fire", "PUBG Mobile", "eFootball", "COD Mobile")

    val filteredTournaments = tournaments.filter {
        selectedGame == "All" || it.game.equals(selectedGame, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Game Filter Chips (Sharp Rectangle)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(games) { game ->
                FilterChip(
                    selected = selectedGame == game,
                    onClick = { selectedGame = game },
                    shape = RectangleShape,
                    label = {
                        Text(
                            text = game,
                            fontSize = 11.sp,
                            fontWeight = if (selectedGame == game) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KheloGreen,
                        selectedLabelColor = DarkBg,
                        containerColor = DarkSurfaceCard,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedGame == game,
                        borderColor = if (selectedGame == game) KheloGreen else DarkBorder
                    ),
                    modifier = Modifier.testTag("filter_chip_$game")
                )
            }
        }

        // Tournaments List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredTournaments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No tournaments found for $selectedGame", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredTournaments) { tour ->
                    HomeTournamentCard(
                        tournament = tour,
                        onClick = { selectedTournament = tour }
                    )
                }
            }
        }
    }

    // Detailed Tournament Dialog
    selectedTournament?.let { tour ->
        TournamentDetailsDialog(
            tournament = tour,
            standings = standings.filter { it.tournamentId == tour.id },
            matches = matches.filter { it.tournamentId == tour.id },
            registrations = registrations.filter { it.tournamentId == tour.id },
            isAlreadyRegistered = registrations.any { it.tournamentId == tour.id && it.userId == currentUser.uid },
            onDismiss = { selectedTournament = null },
            onJoinClick = { showRegisterDialog = true }
        )
    }

    // Register Sheet Dialog
    if (showRegisterDialog && selectedTournament != null) {
        TournamentRegisterDialog(
            tournament = selectedTournament!!,
            currentUser = currentUser,
            onDismiss = { showRegisterDialog = false },
            onConfirmRegistration = { gameUid, method, trxId ->
                val result = onRegister(selectedTournament!!, gameUid, method, trxId)
                if (result.isSuccess) {
                    showRegisterDialog = false
                    selectedTournament = null
                }
                result
            }
        )
    }
    if (alertMessage != null) {
        BanglaAlertDialog(
            title = if (alertDeposit) "ব্যালেন্স প্রয়োজন" else "রেজিস্ট্রেশন বার্তা",
            message = alertMessage!!,
            onConfirm = {
                val go = alertDeposit
                alertMessage = null
                alertDeposit = false
                if (go) onOpenDeposit()
            }
        )
    }
}

@Composable
fun TournamentDetailsDialog(
    tournament: Tournament,
    standings: List<GroupStanding>,
    matches: List<MatchFixture>,
    registrations: List<TournamentRegistration>,
    isAlreadyRegistered: Boolean,
    onDismiss: () -> Unit,
    onJoinClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Standings", "Knockouts", "Roster")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tournament.game.uppercase(),
                            color = KheloGreenBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = tournament.title,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurfaceCard,
                    contentColor = KheloGreenBright,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) KheloGreenBright else TextSecondary
                                )
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
                        0 -> TournamentOverviewTab(tournament = tournament)
                        1 -> TournamentStandingsTab(standings = standings)
                        2 -> TournamentKnockoutsTab(matches = matches.filter { it.isKnockout || it.groupName == null })
                        3 -> TournamentRosterTab(registrations = registrations)
                    }
                }

                Surface(
                    color = DarkSurfaceCard,
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ENTRY FEE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (tournament.entryFee == 0.0) "FREE" else "৳${tournament.entryFee.toInt()}",
                                color = if (tournament.entryFee == 0.0) KheloGreenBright else TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }

                        if (isAlreadyRegistered) {
                            Button(
                                onClick = {},
                                enabled = false,
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFF1E3823))
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Registered", color = KheloGreenBright, fontWeight = FontWeight.Bold)
                            }
                        } else if (tournament.status == TournamentStatus.REGISTRATION) {
                            Button(
                                onClick = onJoinClick,
                                colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                                shape = RectangleShape,
                                modifier = Modifier.testTag("join_tournament_confirm_btn")
                            ) {
                                Text("Join Tournament", color = DarkBg, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = RectangleShape,
                                color = DarkSurfaceElevated
                            ) {
                                Text(
                                    text = "Registration Closed",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentOverviewTab(tournament: Tournament) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RectangleShape
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status", color = TextMuted, fontSize = 11.sp)
                        TournamentStatusBadge(status = tournament.status)
                    }
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Starts", color = TextMuted, fontSize = 11.sp)
                        Text("${tournament.startDate} • ${tournament.startTime}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Format", color = TextMuted, fontSize = 11.sp)
                        Text(tournament.format, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Text("PRIZE DISTRIBUTION", color = EsportsGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PrizeCard(position = "1st Place 🏆", amount = "৳${tournament.firstPrize.toInt()}", color = EsportsGold, modifier = Modifier.weight(1f))
                PrizeCard(position = "2nd Place 🥈", amount = "৳${tournament.secondPrize.toInt()}", color = Color(0xFFC0C0C0), modifier = Modifier.weight(1f))
                if (tournament.thirdPrize > 0) {
                    PrizeCard(position = "3rd Place 🥉", amount = "৳${tournament.thirdPrize.toInt()}", color = Color(0xFFCD7F32), modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Text("OFFICIAL RULES", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RectangleShape
            ) {
                Text(
                    text = tournament.rules,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
fun PrizeCard(position: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RectangleShape,
        color = DarkSurfaceCard,
        border = borderStroke(1.dp, color)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(position, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(amount, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun TournamentStandingsTab(standings: List<GroupStanding>) {
    if (standings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Standings will appear once fixtures are generated.", color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    } else {
        val groups = standings.map { it.groupName }.distinct()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(groups) { groupName ->
                val groupMembers = standings.filter { it.groupName == groupName }
                    .sortedWith(compareByDescending<GroupStanding> { it.points }.thenByDescending { it.pointDiff })

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RectangleShape
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(groupName, color = KheloGreenBright, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Text("Top advance to Knockouts", color = TextMuted, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceElevated, RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                            Text("PLAYER", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("P", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                            Text("W", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                            Text("D", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                            Text("L", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                            Text("PTS", color = EsportsGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                        }

                        groupMembers.forEachIndexed { index, st ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${index + 1}", color = if (st.isQualified) KheloGreenBright else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Text(st.participantName, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (st.isQualified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(color = KheloGreenContainer, shape = RectangleShape) {
                                            Text("Q", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 3.dp))
                                        }
                                    }
                                }
                                Text("${st.played}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                                Text("${st.won}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                                Text("${st.drawn}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                                Text("${st.lost}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                                Text("${st.points}", color = EsportsGold, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                            }
                            if (index < groupMembers.size - 1) {
                                HorizontalDivider(color = DarkBorder)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentKnockoutsTab(matches: List<MatchFixture>) {
    if (matches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Knockout bracket will be drawn after group stage.", color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matches) { match ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = borderStroke(1.dp, if (match.round.contains("Final")) EsportsGold else DarkBorder),
                    shape = RectangleShape
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(match.round.uppercase(), color = if (match.round.contains("Final")) EsportsGold else EsportsCyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            MatchStatusBadge(status = match.status)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                match.participantAName,
                                color = if (match.winnerId == match.participantAId) KheloGreenBright else TextPrimary,
                                fontWeight = if (match.winnerId == match.participantAId) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (match.scoreA != null) "${match.scoreA} : ${match.scoreB}" else "VS",
                                color = if (match.scoreA != null) EsportsGold else TextMuted,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            Text(
                                match.participantBName,
                                color = if (match.winnerId == match.participantBId) KheloGreenBright else TextPrimary,
                                fontWeight = if (match.winnerId == match.participantBId) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentRosterTab(registrations: List<TournamentRegistration>) {
    if (registrations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No participants registered yet.", color = TextMuted, fontSize = 12.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(registrations) { reg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RectangleShape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(reg.userName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("UID: ${reg.gameUid}", color = TextSecondary, fontSize = 11.sp)
                        }
                        if (reg.teamName != null) {
                            Surface(color = DarkSurfaceElevated, shape = RectangleShape) {
                                Text(reg.teamName, color = KheloGreenBright, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentRegisterDialog(
    tournament: Tournament,
    currentUser: UserProfile,
    onDismiss: () -> Unit,
    onConfirmRegistration: (gameUid: String, method: String, trxId: String) -> Result<String>
) {
    var gameUid by remember { mutableStateOf(currentUser.inGameUid) }
    var selectedMethod by remember { mutableStateOf(if (currentUser.walletBalance >= tournament.entryFee) "Wallet" else "bKash") }
    var trxId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tournament Registration", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = tournament.title,
                    color = KheloGreenBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = gameUid,
                    onValueChange = { gameUid = it },
                    label = { Text("In-Game UID / Player ID") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KheloGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_game_uid_input")
                )

                if (tournament.entryFee > 0) {
                    Text("Payment Method (Fee: ৳${tournament.entryFee.toInt()})", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Wallet", "bKash", "Nagad").forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                shape = RectangleShape,
                                label = {
                                    Text(
                                        text = if (method == "Wallet") "Wallet (৳${currentUser.walletBalance.toInt()})" else method,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KheloGreen,
                                    selectedLabelColor = DarkBg,
                                    containerColor = DarkSurfaceCard,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (selectedMethod != "Wallet") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                            shape = RectangleShape
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Send ৳${tournament.entryFee.toInt()} to official $selectedMethod number:",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (selectedMethod == "bKash") "01700123456 (Personal)" else "01900654321 (Personal)",
                                    color = EsportsGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        OutlinedTextField(
                            value = trxId,
                            onValueChange = { trxId = it },
                            label = { Text("Transaction ID (TrxID)") },
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KheloGreen,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_trx_id_input")
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = EsportsRed, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        isSubmitting = true
                        val res = onConfirmRegistration(gameUid, selectedMethod, trxId)
                        isSubmitting = false
                        if (res.isFailure) {
                            errorMessage = banglaErrorMessage(res.exceptionOrNull()?.message ?: "রেজিস্ট্রেশন ব্যর্থ হয়েছে।")
                        }
                    },
                    enabled = !isSubmitting && gameUid.isNotBlank(),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("submit_registration_btn")
                ) {
                    Text("Confirm Registration", color = DarkBg, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
