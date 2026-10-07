package com.example.ui.screens.challenges

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
fun UserChallengeScreen(
    currentUser: UserProfile,
    allUsers: List<UserProfile>,
    userChallenges: List<UserChallenge>,
    onSendChallenge: (String, String) -> Result<Unit>,
    onAcceptChallenge: (String) -> Result<Unit>,
    onRejectChallenge: (String) -> Result<Unit>,
    onCancelChallenge: (String) -> Result<Unit> = { Result.success(Unit) },
    onSetRoomCredentials: (String, String, String) -> Result<Unit>,
    onSubmitProof: (String, String) -> Result<Unit>,
    onOpenDeposit: () -> Unit = {},
    onViewProfile: (UserProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val myChallenges = userChallenges.filter { it.challengerUid == currentUser.uid || it.opponentUid == currentUser.uid }
    val users = allUsers.filter { it.uid != currentUser.uid && !(currentUser.teamId != null && it.teamId == currentUser.teamId) }
    var selectedOpponent by remember { mutableStateOf<UserProfile?>(null) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var alertDeposit by remember { mutableStateOf(false) }

    fun handle(result: Result<Unit>) {
        if (result.isFailure) {
            val raw = result.exceptionOrNull()?.message ?: "অজানা সমস্যা হয়েছে।"
            alertMessage = banglaErrorMessage(raw)
            alertDeposit = raw.contains("ব্যালেন্স নেই", true) || raw.contains("balance", true) || raw.contains("deposit", true)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(DarkBg).padding(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(Modifier.fillMaxWidth(), color = if (currentUser.walletBalance >= 50.0) KheloGreenContainer else Color(0xFFFFEAEA), border = borderStroke(2.dp, if (currentUser.walletBalance >= 50.0) KheloGreen else EsportsRed), shape = RectangleShape) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("PLAYER 1V1 CHALLENGE", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("প্রতি খেলোয়াড় ৫০ টাকা। Accept হলে দুজনের ৫০ টাকা করে Lock হবে।", color = TextSecondary, fontSize = 13.sp)
                    Text("আপনার Wallet: ৳" + currentUser.walletBalance.toInt(), color = if (currentUser.walletBalance >= 50.0) KheloGreen else EsportsRed, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
        item {
            Text("MY ACTIVE CHALLENGES", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        if (myChallenges.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SportsEsports, null, tint = KheloGreen, modifier = Modifier.size(42.dp))
                        Text("কোনো চলমান Challenge নেই", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 17.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("নিচের player list থেকে একজনকে Challenge করুন।", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(myChallenges) { challenge ->
                PlayerChallengeCard(
                    challenge = challenge,
                    currentUser = currentUser,
                    onAccept = { handle(onAcceptChallenge(challenge.id)) },
                    onReject = { handle(onRejectChallenge(challenge.id)) },
                    onCancel = { handle(onCancelChallenge(challenge.id)) },
                    onSetRoom = { room, pass -> handle(onSetRoomCredentials(challenge.id, room, pass)) },
                    onProof = { url -> handle(onSubmitProof(challenge.id, url)) }
                )
            }
        }
        item { Text("CHALLENGE A PLAYER", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp) }
        items(users) { user ->
            PlayerRow(
                user = user,
                onProfile = { onViewProfile(user) },
                onChallenge = { selectedOpponent = user }
            )
        }
    }

    if (selectedOpponent != null) {
        Dialog(onDismissRequest = { selectedOpponent = null }) {
            Surface(Modifier.fillMaxWidth(), color = DarkSurfaceElevated, shape = RectangleShape, border = borderStroke(2.dp, KheloGreen)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CHALLENGE " + selectedOpponent!!.username, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp)
                    Text("Entry: ৳50 each • Winner payout: ৳85", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("Room ২০ মিনিটের মধ্যে দিতে হবে। Room দেওয়ার ১০ মিনিট পরে Screenshot Proof খুলবে। মোট deadline ১ ঘণ্টা।", color = TextSecondary, fontSize = 13.sp)
                    Button(
                        onClick = {
                            val result = onSendChallenge(selectedOpponent!!.uid, selectedOpponent!!.username)
                            if (result.isFailure) {
                                val raw = result.exceptionOrNull()?.message ?: "চ্যালেঞ্জ পাঠানো যায়নি।"
                                alertMessage = banglaErrorMessage(raw)
                                alertDeposit = raw.contains("ব্যালেন্স নেই", true) || raw.contains("balance", true) || raw.contains("deposit", true)
                            }
                            if (result.isSuccess) selectedOpponent = null
                        },
                        enabled = currentUser.walletBalance >= 50.0,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)
                    ) { Text(if (currentUser.walletBalance >= 50.0) "SEND CHALLENGE — ৳50" else "আগে ৫০ টাকা Deposit করুন", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp) }
                    OutlinedButton(onClick = { selectedOpponent = null }, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RectangleShape) { Text("বন্ধ করুন", fontSize = 14.sp) }
                }
            }
        }
    }

    if (alertMessage != null) {
        BanglaAlertDialog(
            title = if (alertDeposit) "ব্যালেন্স প্রয়োজন" else "চ্যালেঞ্জ বার্তা",
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
private fun PlayerRow(user: UserProfile, onProfile: () -> Unit, onChallenge: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onProfile() }, shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), border = borderStroke(1.dp, DarkBorder)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(user.profilePictureUrl, user.username, 54.dp, 17.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(user.fullName.ifBlank { user.username }, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("@" + user.username, color = TextSecondary, fontSize = 12.sp)
                Text(user.preferredGame + " • " + user.inGameUid.ifBlank { "Game UID নেই" }, color = KheloGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Button(onClick = onChallenge, shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen), modifier = Modifier.height(42.dp)) { Text("CHALLENGE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun PlayerChallengeCard(
    challenge: UserChallenge,
    currentUser: UserProfile,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    onSetRoom: (String, String) -> Unit,
    onProof: (String) -> Unit
) {
    var room by remember(challenge.id, challenge.roomId) { mutableStateOf(challenge.roomId ?: "") }
    var pass by remember(challenge.id, challenge.roomPassword) { mutableStateOf(challenge.roomPassword ?: "") }
    var showProof by remember { mutableStateOf(false) }
    val mine = challenge.challengerUid == currentUser.uid
    val statusColor = when (challenge.status) {
        UserChallengeStatus.COMPLETED -> KheloGreen
        UserChallengeStatus.REJECTED, UserChallengeStatus.CANCELLED -> EsportsRed
        UserChallengeStatus.ROOM_SET, UserChallengeStatus.PROOF_SUBMITTED, UserChallengeStatus.UNDER_REVIEW -> EsportsCyan
        else -> EsportsOrange
    }

    Card(Modifier.fillMaxWidth(), shape = RectangleShape, colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated), border = borderStroke(2.dp, statusColor)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("1v1 • " + challenge.game, color = KheloGreen, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("৳" + challenge.stakeAmount.toInt() + " each • Pool ৳100", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
                Surface(color = statusColor.copy(alpha = 0.12f), border = borderStroke(1.dp, statusColor), shape = RectangleShape) {
                    Text(challenge.status.name.replace("_", " "), color = statusColor, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.padding(7.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(challenge.challengerName, color = if (mine) KheloGreen else TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("VS", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(challenge.opponentName, color = if (!mine) KheloGreen else TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }

            if (challenge.status == UserChallengeStatus.PENDING) {
                if (!mine) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f).height(44.dp), shape = RectangleShape, border = borderStroke(1.dp, EsportsRed)) { Text("REJECT", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                        Button(onClick = onAccept, modifier = Modifier.weight(1f).height(44.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text("ACCEPT ৳50", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                    }
                } else {
                    Text("Opponent Accept করার অপেক্ষা...", color = EsportsOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RectangleShape, border = borderStroke(1.dp, EsportsRed)) { Text("CANCEL", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 13.sp) }
                }
            }

            if (challenge.status == UserChallengeStatus.ACCEPTED) {
                CountdownLine("ROOM দেওয়ার deadline", (challenge.acceptedAtMillis.takeIf { it > 0L } ?: challenge.timestamp) + 20 * 60 * 1000L, EsportsOrange)
                if (mine) {
                    OutlinedTextField(room, { room = it }, label = { Text("Room ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(pass, { pass = it }, label = { Text("Room Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onSetRoom(room, pass) }, enabled = room.isNotBlank() && pass.isNotBlank(), modifier = Modifier.fillMaxWidth().height(44.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text("ROOM SET KORUN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
                } else {
                    Text("Challenger Room ID ও Password দেওয়ার অপেক্ষা...", color = TextSecondary, fontSize = 13.sp)
                }
            }

            if (challenge.status == UserChallengeStatus.ROOM_SET || challenge.status == UserChallengeStatus.PROOF_SUBMITTED) {
                Surface(Modifier.fillMaxWidth(), color = Color(0xFFEAF6FA), border = borderStroke(1.dp, EsportsCyan), shape = RectangleShape) {
                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("ROOM DETAILS", color = EsportsCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text("Room ID: " + (challenge.roomId ?: "—"), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("Password: " + (challenge.roomPassword ?: "—"), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
                CountdownLine("মোট Match deadline", challenge.deadlineAtMillis, EsportsGold)
                if (challenge.proofOpenAtMillis > 0L && System.currentTimeMillis() < challenge.proofOpenAtMillis) {
                    CountdownLine("Screenshot Proof খুলবে", challenge.proofOpenAtMillis, EsportsCyan)
                } else {
                    val mineProof = if (mine) challenge.challengerProofUrl else challenge.opponentProofUrl
                    if (mineProof.isNullOrBlank()) {
                        Button(onClick = { showProof = true }, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = EsportsCyan)) { Text("SUBMIT MATCH SCREENSHOT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
                    } else {
                        Text("আপনার Screenshot জমা হয়েছে। Admin review করবেন।", color = KheloGreen, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    if (!mine && challenge.roomId != null) {
                        OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RectangleShape, border = borderStroke(1.dp, EsportsRed)) { Text("WRONG ROOM REPORT", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                    }
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RectangleShape, border = borderStroke(1.dp, EsportsRed)) { Text("CANCEL / REFUND", color = EsportsRed, fontWeight = FontWeight.Black, fontSize = 13.sp) }
            }

            if (challenge.status == UserChallengeStatus.UNDER_REVIEW) {
                Text("দুই পক্ষের Screenshot Admin panel-এ গেছে। Admin Winner নির্বাচন করবেন।", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            if (challenge.status == UserChallengeStatus.COMPLETED) {
                Text(if (challenge.winnerUid == currentUser.uid) "আপনি জিতেছেন। ৮৫ টাকা Wallet-এ যোগ হয়েছে।" else if (challenge.winnerUid != null) "আপনি হেরেছেন।" else "Match সম্পূর্ণ হয়নি; fee কেটে বাকি টাকা ফেরত দেওয়া হয়েছে।", color = if (challenge.winnerUid == currentUser.uid) KheloGreen else EsportsRed, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }

    if (showProof) {
        ProofUploadDialog(
            storagePath = StorageManager.getChallengePicPath(challenge.id, currentUser.uid),
            onDismiss = { showProof = false },
            onSubmit = { url -> showProof = false; onProof(url) }
        )
    }
}

@Composable
private fun CountdownLine(label: String, untilMillis: Long, color: Color) {
    var now by remember(untilMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(untilMillis) { while (true) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(1000L) } }
    val left = (untilMillis - now).coerceAtLeast(0L) / 1000L
    val minutes = left / 60L
    val seconds = left % 60L
    Surface(Modifier.fillMaxWidth(), color = color.copy(alpha = 0.08f), border = borderStroke(1.dp, color), shape = RectangleShape) {
        Row(Modifier.fillMaxWidth().padding(9.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = color, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text("%02dm %02ds".format(minutes, seconds), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ProofUploadDialog(storagePath: String, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var uploaded by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val pick = rememberImagePicker(storagePath, { uploaded = it; error = null }, { error = it })
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.fillMaxWidth(), color = DarkSurfaceElevated, shape = RectangleShape, border = borderStroke(2.dp, EsportsCyan)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MATCH SCREENSHOT", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text("Victory screen / final scoreboard-এর পরিষ্কার Screenshot দিন।", color = TextSecondary, fontSize = 13.sp)
                if (uploaded.isNotBlank()) AsyncImage(uploaded, null, Modifier.fillMaxWidth().height(170.dp), contentScale = ContentScale.Crop)
                if (error != null) Text(error!!, color = EsportsRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Button(onClick = pick, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = EsportsCyan)) { Text(if (uploaded.isBlank()) "UPLOAD SCREENSHOT" else "CHANGE SCREENSHOT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
                Button(onClick = { if (uploaded.isNotBlank()) onSubmit(uploaded) }, enabled = uploaded.isNotBlank(), modifier = Modifier.fillMaxWidth().height(46.dp), shape = RectangleShape, colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)) { Text("SUBMIT PROOF", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp) }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RectangleShape) { Text("CLOSE", fontSize = 13.sp) }
            }
        }
    }
}