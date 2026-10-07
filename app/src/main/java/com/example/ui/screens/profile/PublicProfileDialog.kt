package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.ui.components.borderStroke
import com.example.ui.theme.*

@Composable
fun PublicProfileDialog(
    user: UserProfile,
    currentUserUid: String,
    allUsersRanking: List<UserProfile>, // used to compute overall rank
    onDismiss: () -> Unit,
    onChallengeClick: ((opponentUid: String, opponentName: String) -> Unit)? = null
) {
    val rank = allUsersRanking
        .sortedByDescending { it.points }
        .indexOfFirst { it.uid == user.uid }
        .let { if (it >= 0) it + 1 else null }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(KheloGreenContainer, RectangleShape)
                ) {
                    if (user.profileBannerUrl.isNotBlank()) {
                        AsyncImage(
                            model = user.profileBannerUrl,
                            contentDescription = "Profile Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    // Avatar overlapping banner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 14.dp)
                            .offset(y = 24.dp)
                            .size(48.dp)
                            .background(DarkSurface, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.profilePictureUrl.isNotBlank()) {
                            AsyncImage(
                                model = user.profilePictureUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                user.username.take(2).uppercase(),
                                color = KheloGreenBright,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                // Profile content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 32.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Name + username
                    Column {
                        Text(
                            user.fullName.ifBlank { user.username },
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            "@${user.username}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        if (user.inGameUid.isNotBlank()) {
                            Text(
                                "Game UID: ${user.inGameUid}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Game + Team badges
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = DarkSurfaceCard, shape = RectangleShape) {
                            Text(
                                user.preferredGame,
                                color = KheloGreenBright,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (user.teamName != null) {
                            Surface(color = DarkSurfaceCard, shape = RectangleShape) {
                                Text(
                                    user.teamName,
                                    color = EsportsCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Stats grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceCard, RectangleShape)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PublicStatItem("PLAYED", "${user.matchesPlayed}", TextPrimary)
                        PublicStatItem("WINS", "${user.wins}", KheloGreenBright)
                        PublicStatItem("LOSSES", "${user.losses}", EsportsRed)
                        PublicStatItem("POINTS", "${user.points}", EsportsCyan)
                        if (rank != null) {
                            PublicStatItem("RANK", "#$rank", EsportsGold)
                        }
                    }

                    // Challenge button (only for other users, not self)
                    if (onChallengeClick != null && user.uid != currentUserUid) {
                        Button(
                            onClick = { onChallengeClick(user.uid, user.username) },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("challenge_player_btn")
                        ) {
                            Icon(
                                Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = DarkBg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Challenge ${user.username}",
                                color = DarkBg,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublicStatItem(title: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}
