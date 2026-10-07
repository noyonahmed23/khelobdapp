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
    onSetChallengeRoom: (challengeId: String, roomId: String, password: String) -> Unit = { _, _, _ -> },
    onCancelChallenge: (challengeId: String) -> Unit = {},
    onSendJoinRequest: (teamId: String) -> Unit = {},
    onRespondJoinRequest: (teamId: String, userId: String, accept: Boolean) -> Unit = { _, _, _ -> },
    onRemoveMember: (teamId: String, userId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(0) }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var showChallengeDialog by remember { mutableStateOf(false) }
    var selectedOpponentId by remember { mutableStateOf<String?>(null) }
    var teamMessage by remember { mutableStateOf<String?>(null) }
    val myTeam = teams.find { it.id == currentUser.teamId }
    val userById = allUsers.associateBy { it.uid }
    val liveOpponentTeams = teams.filter { it.id != currentUser.teamId && it.isLive }

    LazyColumn(modifier.fillMaxSize().background(DarkBg).padding(14.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("SQUADS", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("Manage your squad or browse live opponent squads.", color = TextSecondary, fontSize = 13.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { selectedSection = 0 }, shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = if (selectedSection == 0) KheloGreen else DarkSurfaceCard), modifier = Modifier.weight(1f).height(44.dp)) { Text("MY TEAM", color = if (selectedSection == 0) Color.White else TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                Button(onClick = { selectedSection = 1 }, shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = if (selectedSection == 1) KheloGreen else DarkSurfaceCard), modifier = Modifier.weight(1f).height(44.dp)) { Text("BROWSE TEAM", color = if (selectedSection == 1) Color.White else TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp) }
            }
        }
        if (selectedSection == 0) {
            if (myTeam != null) item { MyTeamCard(team = myTeam, currentUser = currentUser, userById = userById, onChallengeClick = { selectedSection = 1 }, onRespondJoinRequest = { uid, accept -> onRespondJoinRequest(myTeam.id, uid, accept) }, onRemoveMember = { uid -> onRemoveMember(myTeam.id, uid) }) }
            else item {
                Card(Modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), border = borderStroke(1.dp, DarkBorder)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Groups, null, tint = KheloGreenBright, modifier = Modifier.size(42.dp))
                        Text("YOU DON'T HAVE A TEAM", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Create your squad first, then challenge live opponents.", color = TextSecondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { showCreateTeamDialog = true }, shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text("CREATE TEAM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                    }
                }
            }
            item { Text("MY TEAM CHALLENGES", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp) }
            val myChallenges = challenges.filter { it.challengerTeamId == currentUser.teamId || it.challengedTeamId == currentUser.teamId }
            if (myChallenges.isEmpty()) item { Text("No team challenges yet.", color = TextMuted, fontSize = 12.sp) }
            else items(myChallenges) { challenge -> TeamChallengeCard(challenge, currentUser, { onAcceptChallenge(challenge.id) }, { id, room, pass -> onSetChallengeRoom(id, room, pass) }, { onCancelChallenge(challenge.id) }) }
        } else {
            item {
                Surface(Modifier.fillMaxWidth(), color = KheloGreenContainer, border = borderStroke(1.dp, KheloGreen), shape = RectangleShape) {
                    Column(Modifier.padding(14.dp)) {
                        Text("LIVE OPPONENT TEAMS ONLY", color = KheloGreenBright, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("Only live teams appear here. You can challenge an opponent once while an active request exists.", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
            if (myTeam == null) item { Text("Create or join a team before challenging an opponent.", color = EsportsOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            else if (liveOpponentTeams.isEmpty()) item { Text("No live opponent team is available right now.", color = TextMuted, fontSize = 13.sp) }
            else items(liveOpponentTeams) { team ->
                TeamListItem(team, currentUser, { selectedOpponentId = team.id; showChallengeDialog = true }, { onSendJoinRequest(team.id); teamMessage = "Join request sent to " + team.name })
            }
        }
        if (teamMessage != null) item { Surface(color = Color(0xFFEAF4F7), shape = RectangleShape, modifier = Modifier.fillMaxWidth()) { Text(teamMessage!!, color = EsportsCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp)) } }
    }
    if (showCreateTeamDialog) CreateTeamDialog(currentUser.uid, { showCreateTeamDialog = false }) { name, tag, logo, banner -> onCreateTeam(name, tag, logo, banner); showCreateTeamDialog = false }
    if (showChallengeDialog && myTeam != null) SendChallengeDialog(myTeam, liveOpponentTeams.filter { selectedOpponentId == null || it.id == selectedOpponentId }, { showChallengeDialog = false; selectedOpponentId = null }, { id, game, stake -> onChallengeTeam(id, game, stake); showChallengeDialog = false; selectedOpponentId = null })
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
fun TeamChallengeCard(
    challenge: TeamChallenge, currentUser: UserProfile, onAccept: () -> Unit,
    onSetRoom: (challengeId: String, roomId: String, password: String) -> Unit, onCancel: () -> Unit
) {
    val isChallenged = challenge.challengedTeamId == currentUser.teamId
    val isChallenger = challenge.challengerTeamId == currentUser.teamId
    var roomId by remember(challenge.id, challenge.roomId) { mutableStateOf(challenge.roomId ?: "") }
    var password by remember(challenge.id, challenge.roomPassword) { mutableStateOf(challenge.roomPassword ?: "") }
    Card(Modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), border = borderStroke(2.dp, if (challenge.status == ChallengeStatus.CANCELLED) EsportsRed else KheloGreen)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(challenge.game.uppercase(), color = KheloGreenBright, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(challenge.status.name.replace("_", " "), color = if (challenge.status == ChallengeStatus.CANCELLED) EsportsRed else KheloGreenBright, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(challenge.challengerTeamName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text("VS", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(challenge.challengedTeamName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Text("STAKE: ৳${challenge.stakeAmount.toInt()}", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
            if (challenge.status == ChallengeStatus.PENDING && isChallenged) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAccept, shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen), modifier = Modifier.weight(1f).height(40.dp)) { Text("ACCEPT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                    OutlinedButton(onClick = onCancel, shape = RectangleShape, border = borderStroke(1.dp, EsportsRed), modifier = Modifier.weight(1f).height(40.dp)) { Text("CANCEL", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                }
            } else if (challenge.status == ChallengeStatus.PENDING && isChallenger) {
                OutlinedButton(onClick = onCancel, shape = RectangleShape, border = borderStroke(1.dp, EsportsRed), modifier = Modifier.fillMaxWidth().height(40.dp)) { Text("CANCEL CHALLENGE", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 13.sp) }
            }
            if (challenge.status == ChallengeStatus.ACCEPTED && isChallenger) {
                Text("ACCEPTED — SET ROOM ID & PASSWORD", color = KheloGreenBright, fontWeight = FontWeight.Black, fontSize = 13.sp)
                OutlinedTextField(roomId, { roomId = it }, label = { Text("ROOM ID") }, singleLine = true, shape = RectangleShape, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("ROOM PASSWORD") }, singleLine = true, shape = RectangleShape, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onSetRoom(challenge.id, roomId, password) }, enabled = roomId.isNotBlank() && password.isNotBlank(), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen), modifier = Modifier.fillMaxWidth().height(40.dp)) { Text("SAVE & SHOW ROOM TO OPPONENT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp) }
            }
            if (challenge.status == ChallengeStatus.ROOM_SET) {
                Surface(Modifier.fillMaxWidth(), color = Color(0xFFEAF4F7), border = borderStroke(1.dp, EsportsCyan), shape = RectangleShape) {
                    Column(Modifier.padding(12.dp)) {
                        Text("ROOM DETAILS", color = EsportsCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("Room ID: ${challenge.roomId}", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("Password: ${challenge.roomPassword}", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
                OutlinedButton(onClick = onCancel, shape = RectangleShape, border = borderStroke(1.dp, EsportsRed), modifier = Modifier.fillMaxWidth().height(38.dp)) { Text("CANCEL MATCH", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 12.sp) }
            }
        }
    }
}
@Composable
fun TeamListItem(team: Team, currentUser: UserProfile, onChallenge: () -> Unit, onSendJoinRequest: () -> Unit) {
    val alreadyMember = team.members.any { it.userId == currentUser.uid }
    val alreadyRequested = team.joinRequests.contains(currentUser.uid)
    Card(Modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), border = borderStroke(1.dp, if (team.isLive) KheloGreen else DarkBorder)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(team.profileImageUrl.ifBlank { team.logoUrl }, team.tag, 52.dp, 15.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(team.name, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("[${team.tag}] • ${team.members.size} members", color = TextSecondary, fontSize = 12.sp)
                Text(if (team.isLive) "LIVE • AVAILABLE FOR CHALLENGE" else "OFFLINE", color = if (team.isLive) KheloGreenBright else TextMuted, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("${team.wins} WINS • ${team.points} PTS", color = EsportsGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            if (team.isLive && currentUser.teamId != null && !alreadyMember) Button(onClick = onChallenge, shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen), modifier = Modifier.height(42.dp)) { Text("CHALLENGE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp) }
            else if (currentUser.teamId == null && !alreadyMember && !alreadyRequested) OutlinedButton(onClick = onSendJoinRequest, shape = RectangleShape, modifier = Modifier.height(38.dp)) { Text("REQUEST JOIN", color = KheloGreenBright, fontWeight = FontWeight.Black, fontSize = 10.sp) }
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