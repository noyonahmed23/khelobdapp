package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import coil.compose.AsyncImage
import com.example.data.firebase.StorageManager
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanglaAlertDialog(
    title: String = "বার্তা",
    message: String,
    confirmText: String = "ঠিক আছে",
    onConfirm: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onConfirm) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurfaceElevated,
            shape = RectangleShape,
            border = borderStroke(1.dp, EsportsOrange)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(message, color = TextSecondary, fontSize = 15.sp, lineHeight = 21.sp)
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = KheloGreen)
                ) {
                    Text(confirmText, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
    }
}

fun banglaErrorMessage(raw: String): String = when {
    raw.contains("Insufficient", ignoreCase = true) ||
        raw.contains("does not have enough balance", ignoreCase = true) ->
        "আপনার অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই। আগে ডিপোজিট করুন।"
    raw.contains("already registered", ignoreCase = true) ->
        "আপনি এই টুর্নামেন্টে আগেই রেজিস্টার করেছেন।"
    raw.contains("Registration is closed", ignoreCase = true) ->
        "এই টুর্নামেন্টের রেজিস্ট্রেশন এখন বন্ধ।"
    raw.contains("already have an open challenge", ignoreCase = true) ||
        raw.contains("active challenge already exists", ignoreCase = true) ->
        "এই প্রতিপক্ষের সাথে একটি চলমান চ্যালেঞ্জ আগে থেকেই আছে।"
    raw.contains("Only the", ignoreCase = true) ->
        "এই কাজটি শুধু নির্ধারিত ক্যাপ্টেন/খেলোয়াড় করতে পারবেন।"
    raw.contains("not live", ignoreCase = true) ->
        "এই টিমটি এখন লাইভ নেই, তাই চ্যালেঞ্জ করা যাবে না।"
    raw.contains("Room ID", ignoreCase = true) && raw.contains("required", ignoreCase = true) ->
        "রুম আইডি এবং পাসওয়ার্ড দুটিই দিতে হবে।"
    raw.contains("Proof can only", ignoreCase = true) ||
        raw.contains("Proof is available", ignoreCase = true) ->
        "রুম দেওয়ার ১০ মিনিট পরে প্রুফ/স্ক্রিনশট জমা দেওয়া যাবে।"
    raw.contains("You cannot challenge yourself", ignoreCase = true) ->
        "নিজেকে চ্যালেঞ্জ করা যাবে না।"
    raw.contains("team member", ignoreCase = true) ->
        "নিজের টিমের সদস্যকে চ্যালেঞ্জ করা যাবে না।"
    else -> raw
}

@Composable
fun KheloTopBar(
    currentUser: UserProfile,
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    onRoleSwitchClick: () -> Unit,
    onAdminClick: () -> Unit,
    onSuperAdminClick: () -> Unit,
    onLogoutClick: () -> Unit,
    showAdminControls: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkSurface,
        shape = RectangleShape,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand Logo & Title (Flat Solid Color, Sharp Rectangle)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("brand_header")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(KheloGreen, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "K",
                            color = DarkBg,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "KHELO ",
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "BD",
                                color = KheloGreenBright,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Esports Tournaments",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right Actions: Wallet, Notification, Role, Logout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Wallet Chip (Sharp Rectangle, Solid Color)
                    Surface(
                        shape = RectangleShape,
                        color = DarkSurfaceCard,
                        border = borderStroke(1.dp, DarkBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = EsportsGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "৳${currentUser.walletBalance.toInt()}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Notification Icon
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge(
                                    containerColor = EsportsRed,
                                    contentColor = Color.White
                                ) {
                                    Text(text = "$unreadNotificationsCount")
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onNotificationsClick,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Role Chip (Only visible for Admin / Super Admin)
                    if (showAdminControls && currentUser.role != UserRole.USER) {
                        AssistChip(
                            onClick = onRoleSwitchClick,
                            label = {
                                Text(
                                    text = currentUser.role.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (currentUser.role == UserRole.SUPER_ADMIN) EsportsGold else EsportsCyan
                                )
                            },
                            shape = RectangleShape,
                            colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurfaceElevated),
                            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = DarkBorder),
                            modifier = Modifier.testTag("role_switcher_chip")
                        )
                    }

                    // Logout Button
                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Quick Banner for Admin / Super Admin
            if (showAdminControls && (currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.SUPER_ADMIN)) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onAdminClick,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = KheloGreenContainer),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .testTag("open_admin_panel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = OnKheloGreenContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Admin Panel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = OnKheloGreenContainer
                        )
                    }

                    if (currentUser.role == UserRole.SUPER_ADMIN) {
                        Button(
                            onClick = onSuperAdminClick,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF382A05)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("open_super_admin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = EsportsGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Super Admin",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = EsportsGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentStatusBadge(status: TournamentStatus) {
    val (bgColor, textColor, text) = when (status) {
        TournamentStatus.REGISTRATION -> Triple(Color(0xFF1B331A), KheloGreenBright, "OPEN REGISTRATION")
        TournamentStatus.REGISTRATION_CLOSED -> Triple(Color(0xFF3B2E15), EsportsOrange, "REGISTRATION CLOSED")
        TournamentStatus.GROUP_STAGE -> Triple(Color(0xFF142B3B), EsportsCyan, "GROUP STAGE")
        TournamentStatus.KNOCKOUT -> Triple(Color(0xFF3B152B), Color(0xFFFF4081), "KNOCKOUTS")
        TournamentStatus.FINAL -> Triple(Color(0xFF3E2D07), EsportsGold, "GRAND FINAL")
        TournamentStatus.COMPLETED -> Triple(Color(0xFF262626), Color(0xFFB0B0B0), "COMPLETED")
        TournamentStatus.CANCELLED -> Triple(Color(0xFF381414), EsportsRed, "CANCELLED")
        TournamentStatus.DRAFT -> Triple(Color(0xFF262626), Color.LightGray, "DRAFT")
    }

    Surface(
        color = bgColor,
        shape = RectangleShape
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MatchStatusBadge(status: MatchStatus) {
    val (bgColor, textColor, text) = when (status) {
        MatchStatus.ROOM_READY -> Triple(Color(0xFF1B381A), KheloGreenBright, "ROOM READY")
        MatchStatus.LIVE -> Triple(Color(0xFF421515), EsportsRed, "● LIVE NOW")
        MatchStatus.SCHEDULED -> Triple(Color(0xFF1B2838), EsportsCyan, "SCHEDULED")
        MatchStatus.UNDER_REVIEW -> Triple(Color(0xFF382A0E), EsportsOrange, "UNDER REVIEW")
        MatchStatus.VERIFIED, MatchStatus.COMPLETED -> Triple(Color(0xFF1A3828), Color(0xFF69F0AE), "VERIFIED")
        MatchStatus.DISPUTED -> Triple(Color(0xFF421B1B), EsportsRed, "DISPUTED")
        MatchStatus.WAITING -> Triple(Color(0xFF2B281A), Color.Yellow, "WAITING")
        MatchStatus.RESULT_PENDING -> Triple(Color(0xFF38281A), EsportsOrange, "PENDING")
        MatchStatus.CANCELLED -> Triple(Color(0xFF262626), Color.Gray, "CANCELLED")
    }

    Surface(
        color = bgColor,
        shape = RectangleShape
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(width, color)
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading overlay — smooth loading state used across both apps
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoadingOverlay(
    visible: Boolean,
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    if (!visible) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg.copy(alpha = 0.72f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, KheloGreen)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = KheloGreenBright, strokeWidth = 3.dp)
                Text(message, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Small inline spinner row for section-level loading. */
@Composable
fun InlineLoader(text: String = "Please wait...", modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(color = KheloGreenBright, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        Text(text, color = TextMuted, fontSize = 11.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Avatar — shows uploaded image or falls back to initials (sharp square style)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun Avatar(
    imageUrl: String?,
    label: String,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(KheloGreenContainer, RectangleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = label.take(2).uppercase(),
                color = KheloGreenBright,
                fontWeight = FontWeight.Black,
                fontSize = fontSize
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Image picker + Firebase Storage upload helper
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns a launch function that opens the system image picker, uploads the chosen
 * image to Firebase Storage at [storagePath], and invokes [onUploaded] with the
 * download URL. [onLoading] reports upload start/finish so callers can show spinners.
 */
@Composable
fun rememberImagePicker(
    storagePath: String,
    onUploaded: (String) -> Unit,
    onError: (String) -> Unit = {},
    onLoading: (Boolean) -> Unit = {}
): () -> Unit {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onLoading(true)
            scope.launch {
                try {
                    val url = StorageManager.uploadImage(uri, storagePath)
                    if (url != null) {
                        onUploaded(url)
                    } else {
                        onError("ছবি আপলোড করা যায়নি। Firebase Storage চালু ও Storage Rules publish করা আছে কি না দেখুন।")
                    }
                } catch (e: Exception) {
                    val detail = e.message?.takeIf { it.isNotBlank() } ?: "অজানা সমস্যা"
                    onError("ছবি আপলোড ব্যর্থ: $detail")
                } finally {
                    onLoading(false)
                }
            }
        }
    }
    return { launcher.launch("image/*") }
}
