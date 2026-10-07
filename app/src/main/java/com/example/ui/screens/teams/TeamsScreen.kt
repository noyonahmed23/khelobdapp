package com.example.ui.screens.teams

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

@Composable
fun TeamsScreen(
    teams: List<Team>,
    challenges: List<TeamChallenge>,
    currentUser: UserProfile,
    allUsers: List<UserProfile> = emptyList(),
    onCreateTeam: (name: String, tag: String, profileImageUrl: String, bannerUrl: String) -> Unit,
    onChallengeTeam: (targetTeamId: String, game: String, stake: Double) -> Unit,
    onAcceptChallenge: (challengeId: String) -> Unit,
    onSendJoinRequest: (teamId: String) -> Unit = {},
    onRespondJoinRequest: (teamId: String, userId: String, accept: Boolean) -> Unit = { _, _, _ -> },
    onRemoveMember: (teamId: String, userId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var showChallengeDialog by remember { mutableStateOf(false) }
    var teamMessage by remember { mutableStateOf<String?>(null) }

    val myTeam = teams.find { it.id == currentUser.teamId }
    val userById = allUsers.associateBy { it.uid }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("MY SQUAD", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 0.5.sp)
        }

        if (myTeam != null) {
            item {
                MyTeamCard(
                    team = myTeam,
                    currentUser = currentUser,
                    userById = userById,
                    onChallengeClick = { showChallengeDialog = true },
                    onRespondJoinRequest = { userId, accept -> onRespondJoinRequest(myTeam.id, userId, accept) },
                    onRemoveMember = { userId -> onRemoveMember(myTeam.id, userId) }
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = borderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("You don't have a team yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Create a squad, invite friends, and challenge rivals", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showCreateTeamDialog = true },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                            modifier = Modifier.testTag("create_team_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = DarkBg, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Team", color = DarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TEAM CHALLENGE ARENA", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 0.5.sp)
                if (myTeam != null) {
                    TextButton(onClick = { showChallengeDialog = true }, shape = RectangleShape) {
                        Text("+ Send Challenge", color = KheloGreenBright, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        if (challenges.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                ) {
                    Text(
                        text = "No open team challenges right now. Challenge a rival squad!",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(challenges) { challenge ->
                ChallengeCard(
                    challenge = challenge,
                    currentUser = currentUser,
                    onAccept = { onAcceptChallenge(challenge.id) }
                )
            }
        }

        item {
            Text("ALL ESPORTS SQUADS", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 0.5.sp)
        }

        if (teamMessage != null) {
            item {
                Surface(color = Color(0xFF132B38), shape = RectangleShape, modifier = Modifier.fillMaxWidth()) {
                    Text(teamMessage!!, color = EsportsCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                }
            }
        }

        items(teams) { team ->
            TeamListItem(
                team = team,
                currentUser = currentUser,
                onSendJoinRequest = {
                    onSendJoinRequest(team.id)
                    teamMessage = "Join request sent to ${team.name}."
                }
            )
        }
    }

    if (showCreateTeamDialog) {
        CreateTeamDialog(
            captainId = currentUser.uid,
            onDismiss = { showCreateTeamDialog = false },
            onCreate = { name, tag, profileImageUrl, bannerUrl ->
                onCreateTeam(name, tag, profileImageUrl, bannerUrl)
                showCreateTeamDialog = false
            }
        )
    }

    if (showChallengeDialog && myTeam != null) {
        SendChallengeDialog(
            myTeam = myTeam,
            availableTeams = teams.filter { it.id != myTeam.id },
            onDismiss = { showChallengeDialog = false },
            onSend = { targetTeamId, game, stake ->
                onChallengeTeam(targetTeamId, game, stake)
                showChallengeDialog = false
            }
        )
    }
}

@Composable
fun MyTeamCard(
    team: Team,
    currentUser: UserProfile,
    userById: Map<String, UserProfile>,
    onChallengeClick: () -> Unit,
    onRespondJoinRequest: (userId: String, accept: Boolean) -> Unit,
    onRemoveMember: (userId: String) -> Unit
) {
    val isCaptain = team.captainId == currentUser.uid

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = borderStroke(1.dp, KheloGreen)
    ) {
        Column {
            // Team banner + logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(KheloGreenContainer, RectangleShape)
            ) {
                if (team.bannerUrl.isNotBlank()) {
                    AsyncImage(
                        model = team.bannerUrl,
                        contentDescription = "Team Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Avatar(
                    imageUrl = team.profileImageUrl.ifBlank { team.logoUrl },
                    label = team.tag,
                    size = 52.dp,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp)
                        .offset(y = 22.dp)
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(team.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("[${team.tag}] • Captain: ${team.captainName}", color = TextSecondary, fontSize = 10.sp)
                    }
                    Button(
                        onClick = onChallengeClick,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Challenge", color = DarkBg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceCard, RectangleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("MATCHES", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${team.matches}", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Column {
                        Text("WINS", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${team.wins}", color = KheloGreenBright, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Column {
                        Text("WIN RATE", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${team.winRate.toInt()}%", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Column {
                        Text("POINTS", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${team.points}", color = EsportsCyan, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Join requests (captain only) ───────────────────────────────
                if (isCaptain && team.joinRequests.isNotEmpty()) {
                    Text("JOIN REQUESTS (${team.joinRequests.size})", color = EsportsOrange, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    team.joinRequests.forEach { reqUid ->
                        val requester = userById[reqUid]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(imageUrl = requester?.profilePictureUrl, label = requester?.username ?: "??", size = 28.dp, fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(requester?.username ?: "Player", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onRespondJoinRequest(reqUid, false) },
                                    shape = RectangleShape,
                                    border = borderStroke(1.dp, EsportsRed),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Reject", color = EsportsRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onRespondJoinRequest(reqUid, true) },
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Add", color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── Roster with avatars ────────────────────────────────────────
                Text("ROSTER MEMBERS (${team.members.size})", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                team.members.forEach { member ->
                    val memberUser = userById[member.userId]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(imageUrl = memberUser?.profilePictureUrl, label = member.username, size = 28.dp, fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(member.username, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            if (member.isCaptain) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(color = Color(0xFF382A05), shape = RectangleShape) {
                                    Text("CAPTAIN", color = EsportsGold, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(member.gameUid, color = TextMuted, fontSize = 10.sp)
                            if (isCaptain && !member.isCaptain) {
                                IconButton(onClick = { onRemoveMember(member.userId) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove", tint = EsportsRed, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeCard(
    challenge: TeamChallenge,
    currentUser: UserProfile,
    onAccept: () -> Unit
) {
    val isMyTeamChallenged = challenge.challengedTeamId == currentUser.teamId

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = borderStroke(1.dp, if (challenge.status == ChallengeStatus.PENDING) EsportsOrange else DarkBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = DarkSurfaceCard, shape = RectangleShape) {
                    Text(challenge.game.uppercase(), color = KheloGreenBright, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Surface(
                    color = if (challenge.status == ChallengeStatus.ACCEPTED) Color(0xFF1B381A) else Color(0xFF3B2A0F),
                    shape = RectangleShape
                ) {
                    Text(
                        challenge.status.name,
                        color = if (challenge.status == ChallengeStatus.ACCEPTED) KheloGreenBright else EsportsOrange,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(challenge.challengerTeamName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("VS", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text(challenge.challengedTeamName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stake: ৳${challenge.stakeAmount.toInt()}", color = TextSecondary, fontSize = 10.sp)
                if (isMyTeamChallenged && challenge.status == ChallengeStatus.PENDING) {
                    Button(
                        onClick = onAccept,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Accept Challenge", color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TeamListItem(
    team: Team,
    currentUser: UserProfile,
    onSendJoinRequest: () -> Unit
) {
    val alreadyMember = team.members.any { it.userId == currentUser.uid }
    val alreadyRequested = team.joinRequests.contains(currentUser.uid)
    val canRequest = currentUser.teamId == null && !alreadyMember && !alreadyRequested

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Avatar(imageUrl = team.profileImageUrl.ifBlank { team.logoUrl }, label = team.tag, size = 34.dp, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(team.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${team.members.size} Members • Captain: ${team.captainName}", color = TextSecondary, fontSize = 9.sp)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${team.points} PTS", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("${team.wins} Wins", color = KheloGreenBright, fontSize = 9.sp)
                Spacer(modifier = Modifier.height(4.dp))
                when {
                    alreadyMember -> {
                        Surface(color = KheloGreenContainer, shape = RectangleShape) {
                            Text("JOINED", color = KheloGreenBright, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    alreadyRequested -> {
                        Surface(color = Color(0xFF3B2A0F), shape = RectangleShape) {
                            Text("REQUESTED", color = EsportsOrange, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    canRequest -> {
                        Button(
                            onClick = onSendJoinRequest,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("Request Join", color = DarkBg, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateTeamDialog(
    captainId: String,
    onDismiss: () -> Unit,
    onCreate: (name: String, tag: String, profileImageUrl: String, bannerUrl: String) -> Unit
) {
    var teamName by remember { mutableStateOf("") }
    var teamTag by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var bannerUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val pickLogo = rememberImagePicker(
        storagePath = StorageManager.getTeamProfileImagePath(captainId),
        onUploaded = { profileImageUrl = it },
        onLoading = { isUploading = it }
    )
    val pickBanner = rememberImagePicker(
        storagePath = StorageManager.getTeamBannerPath(captainId),
        onUploaded = { bannerUrl = it },
        onLoading = { isUploading = it }
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, DarkBorder)
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Create Esports Squad", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                    // Banner preview + picker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .background(KheloGreenContainer, RectangleShape)
                    ) {
                        if (bannerUrl.isNotBlank()) {
                            AsyncImage(model = bannerUrl, contentDescription = "Team Banner", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Team Banner", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                        TextButton(onClick = pickBanner, modifier = Modifier.align(Alignment.TopEnd), shape = RectangleShape) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar(imageUrl = profileImageUrl, label = teamTag.ifBlank { "TG" }, size = 48.dp, fontSize = 14.sp)
                        OutlinedButton(
                            onClick = pickLogo,
                            shape = RectangleShape,
                            border = borderStroke(1.dp, DarkBorder),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Team Logo", color = KheloGreenBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Team Name") },
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = teamTag,
                        onValueChange = { teamTag = it.take(4).uppercase() },
                        label = { Text("Tag (3-4 Letters)") },
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onCreate(teamName, teamTag, profileImageUrl, bannerUrl) },
                        enabled = teamName.isNotBlank() && teamTag.isNotBlank() && !isUploading,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text("Confirm & Create", color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }

                LoadingOverlay(visible = isUploading, message = "Uploading...")
            }
        }
    }
}

@Composable
fun SendChallengeDialog(
    myTeam: Team,
    availableTeams: List<Team>,
    onDismiss: () -> Unit,
    onSend: (targetTeamId: String, game: String, stake: Double) -> Unit
) {
    var selectedTeamId by remember { mutableStateOf(availableTeams.firstOrNull()?.id ?: "") }
    var selectedGame by remember { mutableStateOf("Free Fire") }
    var stake by remember { mutableStateOf("100") }

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
                Text("Send Team Challenge", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                Text("Opponent Squad:", color = TextSecondary, fontSize = 11.sp)
                availableTeams.forEach { t ->
                    FilterChip(
                        selected = selectedTeamId == t.id,
                        onClick = { selectedTeamId = t.id },
                        shape = RectangleShape,
                        label = { Text("${t.name} (${t.tag})") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KheloGreen, selectedLabelColor = DarkBg),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = stake,
                    onValueChange = { stake = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Stake Amount (৳ BDT)") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val amt = stake.toDoubleOrNull() ?: 0.0
                        onSend(selectedTeamId, selectedGame, amt)
                    },
                    enabled = selectedTeamId.isNotBlank(),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text("Issue Challenge", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
