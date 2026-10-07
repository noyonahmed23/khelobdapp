package com.example.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Team
import com.example.data.model.UserProfile
import com.example.ui.components.borderStroke
import com.example.ui.theme.*

@Composable
fun LeaderboardScreen(
    users: List<UserProfile>,
    teams: List<Team>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Top Players", "Top Squads")

    val sortedPlayers = users.sortedWith(compareByDescending<UserProfile> { it.points }.thenByDescending { it.wins })
    val sortedTeams = teams.sortedWith(compareByDescending<Team> { it.points }.thenByDescending { it.wins })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurfaceCard,
            contentColor = KheloGreenBright,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) KheloGreenBright else TextSecondary
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedTab == 0) {
                itemsIndexed(sortedPlayers) { index, player ->
                    PlayerRankCard(rank = index + 1, player = player)
                }
            } else {
                itemsIndexed(sortedTeams) { index, team ->
                    TeamRankCard(rank = index + 1, team = team)
                }
            }
        }
    }
}

@Composable
fun PlayerRankCard(rank: Int, player: UserProfile) {
    val rankBadgeColor = when (rank) {
        1 -> EsportsGold
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> DarkBorder
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = borderStroke(1.dp, if (rank <= 3) rankBadgeColor else DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(if (rank <= 3) rankBadgeColor.copy(alpha = 0.2f) else DarkSurfaceCard, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (rank == 1) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "1st", tint = EsportsGold, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            text = "$rank",
                            color = if (rank <= 3) rankBadgeColor else TextMuted,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = player.username,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${player.preferredGame} • ${player.teamName ?: "Free Agent"}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("WINS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${player.wins}", color = KheloGreenBright, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PTS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${player.points}", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun TeamRankCard(rank: Int, team: Team) {
    val rankBadgeColor = when (rank) {
        1 -> EsportsGold
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> DarkBorder
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = borderStroke(1.dp, if (rank <= 3) rankBadgeColor else DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(if (rank <= 3) rankBadgeColor.copy(alpha = 0.2f) else DarkSurfaceCard, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        color = if (rank <= 3) rankBadgeColor else TextMuted,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "${team.name} [${team.tag}]",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text("Captain: ${team.captainName}", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("WINRATE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${team.winRate.toInt()}%", color = KheloGreenBright, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PTS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${team.points}", color = EsportsGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}
