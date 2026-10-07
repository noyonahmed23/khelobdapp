package com.example.ui.screens.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.firebase.StorageManager
import com.example.data.model.*
import com.example.ui.components.Avatar
import com.example.ui.components.LoadingOverlay
import com.example.ui.components.borderStroke
import com.example.ui.components.rememberImagePicker
import com.example.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UserChallengeScreen(
    currentUser: UserProfile,
    allUsers: List<UserProfile>,
    userChallenges: List<UserChallenge>,
    onSendChallenge: (opponentUid: String, opponentName: String) -> Unit,
    onAcceptChallenge: (challengeId: String) -> Unit,
    onRejectChallenge: (challengeId: String) -> Unit,
    onSetRoomCredentials: (challengeId: String, roomId: String, password: String) -> Unit,
    onSubmitProof: (challengeId: String, proofUrl: String) -> Unit,
    onViewProfile: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Challenges where the current user is involved
    val myChallenges = userChallenges.filter { challenge ->
        challenge.challengerUid == currentUser.uid || challenge.opponentUid == currentUser.uid
    }

    // Users that can be challenged: exclude self and own team members
    val challengeableUsers = allUsers.filter { user ->
        user.uid != currentUser.uid &&
                !(currentUser.teamId != null && user.teamId == currentUser.teamId)
    }

    // Dialog state
    var showSelectOpponentDialog by remember { mutableStateOf(false) }
    var pendingOpponent by remember { mutableStateOf<UserProfile?>(null) }
    var showRulesDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Wallet / eligibility banner ────────────────────────────────────
        item {
            val insufficient = currentUser.walletBalance < 50.0
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                color = if (insufficient) Color(0xFF3B1414) else Color(0xFF132B1A),
                border = borderStroke(1.dp, if (insufficient) EsportsRed else KheloGreen)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (insufficient) Icons.Default.Warning else Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (insufficient) EsportsRed else EsportsGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Wallet: ৳${currentUser.walletBalance.toInt()}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (insufficient) "Insufficient balance — you need at least ৳50 to send or accept a challenge. Add money from your Profile."
                                   else "You are eligible to send and accept 1v1 challenges (৳50 stake).",
                            color = if (insufficient) EsportsRed else TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // ── Section Header: My Challenges ──────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MY 1v1 CHALLENGES",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
                TextButton(
                    onClick = { showSelectOpponentDialog = true },
                    shape = RectangleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = KheloGreenBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "New Challenge",
                        color = KheloGreenBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // ── Challenge Cards ────────────────────────────────────────────────
        if (myChallenges.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = borderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No active 1v1 challenges",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Challenge a player and compete for the prize pool!",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showSelectOpponentDialog = true },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                            modifier = Modifier.testTag("challenge_player_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = DarkBg,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Challenge a Player", color = DarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(myChallenges) { challenge ->
                UserChallengeCard(
                    challenge = challenge,
                    currentUser = currentUser,
                    onAccept = { onAcceptChallenge(challenge.id) },
                    onReject = { onRejectChallenge(challenge.id) },
                    onSetRoomCredentials = { roomId, password ->
                        onSetRoomCredentials(challenge.id, roomId, password)
                    },
                    onSubmitProof = { proofUrl -> onSubmitProof(challenge.id, proofUrl) }
                )
            }
        }

        // ── Section Header: Challenge a Player ────────────────────────────
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "CHALLENGE A PLAYER",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        }

        // ── Player List ────────────────────────────────────────────────────
        if (challengeableUsers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                ) {
                    Text(
                        text = "No other players available to challenge right now.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(challengeableUsers) { user ->
                PlayerListItem(
                    user = user,
                    onViewProfile = { onViewProfile(user) },
                    onChallenge = {
                        pendingOpponent = user
                        showRulesDialog = true
                    }
                )
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

    if (showSelectOpponentDialog) {
        SelectOpponentDialog(
            users = challengeableUsers,
            onDismiss = { showSelectOpponentDialog = false },
            onSelectOpponent = { user ->
                showSelectOpponentDialog = false
                pendingOpponent = user
                showRulesDialog = true
            }
        )
    }

    if (showRulesDialog && pendingOpponent != null) {
        ChallengeRulesDialog(
            opponentName = pendingOpponent!!.username,
            currentBalance = currentUser.walletBalance,
            requiredStake = 50.0,
            onDismiss = {
                showRulesDialog = false
                pendingOpponent = null
            },
            onAgree = {
                val opp = pendingOpponent!!
                showRulesDialog = false
                pendingOpponent = null
                onSendChallenge(opp.uid, opp.username)
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Challenge Rules Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChallengeRulesDialog(
    opponentName: String,
    currentBalance: Double,
    requiredStake: Double,
    onDismiss: () -> Unit,
    onAgree: () -> Unit
) {
    val insufficient = currentBalance < requiredStake
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
                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = KheloGreenBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHALLENGE RULES & REGULATIONS",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Challenging: $opponentName",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                // Rules Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    color = KheloGreenContainer,
                    border = borderStroke(1.dp, KheloGreen)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RuleRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = EsportsGold,
                            text = "Entry: ৳50 per player (৳100 total pool)"
                        )
                        RuleRow(
                            icon = Icons.Default.EmojiEvents,
                            iconTint = EsportsGold,
                            text = "Winner receives: ৳85 (৳15 platform fee)"
                        )
                        RuleRow(
                            icon = Icons.Default.Block,
                            iconTint = EsportsRed,
                            text = "No emulators or hacks allowed"
                        )
                        RuleRow(
                            icon = Icons.Default.CameraAlt,
                            iconTint = EsportsCyan,
                            text = "Screenshot proof required from both players"
                        )
                        RuleRow(
                            icon = Icons.Default.AdminPanelSettings,
                            iconTint = KheloGreenBright,
                            text = "Admin makes the final decision"
                        )
                        RuleRow(
                            icon = Icons.Default.DoNotDisturb,
                            iconTint = EsportsOrange,
                            text = "No refunds after match starts"
                        )
                    }
                }

                // Balance warning
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    color = if (insufficient) Color(0xFF3B1414) else Color(0xFF3B2A0F)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (insufficient) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (insufficient) EsportsRed else EsportsOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (insufficient)
                                "Insufficient balance! Your wallet has ৳${currentBalance.toInt()} but ৳${requiredStake.toInt()} is required. Please add money first."
                            else
                                "Your balance: ৳${currentBalance.toInt()}. ৳${requiredStake.toInt()} will be deducted from both wallets once the opponent accepts.",
                            color = if (insufficient) EsportsRed else EsportsOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RectangleShape,
                        border = borderStroke(1.dp, EsportsRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EsportsRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onAgree,
                        enabled = !insufficient,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Agree & Send",
                            color = DarkBg,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/** Single rule row used inside [ChallengeRulesDialog]. */
@Composable
private fun RuleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = OnKheloGreenContainer, fontSize = 11.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// User Challenge Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UserChallengeCard(
    challenge: UserChallenge,
    currentUser: UserProfile,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onSetRoomCredentials: (roomId: String, password: String) -> Unit,
    onSubmitProof: (proofUrl: String) -> Unit
) {
    val isChallenger = challenge.challengerUid == currentUser.uid
    val isOpponent   = challenge.opponentUid == currentUser.uid

    // Proof dialog state
    var showProofDialog by remember { mutableStateOf(false) }
    // Room credential form state (visible when ACCEPTED + isChallenger)
    var roomIdInput by remember { mutableStateOf("") }
    var roomPasswordInput by remember { mutableStateOf("") }

    val borderColor = when (challenge.status) {
        UserChallengeStatus.PENDING          -> EsportsOrange
        UserChallengeStatus.ACCEPTED         -> KheloGreenBright
        UserChallengeStatus.ROOM_SET         -> EsportsCyan
        UserChallengeStatus.PROOF_SUBMITTED,
        UserChallengeStatus.UNDER_REVIEW     -> EsportsGold
        UserChallengeStatus.COMPLETED        -> KheloGreenBright
        UserChallengeStatus.REJECTED,
        UserChallengeStatus.CANCELLED        -> EsportsRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("challenge_card_${challenge.id}"),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = borderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Header Row ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = DarkSurfaceCard, shape = RectangleShape) {
                    Text(
                        text = "FREE FIRE • 1v1",
                        color = KheloGreenBright,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                UserChallengeStatusBadge(status = challenge.status)
            }

            // ── Matchup ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = challenge.challengerName,
                        color = if (isChallenger) KheloGreenBright else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(text = "Challenger", color = TextMuted, fontSize = 9.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "VS", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text(
                        text = "৳${challenge.stakeAmount.toInt()} each",
                        color = TextMuted,
                        fontSize = 8.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = challenge.opponentName,
                        color = if (isOpponent) KheloGreenBright else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(text = "Opponent", color = TextMuted, fontSize = 9.sp)
                }
            }

            HorizontalDivider(color = DarkBorder, thickness = 1.dp)

            // ── Status-specific Actions ──────────────────────────────────
            when (challenge.status) {

                // Opponent can Accept / Reject
                UserChallengeStatus.PENDING -> {
                    if (isOpponent) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onReject,
                                shape = RectangleShape,
                                border = borderStroke(1.dp, EsportsRed),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EsportsRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text("Reject", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = onAccept,
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text("Accept", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    } else {
                        // Challenger is waiting
                        InfoRow(
                            icon = Icons.Default.HourglassEmpty,
                            tint = EsportsOrange,
                            text = "Waiting for ${challenge.opponentName} to respond..."
                        )
                    }
                }

                // Challenger sets room credentials; opponent waits
                UserChallengeStatus.ACCEPTED -> {
                    if (isChallenger) {
                        // Deduction confirmation note
                        InfoRow(
                            icon = Icons.Default.CheckCircle,
                            tint = KheloGreenBright,
                            text = "Challenge accepted! ৳50 deducted from both players."
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedTextField(
                            value = roomIdInput,
                            onValueChange = { roomIdInput = it },
                            label = { Text("Room ID") },
                            shape = RectangleShape,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KheloGreen,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = KheloGreen,
                                unfocusedLabelColor = TextMuted
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = roomPasswordInput,
                            onValueChange = { roomPasswordInput = it },
                            label = { Text("Room Password") },
                            shape = RectangleShape,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KheloGreen,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = KheloGreen,
                                unfocusedLabelColor = TextMuted
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onSetRoomCredentials(roomIdInput.trim(), roomPasswordInput.trim())
                            },
                            enabled = roomIdInput.isNotBlank() && roomPasswordInput.isNotBlank(),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            Text("Set Room Credentials", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    } else {
                        InfoRow(
                            icon = Icons.Default.HourglassEmpty,
                            tint = EsportsCyan,
                            text = "Waiting for ${challenge.challengerName} to set room credentials..."
                        )
                    }
                }

                // Room is set — both players can see credentials & submit proof
                UserChallengeStatus.ROOM_SET -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        color = Color(0xFF0E2330),
                        border = borderStroke(1.dp, EsportsCyan)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = EsportsCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ROOM CREDENTIALS",
                                    color = EsportsCyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            CredentialRow(label = "Room ID", value = challenge.roomId ?: "—")
                            CredentialRow(label = "Password", value = challenge.roomPassword ?: "—")
                        }
                    }

                    // Proof submission status
                    val hasSubmittedProof = if (isChallenger) {
                        challenge.challengerProofUrl != null
                    } else {
                        challenge.opponentProofUrl != null
                    }

                    if (hasSubmittedProof) {
                        InfoRow(
                            icon = Icons.Default.CheckCircle,
                            tint = KheloGreenBright,
                            text = "Your proof has been submitted. Awaiting admin review."
                        )
                    } else {
                        Button(
                            onClick = { showProofDialog = true },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = EsportsCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = DarkBg,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit Screenshot Proof", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }

                // Awaiting admin
                UserChallengeStatus.PROOF_SUBMITTED,
                UserChallengeStatus.UNDER_REVIEW -> {
                    InfoRow(
                        icon = Icons.Default.AdminPanelSettings,
                        tint = EsportsGold,
                        text = "Awaiting admin review. Result will be declared soon."
                    )
                }

                // Match concluded
                UserChallengeStatus.COMPLETED -> {
                    if (challenge.winnerUid != null) {
                        val didWin = challenge.winnerUid == currentUser.uid
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RectangleShape,
                            color = if (didWin) KheloGreenContainer else Color(0xFF2B1010),
                            border = borderStroke(1.dp, if (didWin) KheloGreen else EsportsRed)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (didWin) Icons.Default.EmojiEvents else Icons.Default.SentimentDissatisfied,
                                    contentDescription = null,
                                    tint = if (didWin) EsportsGold else EsportsRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (didWin) "YOU WON! 🎉" else "You Lost",
                                        color = if (didWin) EsportsGold else EsportsRed,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Winner: ${challenge.winnerName}",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                    if (didWin) {
                                        Text(
                                            text = "+৳85 credited to your wallet",
                                            color = KheloGreenBright,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Rejected / Cancelled
                UserChallengeStatus.REJECTED -> {
                    InfoRow(
                        icon = Icons.Default.Cancel,
                        tint = EsportsRed,
                        text = "Challenge was rejected by ${challenge.opponentName}."
                    )
                }

                UserChallengeStatus.CANCELLED -> {
                    InfoRow(
                        icon = Icons.Default.Cancel,
                        tint = EsportsRed,
                        text = "This challenge was cancelled."
                    )
                }
            }
        }
    }

    // Proof submission dialog
    if (showProofDialog) {
        SubmitProofDialog(
            storagePath = StorageManager.getChallengePicPath(challenge.id, currentUser.uid),
            onDismiss = { showProofDialog = false },
            onSubmit = { url ->
                showProofDialog = false
                onSubmitProof(url)
            }
        )
    }
}

/** Compact info row with an icon and a label. */
@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = TextSecondary, fontSize = 11.sp)
    }
}

/** Label / value pair for room credentials. */
@Composable
private fun CredentialRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 11.sp)
        Surface(
            shape = RectangleShape,
            color = DarkSurfaceCard,
            border = borderStroke(1.dp, EsportsCyan)
        ) {
            Text(
                text = value,
                color = EsportsCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status Badge for UserChallenge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun UserChallengeStatusBadge(status: UserChallengeStatus) {
    val (bgColor, textColor, label) = when (status) {
        UserChallengeStatus.PENDING          -> Triple(Color(0xFF3B2A0F), EsportsOrange,    "PENDING")
        UserChallengeStatus.ACCEPTED         -> Triple(Color(0xFF1B381A), KheloGreenBright, "ACCEPTED")
        UserChallengeStatus.ROOM_SET         -> Triple(Color(0xFF0E2330), EsportsCyan,      "ROOM SET")
        UserChallengeStatus.PROOF_SUBMITTED  -> Triple(Color(0xFF382A05), EsportsGold,      "PROOF SUBMITTED")
        UserChallengeStatus.UNDER_REVIEW     -> Triple(Color(0xFF382A05), EsportsGold,      "UNDER REVIEW")
        UserChallengeStatus.COMPLETED        -> Triple(Color(0xFF1B381A), KheloGreenBright, "COMPLETED")
        UserChallengeStatus.REJECTED         -> Triple(Color(0xFF381414), EsportsRed,       "REJECTED")
        UserChallengeStatus.CANCELLED        -> Triple(Color(0xFF381414), EsportsRed,       "CANCELLED")
    }

    Surface(color = bgColor, shape = RectangleShape) {
        Text(
            text = label,
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            letterSpacing = 0.5.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Submit Proof Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SubmitProofDialog(
    storagePath: String,
    onDismiss: () -> Unit,
    onSubmit: (proofUrl: String) -> Unit
) {
    var uploadedUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val pickProof = rememberImagePicker(
        storagePath = storagePath,
        onUploaded = { uploadedUrl = it; errorMsg = null },
        onError = { errorMsg = it },
        onLoading = { isUploading = it }
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, EsportsCyan)
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = EsportsCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UPLOAD END-MATCH SCREENSHOT",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = "Upload the final scoreboard / victory screen as proof. Admin reviews both screenshots to declare the winner.",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )

                    // Preview / upload area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(DarkSurfaceCard, RectangleShape)
                            .border(1.dp, if (uploadedUrl.isNotBlank()) EsportsCyan else DarkBorder, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uploadedUrl.isNotBlank()) {
                            AsyncImage(
                                model = uploadedUrl,
                                contentDescription = "Proof preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ImageSearch, contentDescription = null, tint = TextMuted, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("No screenshot selected", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = pickProof,
                        shape = RectangleShape,
                        border = borderStroke(1.dp, EsportsCyan),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = EsportsCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (uploadedUrl.isNotBlank()) "Replace Screenshot" else "Choose & Upload Screenshot", color = EsportsCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (errorMsg != null) {
                        Text(errorMsg!!, color = EsportsRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RectangleShape,
                            border = borderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onSubmit(uploadedUrl) },
                            enabled = uploadedUrl.isNotBlank() && !isUploading,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = EsportsCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Submit Proof", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }

                LoadingOverlay(visible = isUploading, message = "Uploading screenshot...")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Select Opponent Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SelectOpponentDialog(
    users: List<UserProfile>,
    onDismiss: () -> Unit,
    onSelectOpponent: (UserProfile) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Dialog header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = KheloGreenBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SELECT OPPONENT",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 1.dp)

                if (users.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No players available to challenge.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        items(users) { user ->
                            OpponentListItem(
                                user = user,
                                onClick = { onSelectOpponent(user) }
                            )
                            HorizontalDivider(color = DarkBorder, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

/** Row inside [SelectOpponentDialog]. */
@Composable
private fun OpponentListItem(
    user: UserProfile,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar initials
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(KheloGreenContainer, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(2).uppercase(),
                    color = KheloGreenBright,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = user.username,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "UID: ${user.inGameUid.ifBlank { "N/A" }}",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Button(
            onClick = onClick,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
        ) {
            Text("Challenge", color = DarkBg, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Player List Item (inline in the main screen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayerListItem(
    user: UserProfile,
    onViewProfile: () -> Unit,
    onChallenge: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = borderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onViewProfile)
            ) {
                Avatar(imageUrl = user.profilePictureUrl, label = user.username, size = 34.dp, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = user.username,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "UID: ${user.inGameUid.ifBlank { "N/A" }}",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                        if (user.teamName != null) {
                            Surface(shape = RectangleShape, color = DarkSurfaceElevated) {
                                Text(
                                    text = user.teamName,
                                    color = EsportsCyan,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${user.wins}W / ${user.losses}L",
                    color = TextSecondary,
                    fontSize = 9.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Button(
                    onClick = onChallenge,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("Challenge", color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
