package com.example.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.*
import com.example.ui.components.MatchStatusBadge
import com.example.ui.components.TournamentStatusBadge
import com.example.ui.components.borderStroke
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    currentUser: UserProfile,
    tournaments: List<Tournament>,
    matches: List<MatchFixture>,
    onSelectTournament: (Tournament) -> Unit,
    onNavigateTournaments: () -> Unit,
    onNavigateMatches: () -> Unit,
    onNavigateTeams: () -> Unit,
    onNavigateLeaderboard: () -> Unit,
    onNavigateProfile: () -> Unit,
    onMatchClick: (MatchFixture) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Championship Banner (Sharp Rectangular, Flat Overlay)
        item {
            HeroBanner(
                currentUser = currentUser,
                onExploreClick = onNavigateTournaments,
                onWalletClick = onNavigateProfile
            )
        }

        // Quick Action Grid (Sharp Rectangles)
        item {
            QuickActionsRow(
                onJoinTournaments = onNavigateTournaments,
                onMyMatches = onNavigateMatches,
                onTeamClash = onNavigateTeams,
                onLeaderboard = onNavigateLeaderboard
            )
        }

        // Live & Upcoming Matches
        val activeMatches = matches.filter { it.status == MatchStatus.LIVE || it.status == MatchStatus.ROOM_READY || it.status == MatchStatus.SCHEDULED }.take(4)
        if (activeMatches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(EsportsRed, RectangleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE & UPCOMING MATCHES",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    TextButton(onClick = onNavigateMatches, shape = RectangleShape) {
                        Text("View All (${matches.size})", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(activeMatches) { match ->
                HomeMatchItem(match = match, onClick = { onMatchClick(match) })
            }
        }

        // Featured Active Tournaments Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FEATURED TOURNAMENTS",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
                TextButton(onClick = onNavigateTournaments, shape = RectangleShape) {
                    Text("Explore All", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Featured Tournaments Cards
        val featuredTournaments = tournaments.take(4)
        items(featuredTournaments) { tour ->
            HomeTournamentCard(
                tournament = tour,
                onClick = { onSelectTournament(tour) }
            )
        }

        // Champion Hall of Fame
        val completedTournaments = tournaments.filter { it.status == TournamentStatus.COMPLETED }
        if (completedTournaments.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = borderStroke(1.dp, EsportsGold),
                    shape = RectangleShape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF382A05), RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Trophy",
                                tint = EsportsGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RECENT CHAMPION",
                                color = EsportsGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            val champ = completedTournaments.first()
                            Text(
                                text = champ.championName ?: "BD Tigers",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Winner of ${champ.title} (৳${champ.firstPrize.toInt()})",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroBanner(
    currentUser: UserProfile,
    onExploreClick: () -> Unit,
    onWalletClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_banner_card"),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = borderStroke(1.dp, DarkBorder)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.esports_banner),
                contentDescription = "Championship",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentScale = ContentScale.Crop
            )

            // Flat Solid Dark Overlay (NO Gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(DarkBg.copy(alpha = 0.85f), RectangleShape)
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = KheloGreenContainer,
                        shape = RectangleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = KheloGreenBright,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BANGLADESH PRO LEAGUE",
                                color = KheloGreenBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Text(
                        text = "Player: ${currentUser.username}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "COMPETE. WIN. EARN.",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Daily tournaments for Free Fire, PUBG Mobile & eFootball",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                        shape = RectangleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Join Tournaments", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onWalletClick,
                        shape = RectangleShape,
                        border = borderStroke(1.dp, KheloGreenBright),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deposit (bKash/Nagad)", color = KheloGreenBright, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onJoinTournaments: () -> Unit,
    onMyMatches: () -> Unit,
    onTeamClash: () -> Unit,
    onLeaderboard: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        QuickActionButton(
            title = "Tournaments",
            subtitle = "Active Cups",
            icon = Icons.Default.EmojiEvents,
            color = EsportsGold,
            onClick = onJoinTournaments,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            title = "My Matches",
            subtitle = "Rooms",
            icon = Icons.Default.Gamepad,
            color = KheloGreenBright,
            onClick = onMyMatches,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            title = "Squads",
            subtitle = "Clash",
            icon = Icons.Default.Groups,
            color = EsportsCyan,
            onClick = onTeamClash,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            title = "Rankings",
            subtitle = "Top BD",
            icon = Icons.Default.Leaderboard,
            color = EsportsOrange,
            onClick = onLeaderboard,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RectangleShape,
        color = DarkSurfaceCard,
        border = borderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(DarkSurfaceElevated, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HomeMatchItem(
    match: MatchFixture,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("match_item_${match.id}"),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = borderStroke(1.dp, if (match.status == MatchStatus.ROOM_READY) KheloGreen else DarkBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = match.game.uppercase(),
                        color = KheloGreenBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = " • ${match.tournamentTitle}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                }
                MatchStatusBadge(status = match.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = match.participantAName,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RectangleShape,
                    color = DarkSurfaceCard,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = if (match.scoreA != null && match.scoreB != null) "${match.scoreA} : ${match.scoreB}" else "VS",
                        color = if (match.scoreA != null) EsportsGold else TextMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = match.participantBName,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = match.scheduledTime,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                if (match.status == MatchStatus.ROOM_READY) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Room Ready — Tap to view", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeTournamentCard(
    tournament: Tournament,
    onClick: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(tournament.id, tournament.startAtMillis) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }

    val startAt = tournament.startAtMillis
    val diff = if (startAt > 0L) startAt - now else 0L
    val fiveMinutes = 5 * 60 * 1000L
    val isFinished = tournament.status == TournamentStatus.COMPLETED ||
        tournament.status == TournamentStatus.CANCELLED
    val isLive = tournament.status == TournamentStatus.GROUP_STAGE ||
        tournament.status == TournamentStatus.KNOCKOUT ||
        tournament.status == TournamentStatus.FINAL

    val statusLabel = when {
        tournament.status == TournamentStatus.CANCELLED -> "CANCELLED"
        tournament.status == TournamentStatus.COMPLETED -> "FINISHED"
        isLive -> "LIVE"
        tournament.status == TournamentStatus.REGISTRATION_CLOSED -> "CLOSED"
        startAt > 0L && diff in 1..fiveMinutes && tournament.status == TournamentStatus.REGISTRATION -> "STARTING SOON"
        startAt > 0L && diff <= 0L && tournament.status == TournamentStatus.REGISTRATION -> "LIVE"
        else -> "OPEN"
    }

    val statusColor = when (statusLabel) {
        "OPEN" -> KheloGreen
        "STARTING SOON", "CLOSED" -> EsportsOrange
        "LIVE" -> EsportsCyan
        "FINISHED", "CANCELLED" -> EsportsRed
        else -> TextSecondary
    }

    val canJoin = statusLabel == "OPEN" &&
        tournament.registeredCount < tournament.maxParticipants

    val progress = (
        tournament.registeredCount.toFloat() /
            tournament.maxParticipants.coerceAtLeast(1)
        ).coerceIn(0f, 1f)

    val totalSeconds = diff.coerceAtLeast(0L) / 1000L
    val days = totalSeconds / 86400L
    val hours = (totalSeconds % 86400L) / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    val countdownText = when {
        startAt <= 0L -> "সময় সেট করা হয়নি"
        diff <= 0L -> "MATCH TIME"
        days > 0L -> "%02dd %02dh %02dm %02ds".format(days, hours, minutes, seconds)
        else -> "%02dh %02dm %02ds".format(hours, minutes, seconds)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tournament_card_" + tournament.id),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = borderStroke(2.dp, statusColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RectangleShape,
                    color = DarkSurfaceCard,
                    border = borderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    if (tournament.gameLogoUrl.isNotBlank()) {
                        AsyncImage(
                            model = tournament.gameLogoUrl,
                            contentDescription = tournament.game,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.SportsEsports,
                                contentDescription = tournament.game,
                                tint = KheloGreen,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tournament.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        tournament.game,
                        color = KheloGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Text(
                        "MODE: " + tournament.gameMode + "  •  MAP: " + tournament.mapName,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    border = borderStroke(1.dp, statusColor),
                    shape = RectangleShape
                ) {
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = when (statusLabel) {
                    "OPEN" -> Color(0xFFEAF7ED)
                    "STARTING SOON" -> Color(0xFFFFF3DE)
                    "LIVE" -> Color(0xFFE8F6FA)
                    else -> Color(0xFFF8EAEA)
                },
                border = borderStroke(1.dp, statusColor),
                shape = RectangleShape
            ) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (statusLabel == "LIVE") "MATCH IS LIVE" else "STARTS IN",
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                    Text(
                        countdownText,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                    Text(
                        tournament.startDate + " • " + tournament.startTime,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TournamentMetric("PRIZE POOL", "৳" + tournament.prizePool.toInt(), EsportsGold, Modifier.weight(1f))
                TournamentMetric(
                    "ENTRY FEE",
                    if (tournament.entryFee == 0.0) "FREE" else "৳" + tournament.entryFee.toInt(),
                    KheloGreen,
                    Modifier.weight(1f)
                )
                TournamentMetric(
                    "PER KILL",
                    if (tournament.perKillReward <= 0.0) "৳0" else "৳" + tournament.perKillReward.toInt(),
                    EsportsCyan,
                    Modifier.weight(1f)
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = statusColor,
                trackColor = DarkBorder
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    tournament.registeredCount.toString() + "/" +
                        tournament.maxParticipants + " PLAYERS",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
                Text(
                    (progress * 100).toInt().toString() + "% FILLED",
                    color = statusColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { if (canJoin) onClick() },
                enabled = canJoin,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = statusColor,
                    disabledContainerColor = statusColor.copy(alpha = 0.45f),
                    disabledContentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text(
                    when {
                        canJoin -> "JOIN NOW"
                        statusLabel == "STARTING SOON" -> "STARTING SOON"
                        statusLabel == "LIVE" -> "LIVE NOW"
                        statusLabel == "CLOSED" -> "JOIN CLOSED"
                        statusLabel == "FINISHED" -> "FINISHED"
                        else -> "CANCELLED"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
            }
        }
    }
}

@Composable
private fun TournamentMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = DarkSurfaceCard,
        border = borderStroke(1.dp, DarkBorder),
        shape = RectangleShape
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 17.sp)
        }
    }
}
