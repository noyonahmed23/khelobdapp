package com.example.ui.screens.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.MatchStatusBadge
import com.example.ui.components.borderStroke
import com.example.ui.theme.*

@Composable
fun MatchesScreen(
    matches: List<MatchFixture>,
    currentUser: UserProfile,
    onSubmitScore: (matchId: String, scoreA: Int, scoreB: Int, winnerId: String, proofNote: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Live", "Upcoming", "Completed")

    var selectedRoomMatch by remember { mutableStateOf<MatchFixture?>(null) }
    var selectedScoreMatch by remember { mutableStateOf<MatchFixture?>(null) }

    val filteredMatches = matches.filter { match ->
        when (selectedTab) {
            0 -> match.status == MatchStatus.LIVE || match.status == MatchStatus.ROOM_READY
            1 -> match.status == MatchStatus.SCHEDULED || match.status == MatchStatus.WAITING
            2 -> match.status == MatchStatus.VERIFIED || match.status == MatchStatus.COMPLETED || match.status == MatchStatus.UNDER_REVIEW
            else -> true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurfaceCard,
            contentColor = KheloGreenBright,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                val count = matches.count {
                    when (index) {
                        0 -> it.status == MatchStatus.LIVE || it.status == MatchStatus.ROOM_READY
                        1 -> it.status == MatchStatus.SCHEDULED || it.status == MatchStatus.WAITING
                        2 -> it.status == MatchStatus.VERIFIED || it.status == MatchStatus.COMPLETED || it.status == MatchStatus.UNDER_REVIEW
                        else -> false
                    }
                }
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = "$title ($count)",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) KheloGreenBright else TextSecondary
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredMatches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No ${tabs[selectedTab].lowercase()} matches right now.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(filteredMatches) { match ->
                    MatchDetailedCard(
                        match = match,
                        currentUser = currentUser,
                        onViewRoom = { selectedRoomMatch = match },
                        onSubmitScore = { selectedScoreMatch = match }
                    )
                }
            }
        }
    }

    selectedRoomMatch?.let { match ->
        PrivateRoomDialog(
            match = match,
            onDismiss = { selectedRoomMatch = null }
        )
    }

    selectedScoreMatch?.let { match ->
        SubmitScoreDialog(
            match = match,
            currentUser = currentUser,
            onDismiss = { selectedScoreMatch = null },
            onSubmit = { sA, sB, winId, proof ->
                onSubmitScore(match.id, sA, sB, winId, proof)
                selectedScoreMatch = null
            }
        )
    }
}

@Composable
fun MatchDetailedCard(
    match: MatchFixture,
    currentUser: UserProfile,
    onViewRoom: () -> Unit,
    onSubmitScore: () -> Unit
) {
    val isParticipant = match.participantAId == currentUser.uid || match.participantBId == currentUser.uid ||
            currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.SUPER_ADMIN

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_card_${match.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RectangleShape,
        border = borderStroke(
            1.dp,
            if (match.status == MatchStatus.ROOM_READY) KheloGreenBright
            else if (match.status == MatchStatus.LIVE) EsportsRed
            else DarkBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = DarkSurfaceCard,
                        shape = RectangleShape
                    ) {
                        Text(
                            text = match.game.uppercase(),
                            color = KheloGreenBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${match.tournamentTitle} • ${match.groupName ?: match.round}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                }
                MatchStatusBadge(status = match.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.participantAName,
                        color = if (match.winnerId == match.participantAId) KheloGreenBright else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (match.participantAId == currentUser.uid) {
                        Text("You", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RectangleShape,
                    color = DarkSurfaceCard,
                    border = borderStroke(0.5.dp, DarkBorder),
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = if (match.scoreA != null && match.scoreB != null) "${match.scoreA} : ${match.scoreB}" else "VS",
                        color = if (match.scoreA != null) EsportsGold else TextMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = match.participantBName,
                        color = if (match.winnerId == match.participantBId) KheloGreenBright else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (match.participantBId == currentUser.uid) {
                        Text("You", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextMuted, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(match.scheduledTime, color = TextMuted, fontSize = 11.sp)
                }

                if (match.winnerName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = EsportsGold, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Winner: ${match.winnerName}", color = EsportsGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isParticipant) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (match.status == MatchStatus.ROOM_READY || match.status == MatchStatus.LIVE) {
                        Button(
                            onClick = onViewRoom,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreenContainer),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("view_room_btn_${match.id}")
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Room Credentials", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (match.status == MatchStatus.LIVE || match.status == MatchStatus.ROOM_READY || match.status == MatchStatus.RESULT_PENDING) {
                        OutlinedButton(
                            onClick = onSubmitScore,
                            shape = RectangleShape,
                            border = borderStroke(1.dp, EsportsGold),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("submit_score_btn_${match.id}")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = EsportsGold, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Submit Result", color = EsportsGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateRoomDialog(
    match: MatchFixture,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, KheloGreen)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Private Room Credentials", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "${match.participantAName} vs ${match.participantBName}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RectangleShape
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("CUSTOM ROOM ID", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = match.roomId ?: "Pending Generation",
                            color = KheloGreenBright,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("ROOM PASSWORD", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = match.roomPassword ?: "Pending Generation",
                            color = EsportsGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        val text = "Room ID: ${match.roomId}\nPassword: ${match.roomPassword}"
                        clipboard.setText(AnnotatedString(text))
                        copied = true
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = null, tint = DarkBg, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (copied) "Copied!" else "Copy Room Credentials", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SubmitScoreDialog(
    match: MatchFixture,
    currentUser: UserProfile,
    onDismiss: () -> Unit,
    onSubmit: (scoreA: Int, scoreB: Int, winnerId: String, proofNote: String) -> Unit
) {
    var scoreA by remember { mutableStateOf("0") }
    var scoreB by remember { mutableStateOf("0") }
    var selectedWinnerId by remember { mutableStateOf(match.participantAId) }
    var proofNote by remember { mutableStateOf("") }

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
                    Text("Submit Match Result", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = scoreA,
                        onValueChange = { scoreA = it.filter { ch -> ch.isDigit() } },
                        label = { Text(match.participantAName.take(8)) },
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = scoreB,
                        onValueChange = { scoreB = it.filter { ch -> ch.isDigit() } },
                        label = { Text(match.participantBName.take(8)) },
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Select Winner:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedWinnerId == match.participantAId,
                        onClick = { selectedWinnerId = match.participantAId },
                        shape = RectangleShape,
                        label = { Text(match.participantAName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KheloGreen, selectedLabelColor = DarkBg),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedWinnerId == match.participantBId,
                        onClick = { selectedWinnerId = match.participantBId },
                        shape = RectangleShape,
                        label = { Text(match.participantBName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KheloGreen, selectedLabelColor = DarkBg),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = proofNote,
                    onValueChange = { proofNote = it },
                    label = { Text("Match proof notes / link") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val sA = scoreA.toIntOrNull() ?: 0
                        val sB = scoreB.toIntOrNull() ?: 0
                        onSubmit(sA, sB, selectedWinnerId, proofNote)
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("submit_result_confirm_btn")
                ) {
                    Text("Submit for Verification", color = DarkBg, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
