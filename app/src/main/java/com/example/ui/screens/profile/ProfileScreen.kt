package com.example.ui.screens.profile

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.firebase.StorageManager
import com.example.data.model.*
import com.example.ui.components.Avatar
import com.example.ui.components.LoadingOverlay
import com.example.ui.components.borderStroke
import com.example.ui.components.rememberImagePicker
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    currentUser: UserProfile,
    initialShowDeposit: Boolean = false,
    payments: List<PaymentTransaction>,
    settings: SystemSettings,
    allUsersRanking: List<UserProfile> = emptyList(),
    onUpdateProfile: (name: String, gameUid: String, game: String) -> Unit,
    onProfileImagePicked: (String) -> Unit = {},
    onBannerImagePicked: (String) -> Unit = {},
    onRequestDeposit: (amount: Double, method: String, senderNumber: String, trxId: String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDepositDialog by remember { mutableStateOf(false) }
    var codeCopied by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialShowDeposit) {
        if (initialShowDeposit) showDepositDialog = true
    }

    val pickProfileImage = rememberImagePicker(
        storagePath = StorageManager.getProfilePicturePath(currentUser.uid),
        onUploaded = { onProfileImagePicked(it) },
        onError = { uploadError = it },
        onLoading = { isUploading = it }
    )
    val pickBannerImage = rememberImagePicker(
        storagePath = StorageManager.getProfileBannerPath(currentUser.uid),
        onUploaded = { onBannerImagePicked(it) },
        onError = { uploadError = it },
        onLoading = { isUploading = it }
    )

    val myRank = allUsersRanking
        .sortedByDescending { it.points }
        .indexOfFirst { it.uid == currentUser.uid }
        .let { if (it >= 0) it + 1 else null }

    val myPayments = payments.filter { it.userId == currentUser.uid }

    Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Player Header Card (banner + avatar upload + full stats)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = borderStroke(1.dp, KheloGreen)
            ) {
                Column {
                    // Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .background(KheloGreenContainer, RectangleShape)
                    ) {
                        if (currentUser.profileBannerUrl.isNotBlank()) {
                            AsyncImage(
                                model = currentUser.profileBannerUrl,
                                contentDescription = "Profile Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        TextButton(
                            onClick = pickBannerImage,
                            shape = RectangleShape,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Banner", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Avatar overlapping banner with change button
                            Box {
                                Avatar(
                                    imageUrl = currentUser.profilePictureUrl,
                                    label = currentUser.username,
                                    size = 64.dp,
                                    fontSize = 22.sp,
                                    modifier = Modifier.offset(y = (-28).dp)
                                )
                                IconButton(
                                    onClick = pickProfileImage,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(y = (-24).dp)
                                        .size(26.dp)
                                        .background(DarkSurface, RectangleShape)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "Change photo", tint = KheloGreenBright, modifier = Modifier.size(15.dp))
                                }
                            }

                            IconButton(onClick = { showEditProfileDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = currentUser.fullName.ifBlank { currentUser.username },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "@${currentUser.username}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "UID: ${currentUser.uid}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = DarkSurfaceCard, shape = RectangleShape) {
                                Text(
                                    text = currentUser.preferredGame,
                                    color = KheloGreenBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(color = DarkSurfaceCard, shape = RectangleShape) {
                                Text(
                                    text = currentUser.teamName ?: "Free Agent",
                                    color = EsportsCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceCard, RectangleShape)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem("PLAYED", "${currentUser.matchesPlayed}", TextPrimary)
                            StatItem("WINS", "${currentUser.wins}", KheloGreenBright)
                            StatItem("LOSSES", "${currentUser.losses}", EsportsRed)
                            StatItem("POINTS", "${currentUser.points}", EsportsCyan)
                            StatItem("RANK", myRank?.let { "#$it" } ?: "—", EsportsGold)
                        }
                    }
                }
            }
        }

        if (uploadError != null) {
            item {
                Surface(color = Color(0xFF2E1212), shape = RectangleShape, modifier = Modifier.fillMaxWidth()) {
                    Text(uploadError!!, color = EsportsRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                }
            }
        }

        // Wallet Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = borderStroke(1.dp, EsportsGold)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EsportsGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("KHELO WALLET", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showDepositDialog = true },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("topup_wallet_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = DarkBg, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Money", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "৳${String.format("%.2f", currentUser.walletBalance)} BDT",
                        color = EsportsGold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "Use wallet balance to register instantly for tournaments without waiting for manual payment confirmation.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Referral Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = borderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = EsportsCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REFERRAL PROGRAM", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                        Surface(color = Color(0xFF132B38), shape = RectangleShape) {
                            Text("Earn ৳${settings.referralBonus.toInt()} / user", color = EsportsCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Share referral code with mobile gamers to earn bonus tournament tickets.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceElevated, RectangleShape)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentUser.referralCode, color = KheloGreenBright, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                        TextButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(currentUser.referralCode))
                                codeCopied = true
                            },
                            shape = RectangleShape
                        ) {
                            Text(if (codeCopied) "Copied!" else "Copy Code", color = EsportsCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Logout & Role Reset Button
        item {
            Button(
                onClick = onLogout,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                border = borderStroke(1.dp, EsportsRed),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = EsportsRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out (${currentUser.username})", color = EsportsRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Payment History
        item {
            Text("PAYMENT & DEPOSIT HISTORY", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.5.sp)
        }

        if (myPayments.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard), shape = RectangleShape) {
                    Text("No transactions recorded yet.", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                }
            }
        } else {
            items(myPayments) { trx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(trx.method, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("TrxID: ${trx.transactionId}", color = TextSecondary, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("৳${trx.amount.toInt()}", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Surface(
                                color = if (trx.status == PaymentStatus.PAID) Color(0xFF1B381A) else Color(0xFF382A0E),
                                shape = RectangleShape
                            ) {
                                Text(
                                    trx.status.name,
                                    color = if (trx.status == PaymentStatus.PAID) KheloGreenBright else EsportsOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LoadingOverlay(visible = isUploading, message = "Uploading image...")
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentUser = currentUser,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, gameUid, game ->
                onUpdateProfile(name, gameUid, game)
                showEditProfileDialog = false
            }
        )
    }

    if (showDepositDialog) {
        DepositMoneyDialog(
            settings = settings,
            onDismiss = { showDepositDialog = false },
            onSubmit = { amount, method, senderNumber, trxId ->
                onRequestDeposit(amount, method, senderNumber, trxId)
                showDepositDialog = false
            }
        )
    }



@Composable
fun StatItem(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun EditProfileDialog(
    currentUser: UserProfile,
    onDismiss: () -> Unit,
    onSave: (fullName: String, inGameUid: String, game: String) -> Unit
) {
    var fullName by remember { mutableStateOf(currentUser.fullName) }
    var inGameUid by remember { mutableStateOf(currentUser.inGameUid) }
    var preferredGame by remember { mutableStateOf(currentUser.preferredGame) }

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
                Text("Edit Profile & Game ID", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = inGameUid,
                    onValueChange = { inGameUid = it },
                    label = { Text("In-Game UID / Tag") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Primary Game:", color = TextSecondary, fontSize = 11.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Free Fire", "PUBG Mobile", "eFootball").forEach { g ->
                        FilterChip(
                            selected = preferredGame == g,
                            onClick = { preferredGame = g },
                            shape = RectangleShape,
                            label = { Text(g, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KheloGreen, selectedLabelColor = DarkBg),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Button(
                    onClick = { onSave(fullName, inGameUid, preferredGame) },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text("Save Changes", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DepositMoneyDialog(
    settings: SystemSettings,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, method: String, senderNumber: String, trxId: String) -> Unit
) {
    var amount by remember { mutableStateOf("200") }
    var selectedMethod by remember { mutableStateOf("bKash") }
    var senderNumber by remember { mutableStateOf("") }
    var trxId by remember { mutableStateOf("") }

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
                Text("WALLET DEPOSIT", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(
                    "১) নিচের অফিসিয়াল bKash/Nagad নম্বরে Send Money করুন।\n" +
                    "২) যে নম্বর থেকে টাকা পাঠিয়েছেন সেটা দিন।\n" +
                    "৩) TrxID দিন এবং Submit করুন।\n" +
                    "৪) Admin যাচাই করার পর আপনার Wallet-এ টাকা যোগ হবে।",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("bKash", "Nagad").forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            shape = RectangleShape,
                            label = { Text(method, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KheloGreen, selectedLabelColor = DarkBg),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard), shape = RectangleShape) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("অফিসিয়াল নম্বরে Send Money করুন:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (selectedMethod == "bKash") settings.bkashNumber else settings.nagadNumber,
                            color = EsportsGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() } },
                    label = { Text("ডিপোজিটের পরিমাণ (৳)") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input")
                )

                OutlinedTextField(
                    value = senderNumber,
                    onValueChange = { senderNumber = it },
                    label = { Text("যে মোবাইল নম্বর থেকে টাকা পাঠিয়েছেন") },
                    placeholder = { Text("e.g. 017XXXXXXXX") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("deposit_sender_number_input")
                )

                OutlinedTextField(
                    value = trxId,
                    onValueChange = { trxId = it },
                    label = { Text("Transaction ID (TrxID)") },
                    placeholder = { Text("e.g. 9J2K8L1M") },
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KheloGreen, unfocusedBorderColor = DarkBorder, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("deposit_trx_id_input")
                )

                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && senderNumber.isNotBlank() && trxId.isNotBlank()) {
                            onSubmit(amt, selectedMethod, senderNumber, trxId)
                        }
                    },
                    enabled = senderNumber.isNotBlank() && trxId.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("submit_deposit_button")
                ) {
                    Text("Submit Deposit", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ApkDownloadDialog(
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var copiedMessage by remember { mutableStateOf<String?>(null) }

    val userApkUrl = "https://github.com/khelobd/releases/releases/download/v1.0.0/khelobd.apk"
    val adminApkUrl = "https://github.com/khelobd/releases/releases/download/v1.0.0/khelobdadmin.apk"

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
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DOWNLOAD APKs", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "Khelo BD provides dedicated independent APK packages for general players and tournament administrators:",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                if (copiedMessage != null) {
                    Surface(color = Color(0xFF132B38), shape = RectangleShape, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = copiedMessage!!,
                            color = EsportsCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                // 1. Player App: khelobd.apk
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = borderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. Player App (khelobd.apk)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Surface(color = Color(0xFF1B381A), shape = RectangleShape) {
                                Text("PLAYERS", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            text = "Standard player client with registration, tournaments, matches, wallet deposits, and room ID notifications.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(userApkUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        clipboard.setText(AnnotatedString(userApkUrl))
                                        copiedMessage = "Link copied: khelobd.apk"
                                    }
                                },
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                                modifier = Modifier.weight(1f).height(32.dp).testTag("download_khelobd_apk_btn")
                            ) {
                                Text("Download APK", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(userApkUrl))
                                    copiedMessage = "Link copied: khelobd.apk"
                                },
                                shape = RectangleShape,
                                border = borderStroke(1.dp, DarkBorder),
                                modifier = Modifier.weight(1f).height(32.dp).testTag("copy_khelobd_apk_link")
                            ) {
                                Text("Copy Link", color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 2. Admin App: khelobdadmin.apk
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = borderStroke(1.dp, EsportsGold)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. Admin App (khelobdadmin.apk)", color = EsportsGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Surface(color = Color(0xFF382A0E), shape = RectangleShape) {
                                Text("RESTRICTED", color = EsportsGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            text = "Dedicated Admin APK exclusively for platform moderators. Used for payment approvals, room credentials, and tournament lifecycle.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adminApkUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        clipboard.setText(AnnotatedString(adminApkUrl))
                                        copiedMessage = "Link copied: khelobdadmin.apk"
                                    }
                                },
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = EsportsGold),
                                modifier = Modifier.weight(1f).height(32.dp).testTag("download_khelobdadmin_apk_btn")
                            ) {
                                Text("Download APK", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(adminApkUrl))
                                    copiedMessage = "Link copied: khelobdadmin.apk"
                                },
                                shape = RectangleShape,
                                border = borderStroke(1.dp, DarkBorder),
                                modifier = Modifier.weight(1f).height(32.dp).testTag("copy_khelobdadmin_apk_link")
                            ) {
                                Text("Copy Link", color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
