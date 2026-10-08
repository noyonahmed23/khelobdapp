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
        // Hero Championship Banner
        item {
            HeroBanner(
                currentUser = currentUser,
                onExploreClick = onNavigateTournaments,
                onWalletClick = onNavigateProfile
            )
        }

        // Quick Action Grid
        item {
            QuickActionsRow(
                onJoinTournaments = onNavigateTournaments,
                onMyMatches = onNavigateMatches,
                onTeamClash = onNavigateTeams,
                onLeaderboard = onNavigateLeaderboard
            )
        }

        // ====== JOIN OPEN TOURNAMENTS (PRIMARY) ======
        val joinOpenTournaments = tournaments.filter { 
            it.status == TournamentStatus.REGISTRATION 
        }.sortedByDescending { it.createdAt }.take(6)
        
        if (joinOpenTournaments.isNotEmpty()) {
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
                                .background(KheloGreenBright, RectangleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "যোগদানের জন্য খোলা",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    TextButton(onClick = onNavigateTournaments, shape = RectangleShape) {
                        Text("সব দেখুন", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(joinOpenTournaments) { tournament ->
                HomeTournamentCard(
                    tournament = tournament,
                    onClick = { onSelectTournament(tournament) }
                )
            }
        }

        // ====== LIVE TOURNAMENTS ======
        val liveTournaments = tournaments.filter { 
            it.status == TournamentStatus.GROUP_STAGE || 
            it.status == TournamentStatus.KNOCKOUT ||
            it.status == TournamentStatus.FINAL
        }.sortedByDescending { it.createdAt }.take(3)

        if (liveTournaments.isNotEmpty()) {
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
                            text = "লাইভ টুর্নামেন্ট",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    TextButton(onClick = onNavigateTournaments, shape = RectangleShape) {
                        Text("সব দেখুন", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(liveTournaments) { tournament ->
                HomeTournamentCard(
                    tournament = tournament,
                    onClick = { onSelectTournament(tournament) }
                )
            }
        }

        // ====== LIVE & UPCOMING MATCHES ======
        val activeMatches = matches.filter { 
            it.status == MatchStatus.LIVE || 
            it.status == MatchStatus.ROOM_READY || 
            it.status == MatchStatus.SCHEDULED 
        }.take(4)
        
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
                            text = "লাইভ ও আসন্ন ম্যাচ",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                    TextButton(onClick = onNavigateMatches, shape = RectangleShape) {
                        Text("সব দেখুন (${matches.size})", color = KheloGreenBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(activeMatches) { match ->
                HomeMatchItem(match = match, onClick = { onMatchClick(match) })
            }
        }

        // ====== TOP PLAYERS ======
        item {
            Text(
                text = "শীর্ষ খেলোয়াড়",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        }

        // ====== TOP SQUADS / TEAMS ======
        item {
            Text(
                text = "শীর্ষ দল",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        }

        // ====== RECENT CHAMPION ======
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
                                text = "সম্প্রতি চ্যাম্পিয়ন",
                                color = EsportsGold,
                                fontSize = 9.sp,
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
                                text = "${champ.title} এর বিজয়ী (৳${champ.firstPrize.toInt()})",
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

            // Flat Solid Dark Overlay
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
                                text = "খেলো বাংলাদেশ",
                                color = KheloGreenBright,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Text(
                        text = "খেলোয়াড়: ${currentUser.username}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "প্রতিযোগিতা করুন। জিতুন। উপার্জন করুন।",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Free Fire, PUBG Mobile ও eFootball এ প্রতিদিনের টুর্নামেন্ট",
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
                        Text("টুর্নামেন্টে যোগ দিন", color = DarkBg, fontWeight = FontWeight.Black, fontSize = 11.sp)
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
                        Text("জমা করুন", color = KheloGreenBright, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
            title = "টুর্নামেন্ট",
            subtitle = "সক্রিয় কাপ",
            icon = Icons.Default.EmojiEvents,
            color = EsportsGold,
            onClick = onJoinTournaments,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            title = "আমার ম্যাচ",
            subtitle = "রুম",
            icon = Icons.Default.Gamepad,
            color = KheloGreenBright,
            onClick = onMyMatches,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            title = "দল",
            subtitle = "ক্লাশ",
            icon = Icons.Default.Groups,
            color = EsportsCyan,
            onClick = onTeamClash,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            title = "র‍্যাঙ্কিং",
            subtitle = "শীর্ষ বাংলা",
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
                fontSize = 8.sp,
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = " • ${match.tournamentTitle}",
                        color = TextSecondary,
                        fontSize = 10.sp,
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
                        fontSize = 10.sp
                    )
                }

                if (match.status == MatchStatus.ROOM_READY) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = KheloGreenBright, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("রুম প্রস্তুত", color = KheloGreenBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    val statusColor = when (tournament.status) {
        TournamentStatus.REGISTRATION -> KheloGreenBright
        TournamentStatus.GROUP_STAGE, TournamentStatus.KNOCKOUT, TournamentStatus.FINAL -> EsportsRed
        TournamentStatus.COMPLETED -> EsportsGold
        else -> TextMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tournament_card_${tournament.id}"),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = borderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF1F2B1D),
                    shape = RectangleShape
                ) {
                    Text(
                        text = tournament.game.uppercase(),
                        color = KheloGreenBright,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RectangleShape,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (tournament.status) {
                            TournamentStatus.REGISTRATION -> "যোগদানের জন্য খোলা"
                            TournamentStatus.GROUP_STAGE, TournamentStatus.KNOCKOUT, TournamentStatus.FINAL -> "লাইভ"
                            TournamentStatus.COMPLETED -> "সম্পন্ন"
                            else -> "অন্যান্য"
                        },
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tournament.title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Text(
                text = tournament.description,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceCard, RectangleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("পুরস্কার পুল", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = EsportsGold, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("৳${tournament.prizePool.toInt()}", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }

                Column {
                    Text("অংশগ্রহণের ফি", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (tournament.entryFee == 0.0) "বিনামূল্যে" else "৳${tournament.entryFee.toInt()}",
                        color = if (tournament.entryFee == 0.0) KheloGreenBright else TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Column {
                    Text("স্লট", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${tournament.registeredCount}/${tournament.maxParticipants}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
