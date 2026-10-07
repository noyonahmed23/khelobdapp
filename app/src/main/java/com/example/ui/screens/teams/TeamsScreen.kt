package com.example.ui.screens.teams

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.firebase.StorageManager
import com.example.data.model.*
import com.example.ui.components.Avatar
import com.example.ui.components.BanglaAlertDialog
import com.example.ui.components.banglaErrorMessage
import com.example.ui.components.borderStroke
import com.example.ui.components.rememberImagePicker
import com.example.ui.theme.*

@Composable
fun TeamsScreen(
    teams: List<Team>,
    challenges: List<TeamChallenge>,
    currentUser: UserProfile,
    allUsers: List<UserProfile> = emptyList(),
    onCreateTeam: (String, String, String, String) -> Result<Unit>,
    onChallengeTeam: (String, String, Double) -> Result<Unit>,
    onAcceptChallenge: (String) -> Result<Unit>,
    onSetChallengeRoom: (String, String, String) -> Result<Unit> = { _, _, _ -> Result.success(Unit) },
    onSubmitProof: (String, String) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    onReportWrongRoom: (String) -> Result<Unit> = { Result.success(Unit) },
    onCancelChallenge: (String) -> Result<Unit> = { Result.success(Unit) },
    onSendJoinRequest: (String) -> Result<Unit> = { Result.success(Unit) },
    onRespondJoinRequest: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onRemoveMember: (String, String) -> Unit = { _, _ -> },
    onOpenDeposit: () -> Unit = {},
    onViewProfile: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var section by remember { mutableIntStateOf(0) }
    var selectedOpponent by remember { mutableStateOf<Team?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var alertDeposit by remember { mutableStateOf(false) }
    val myTeam = teams.find { it.id == currentUser.teamId }
    val liveTeams = teams.filter { it.id != currentUser.teamId && it.isLive }
    val userMap = allUsers.associateBy { it.uid }

    fun handle(result: Result<Unit>) {
        if (result.isFailure) {
            val raw = result.exceptionOrNull()?.message ?: "সমস্যা হয়েছে।"
            alertMessage = banglaErrorMessage(raw)
            alertDeposit = raw.contains("ব্যালেন্স নেই", true) || raw.contains("balance", true) || raw.contains("deposit", true)
        }
    }

    LazyColumn(modifier.fillMaxSize().background(DarkBg).padding(14.dp), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("TEAM CHALLENGE", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 21.sp)
            Text("Live team browse করুন। Team captain ৫০ টাকা stake-এ challenge পাঠাতে পারবেন।", color = TextSecondary, fontSize = 13.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { section = 0 }, modifier = Modifier.weight(1f).height(46.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = if (section == 0) KheloGreen else DarkSurfaceCard)) { Text("MY TEAM", color = if (section == 0) Color.White else TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp) }
                Button(onClick = { section = 1 }, modifier = Modifier.weight(1f).height(46.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = if (section == 1) KheloGreen else DarkSurfaceCard)) { Text("BROWSE TEAM", color = if (section == 1) Color.White else TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp) }
            }
        }

        if (section == 0) {
            if (myTeam == null) {
                item {
                    Card(Modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), border = borderStroke(2.dp, EsportsOrange)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Groups, null, tint = KheloGreen, modifier = Modifier.size(48.dp))
                            Text("আপনার কোনো Team নেই", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text("CREATE MY TEAM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
                        }
                    }
                }
            } else {
                item { MyTeamCard(myTeam, currentUser, userMap, onViewProfile, onRemoveMember) }
                item { Text("MY TEAM CHALLENGES", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp) }
                val mine = challenges.filter { it.challengerTeamId == myTeam.id || it.challengedTeamId == myTeam.id }
                if (mine.isEmpty()) item { Text("এখনো কোনো Team Challenge নেই।", color = TextMuted, fontSize = 13.sp) }
                else items(mine) { challenge ->
                    TeamChallengeCard(
                        challenge = challenge,
                        currentUser = currentUser,
                        onAccept = { handle(onAcceptChallenge(challenge.id)) },
                        onSetRoom = { room, pass -> handle(onSetChallengeRoom(challenge.id, room, pass)) },
                        onProof = { url -> handle(onSubmitProof(challenge.id, url)) },
                        onWrongRoom = { handle(onReportWrongRoom(challenge.id)) },
                        onCancel = { handle(onCancelChallenge(challenge.id)) }
                    )
                }
            }
        } else {
            item {
                Surface(Modifier.fillMaxWidth(), color = Color(0xFFEAF7ED), border = borderStroke(1.dp, KheloGreen), shape = RectangleShape) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("LIVE TEAMS ONLY", color = KheloGreen, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("Offline team এখানে দেখানো হবে না। Challenge একবার active হলে একই দুই team আবার challenge করা যাবে না।", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
            if (myTeam == null) item { Text("Challenge পাঠানোর জন্য আগে Team তৈরি বা join করুন।", color = EsportsOrange, fontWeight = FontWeight.Black, fontSize = 14.sp) }
            else if (myTeam.captainId != currentUser.uid) item { Text("Challenge পাঠাতে Team Captain account ব্যবহার করুন।", color = EsportsOrange, fontWeight = FontWeight.Black, fontSize = 14.sp) }
            else if (liveTeams.isEmpty()) item { Text("এখন কোনো Live opponent Team নেই।", color = TextMuted, fontSize = 14.sp) }
            else items(liveTeams) { team ->
                TeamRow(
                    team = team,
                    currentUser = currentUser,
                    onProfile = { },
                    onChallenge = { selectedOpponent = team },
                    onJoin = { handle(onSendJoinRequest(team.id)) }
                )
            }
        }
    }

    if (showCreate) {
        CreateTeamDialog(
            teamKey = currentUser.uid,
            onDismiss = { showCreate = false },
            onCreate = { name, tag, logo, banner ->
                val result = onCreateTeam(name, tag, logo, banner)
                handle(result)
                if (result.isSuccess) showCreate = false
            }
        )
    }

    if (selectedOpponent != null && myTeam != null) {
        TeamChallengeDialog(
            target = selectedOpponent!!,
            wallet = currentUser.walletBalance,
            onDismiss = { selectedOpponent = null },
            onSend = {
                val result = onChallengeTeam(selectedOpponent!!.id, "Free Fire", 50.0)
                handle(result)
                if (result.isSuccess) selectedOpponent = null
            }
        )
    }

    if (alertMessage != null) {
        BanglaAlertDialog(
            title = if (alertDeposit) "ব্যালেন্স প্রয়োজন" else "Team Challenge",
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
private fun TeamRow(team: Team, currentUser: UserProfile, onProfile: () -> Unit, onChallenge: () -> Unit, onJoin: () -> Unit) {
    val isCaptain = team.captainId == currentUser.uid
    Card(Modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), border = borderStroke(2.dp, KheloGreen)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(team.profileImageUrl.ifBlank { team.logoUrl }, team.tag, 58.dp, 17.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(team.name, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("[" + team.tag + "] • " + team.members.size + " members", color = TextSecondary, fontSize = 12.sp)
                Text("LIVE • " + team.wins + " Wins • " + team.points + " Points", color = KheloGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            if (currentUser.teamId == null) {
                OutlinedButton(onClick = onJoin, modifier = Modifier.height(42.dp), shape = RectangleShape) { Text("JOIN", color = KheloGreen, fontWeight = FontWeight.Black, fontSize = 12.sp) }
            } else if (isCaptain) {
                Button(onClick = onChallenge, modifier = Modifier.height(46.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text("CHALLENGE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun MyTeamCard(team: Team, currentUser: UserProfile, users: Map<String, UserProfile>, onViewProfile: (UserProfile) -> Unit, onRemoveMember: (String, String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), border = borderStroke(2.dp, KheloGreen)) {
        Column {
            if (team.bannerUrl.isNotBlank()) AsyncImage(team.bannerUrl, null, Modifier.fillMaxWidth().height(110.dp), contentScale = ContentScale.Crop)
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(team.profileImageUrl.ifBlank { team.logoUrl }, team.tag, 68.dp, 18.sp)
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text(team.name, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("[" + team.tag + "] • Captain: " + team.captainName, color = TextSecondary, fontSize = 13.sp)
                        Text(if (team.isLive) "LIVE / AVAILABLE" else "OFFLINE", color = if (team.isLive) KheloGreen else EsportsRed, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
                Text(team.wins.toString() + " WINS  •  " + team.losses + " LOSSES  •  " + team.points + " POINTS", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("MEMBERS", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                team.members.forEach { member ->
                    val profile = users[member.userId]
                    Row(Modifier.fillMaxWidth().clickable(enabled = profile != null) { if (profile != null) onViewProfile(profile) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(profile?.profilePictureUrl ?: "", member.username, 38.dp, 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(member.username, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(if (member.isCaptain) "CAPTAIN" else "MEMBER", color = if (member.isCaptain) EsportsGold else TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        if (team.captainId == currentUser.uid && !member.isCaptain) {
                            IconButton(onClick = { onRemoveMember(team.id, member.userId) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.PersonRemove, null, tint = EsportsRed, modifier = Modifier.size(19.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamChallengeDialog(target: Team, wallet: Double, onDismiss: () -> Unit, onSend: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(), color = DarkSurfaceElevated, shape = RectangleShape, border = borderStroke(2.dp, KheloGreen)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CHALLENGE " + target.name, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("Stake: ৳50 per Team Captain", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text("আপনার Captain wallet: ৳" + wallet.toInt(), color = if (wallet >= 50) KheloGreen else EsportsRed, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text("Accept হলে দুই Team Captain-এর ৫০ টাকা করে Lock হবে। Challenger ২০ মিনিটের মধ্যে Room না দিলে opponent auto win হবে।", color = TextSecondary, fontSize = 13.sp)
                Button(onClick = onSend, enabled = wallet >= 50, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text(if (wallet >= 50) "SEND 50 TK CHALLENGE" else "আগে Deposit করুন", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp) }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RectangleShape) { Text("CANCEL", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun TeamChallengeCard(challenge: TeamChallenge, currentUser: UserProfile, onAccept: () -> Unit, onSetRoom: (String, String) -> Unit, onProof: (String) -> Unit, onWrongRoom: () -> Unit, onCancel: () -> Unit) {
    val isChallenger = challenge.challengerTeamId == currentUser.teamId
    var room by remember(challenge.id, challenge.roomId) { mutableStateOf(challenge.roomId ?: "") }
    var pass by remember(challenge.id, challenge.roomPassword) { mutableStateOf(challenge.roomPassword ?: "") }
    var showProof by remember { mutableStateOf(false) }
    val color = when (challenge.status) {
        ChallengeStatus.COMPLETED -> if (challenge.winnerTeamId == currentUser.teamId) KheloGreen else EsportsRed
        ChallengeStatus.CANCELLED -> EsportsRed
        ChallengeStatus.ROOM_SET, ChallengeStatus.PROOF_SUBMITTED, ChallengeStatus.UNDER_REVIEW -> EsportsCyan
        else -> EsportsOrange
    }
    Card(Modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), border = borderStroke(2.dp, color)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(challenge.game.uppercase(), color = KheloGreen, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(challenge.status.name.replace("_", " "), color = color, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(challenge.challengerTeamName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("VS", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(challenge.challengedTeamName, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Text("৳50 each • Pool ৳100 • Winner ৳85", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp)

            if (challenge.status == ChallengeStatus.PENDING) {
                if (!isChallenger) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(44.dp), shape = RectangleShape, border = borderStroke(1.dp, EsportsRed)) { Text("REJECT / CANCEL", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 11.sp) }
                        Button(onClick = onAccept, modifier = Modifier.weight(1f).height(44.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text("ACCEPT ৳50", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                    }
                } else {
                    Text("Opponent Team Accept করার অপেক্ষা...", color = EsportsOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RectangleShape, border = borderStroke(1.dp, EsportsRed)) { Text("CANCEL", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                }
            }
            if (challenge.status == ChallengeStatus.ACCEPTED) {
                CountdownLine("Room deadline", (challenge.acceptedAtMillis.takeIf { it > 0L } ?: challenge.timestamp) + 20 * 60 * 1000L, EsportsOrange)
                if (isChallenger) {
                    OutlinedTextField(room, { room = it }, label = { Text("Room ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(pass, { pass = it }, label = { Text("Room Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onSetRoom(room, pass) }, enabled = room.isNotBlank() && pass.isNotBlank(), modifier = Modifier.fillMaxWidth().height(45.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text("SET ROOM & START MATCH", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
                } else Text("Challenger Team Room দেওয়ার অপেক্ষা...", color = TextSecondary, fontSize = 13.sp)
            }
            if (challenge.status == ChallengeStatus.ROOM_SET || challenge.status == ChallengeStatus.PROOF_SUBMITTED) {
                Surface(Modifier.fillMaxWidth(), color = Color(0xFFEAF6FA), border = borderStroke(1.dp, EsportsCyan), shape = RectangleShape) {
                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ROOM", color = EsportsCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("ID: " + (challenge.roomId ?: "—"), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("PASS: " + (challenge.roomPassword ?: "—"), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
                CountdownLine("Total Match deadline", challenge.deadlineAtMillis, EsportsGold)
                if (challenge.proofOpenAtMillis > 0L && System.currentTimeMillis() < challenge.proofOpenAtMillis) CountdownLine("Proof খুলবে", challenge.proofOpenAtMillis, EsportsCyan)
                else {
                    val proof = if (isChallenger) challenge.challengerProofUrl else challenge.challengedProofUrl
                    if (proof.isNullOrBlank()) Button(onClick = { showProof = true }, modifier = Modifier.fillMaxWidth().height(45.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = EsportsCyan)) { Text("UPLOAD MATCH SCREENSHOT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                    else Text("আপনার Proof জমা হয়েছে। Admin review করবেন।", color = KheloGreen, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    if (!isChallenger) OutlinedButton(onClick = onWrongRoom, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RectangleShape, border = borderStroke(1.dp, EsportsRed)) { Text("WRONG ROOM REPORT", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RectangleShape, border = borderStroke(1.dp, EsportsRed)) { Text("CANCEL / REFUND", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 13.sp) }
            }
            if (challenge.status == ChallengeStatus.UNDER_REVIEW) Text("দুই Team-এর Proof Admin panel-এ গেছে। Winner Admin ঠিক করবেন।", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
            if (challenge.status == ChallengeStatus.COMPLETED) Text(if (challenge.winnerTeamId == currentUser.teamId) "আপনার Team জিতেছে — ৮৫ টাকা Captain wallet-এ যোগ হয়েছে।" else if (challenge.winnerTeamId != null) "আপনার Team হেরে গেছে।" else "Match সম্পূর্ণ হয়নি; ২০ টাকা fee রেখে ৩০ টাকা করে refund হয়েছে।", color = if (challenge.winnerTeamId == currentUser.teamId) KheloGreen else EsportsRed, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
    if (showProof) ProofDialog(StorageManager.getChallengePicPath(challenge.id, currentUser.uid), { showProof = false }) { url -> showProof = false; onProof(url) }
}

@Composable
private fun CountdownLine(label: String, untilMillis: Long, color: Color) {
    var now by remember(untilMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(untilMillis) { while (true) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(1000L) } }
    val left = (untilMillis - now).coerceAtLeast(0L) / 1000L
    val m = left / 60L
    val sec = left % 60L
    Surface(Modifier.fillMaxWidth(), color = color.copy(alpha = 0.08f), border = borderStroke(1.dp, color), shape = RectangleShape) {
        Row(Modifier.fillMaxWidth().padding(9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = color, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text("%02dm %02ds".format(m, sec), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ProofDialog(storagePath: String, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var uploaded by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val pick = rememberImagePicker(storagePath, { uploaded = it; error = null }, { error = it })
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(), color = DarkSurfaceElevated, shape = RectangleShape, border = borderStroke(2.dp, EsportsCyan)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MATCH PROOF", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text("Final result / victory screen-এর পরিষ্কার Screenshot দিন।", color = TextSecondary, fontSize = 13.sp)
                if (uploaded.isNotBlank()) AsyncImage(uploaded, null, Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Crop)
                if (error != null) Text(error!!, color = EsportsRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Button(onClick = pick, modifier = Modifier.fillMaxWidth().height(45.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = EsportsCyan)) { Text("UPLOAD", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
                Button(onClick = { if (uploaded.isNotBlank()) onSubmit(uploaded) }, enabled = uploaded.isNotBlank(), modifier = Modifier.fillMaxWidth().height(45.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text("SUBMIT PROOF", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RectangleShape) { Text("CLOSE", fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun CreateTeamDialog(teamKey: String, onDismiss: () -> Unit, onCreate: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var logo by remember { mutableStateOf("") }
    var banner by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val pickLogo = rememberImagePicker(StorageManager.getTeamProfileImagePath(teamKey), { logo = it }, { error = it })
    val pickBanner = rememberImagePicker(StorageManager.getTeamBannerPath(teamKey), { banner = it }, { error = it })
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(), color = DarkSurfaceElevated, shape = RectangleShape, border = borderStroke(2.dp, KheloGreen)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("CREATE TEAM", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                OutlinedTextField(name, { name = it }, label = { Text("Team Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tag, { tag = it.take(8) }, label = { Text("Team Tag") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = pickLogo, modifier = Modifier.weight(1f).height(44.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text("UPLOAD LOGO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                    Button(onClick = pickBanner, modifier = Modifier.weight(1f).height(44.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = EsportsCyan)) { Text("UPLOAD BANNER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                }
                if (error != null) Text(error!!, color = EsportsRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Button(onClick = { if (name.isNotBlank() && tag.isNotBlank()) onCreate(name, tag, logo, banner) }, enabled = name.isNotBlank() && tag.isNotBlank(), modifier = Modifier.fillMaxWidth().height(48.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text("CREATE TEAM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp) }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RectangleShape) { Text("CANCEL", fontSize = 13.sp) }
            }
        }
    }
}