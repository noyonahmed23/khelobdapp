package com.example.data.firebase

import android.util.Log
import com.example.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firestore", e)
            null
        }
    }

    val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FirebaseAuth", e)
            null
        }
    }

    // Active snapshot listeners
    private val activeListeners = mutableListOf<ListenerRegistration>()

    fun startRealtimeSync(
        onTournamentsUpdate: (List<Tournament>) -> Unit,
        onMatchesUpdate: (List<MatchFixture>) -> Unit,
        onRegistrationsUpdate: (List<TournamentRegistration>) -> Unit,
        onStandingsUpdate: (List<GroupStanding>) -> Unit,
        onTeamsUpdate: (List<Team>) -> Unit,
        onChallengesUpdate: (List<TeamChallenge>) -> Unit,
        onPaymentsUpdate: (List<PaymentTransaction>) -> Unit,
        onActivityLogsUpdate: (List<AdminActivityLog>) -> Unit,
        onSettingsUpdate: (SystemSettings) -> Unit,
        onUserChallengesUpdate: (List<UserChallenge>) -> Unit = {},
        onWelcomePopupUpdate: (WelcomePopupConfig) -> Unit = {},
        onUsersUpdate: (List<UserProfile>) -> Unit = {},
        onNotificationsUpdate: (List<NotificationItem>) -> Unit = {}
    ) {
        val db = firestore ?: return

        try {
            // Tournaments Listener
            val tourListener = db.collection("tournaments").addSnapshotListener { snapshot, error ->
                if (error != null) { Log.w(TAG, "Tournaments listener error", error); return@addSnapshotListener }
                if (snapshot != null) onTournamentsUpdate(snapshot.documents.mapNotNull { parseTournament(it) })
            }
            activeListeners.add(tourListener)

            // Matches Listener
            val matchesListener = db.collection("matches").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) onMatchesUpdate(snapshot.documents.mapNotNull { parseMatch(it) })
            }
            activeListeners.add(matchesListener)

            // Registrations Listener
            val regListener = db.collection("registrations").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) onRegistrationsUpdate(snapshot.documents.mapNotNull { parseRegistration(it) })
            }
            activeListeners.add(regListener)

            // Standings Listener
            val standListener = db.collection("standings").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) onStandingsUpdate(snapshot.documents.mapNotNull { parseStanding(it) })
            }
            activeListeners.add(standListener)

            // Teams Listener
            val teamsListener = db.collection("teams").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) onTeamsUpdate(snapshot.documents.mapNotNull { parseTeam(it) })
            }
            activeListeners.add(teamsListener)

            // Team Challenges Listener
            val chalListener = db.collection("challenges").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) onChallengesUpdate(snapshot.documents.mapNotNull { parseChallenge(it) })
            }
            activeListeners.add(chalListener)

            // Payments Listener
            val payListener = db.collection("payments").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) onPaymentsUpdate(snapshot.documents.mapNotNull { parsePayment(it) })
            }
            activeListeners.add(payListener)

            // Activity Logs Listener
            val logsListener = db.collection("activity_logs").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) onActivityLogsUpdate(snapshot.documents.mapNotNull { parseLog(it) })
            }
            activeListeners.add(logsListener)

            // System Settings Listener
            val settingsListener = db.collection("settings").document("system_config").addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val s = SystemSettings(
                        bkashNumber = doc.getString("bkashNumber") ?: "01700123456 (Personal)",
                        nagadNumber = doc.getString("nagadNumber") ?: "01900654321 (Personal)",
                        referralBonus = doc.getDouble("referralBonus") ?: 50.0,
                        minWithdrawal = doc.getDouble("minWithdrawal") ?: 100.0,
                        maintenanceMode = doc.getBoolean("maintenanceMode") ?: false
                    )
                    onSettingsUpdate(s)
                }
            }
            activeListeners.add(settingsListener)

            // Notifications Listener
            val notificationListener = db.collection("notifications").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) onNotificationsUpdate(snapshot.documents.mapNotNull { parseNotification(it) })
            }
            activeListeners.add(notificationListener)

            // User Challenges (1v1) Listener
            val userChalListener = db.collection("user_challenges").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) onUserChallengesUpdate(snapshot.documents.mapNotNull { parseUserChallenge(it) })
            }
            activeListeners.add(userChalListener)

            // Welcome Popup Config Listener
            val popupListener = db.collection("settings").document("welcome_popup").addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val config = WelcomePopupConfig(
                        isVisible = doc.getBoolean("isVisible") ?: false,
                        title = doc.getString("title") ?: "Welcome to Khelo BD!",
                        message = doc.getString("message") ?: "Bangladesh's #1 Esports Tournament Platform.",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        buttonText = doc.getString("buttonText") ?: "Let's Play!"
                    )
                    onWelcomePopupUpdate(config)
                }
            }
            activeListeners.add(popupListener)

            // All Users Listener
            val usersListener = db.collection("users").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) onUsersUpdate(snapshot.documents.mapNotNull { parseUser(it) })
            }
            activeListeners.add(usersListener)

        } catch (e: Exception) {
            Log.e(TAG, "Error attaching Firestore listeners: ${e.message}")
        }
    }

    // ==================== Parsers ====================

    fun parseUser(doc: DocumentSnapshot): UserProfile? {
        val uid = doc.getString("uid") ?: doc.id
        val email = doc.getString("email") ?: return null
        val username = doc.getString("username") ?: ""
        val fullName = doc.getString("fullName") ?: username
        val roleStr = doc.getString("role") ?: "USER"
        val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.USER }
        val preferredGame = doc.getString("preferredGame") ?: "Free Fire"
        val inGameUid = doc.getString("inGameUid") ?: ""
        val teamId = doc.getString("teamId").takeIf { it?.isNotBlank() == true }
        val teamName = doc.getString("teamName").takeIf { it?.isNotBlank() == true }
        val matchesPlayed = (doc.getLong("matchesPlayed") ?: 0L).toInt()
        val wins = (doc.getLong("wins") ?: 0L).toInt()
        val losses = (doc.getLong("losses") ?: 0L).toInt()
        val draws = (doc.getLong("draws") ?: 0L).toInt()
        val points = (doc.getLong("points") ?: 0L).toInt()
        val tournamentWins = (doc.getLong("tournamentWins") ?: 0L).toInt()
        val walletBalance = doc.getDouble("walletBalance") ?: 0.0
        val referralCode = doc.getString("referralCode") ?: "KHELO-USER"
        val isBanned = doc.getBoolean("isBanned") ?: false
        val profilePictureUrl = doc.getString("profilePictureUrl") ?: ""
        val profileBannerUrl = doc.getString("profileBannerUrl") ?: ""
        return UserProfile(
            uid = uid,
            email = email,
            username = username,
            fullName = fullName,
            role = role,
            preferredGame = preferredGame,
            inGameUid = inGameUid,
            teamId = teamId,
            teamName = teamName,
            matchesPlayed = matchesPlayed,
            wins = wins,
            losses = losses,
            draws = draws,
            points = points,
            tournamentWins = tournamentWins,
            walletBalance = walletBalance,
            referralCode = referralCode,
            isBanned = isBanned,
            profilePictureUrl = profilePictureUrl,
            profileBannerUrl = profileBannerUrl
        )
    }

    private fun parseTournament(doc: DocumentSnapshot): Tournament? {
        val id = doc.getString("id") ?: doc.id
        val title = doc.getString("title") ?: return null
        val game = doc.getString("game") ?: "Free Fire"
        val desc = doc.getString("description") ?: ""
        val entryFee = doc.getDouble("entryFee") ?: 0.0
        val prizePool = doc.getDouble("prizePool") ?: 0.0
        val maxSlots = (doc.getLong("maxParticipants") ?: 16L).toInt()
        val regCount = (doc.getLong("registeredCount") ?: 0L).toInt()
        val statusStr = doc.getString("status") ?: "REGISTRATION"
        val status = try { TournamentStatus.valueOf(statusStr) } catch (_: Exception) { TournamentStatus.REGISTRATION }
        val gameLogoUrl = doc.getString("gameLogoUrl") ?: ""
        val gameMode = doc.getString("gameMode") ?: "Squad"
        val mapName = doc.getString("mapName") ?: "Bermuda"
        val perKillReward = doc.getDouble("perKillReward") ?: 0.0
        val startAtMillis = doc.getLong("startAtMillis") ?: 0L
        val registrationDeadlineAtMillis = doc.getLong("registrationDeadlineAtMillis") ?: 0L
        val numGroups = (doc.getLong("numGroups") ?: 2L).toInt()
        val p1 = doc.getDouble("firstPrize") ?: (prizePool * 0.6)
        val p2 = doc.getDouble("secondPrize") ?: (prizePool * 0.3)
        val p3 = doc.getDouble("thirdPrize") ?: (prizePool * 0.1)
        val champ = doc.getString("championName")
        val roomId = doc.getString("roomId").takeIf { it?.isNotBlank() == true }
        val roomPassword = doc.getString("roomPassword").takeIf { it?.isNotBlank() == true }
        val roomVisible = doc.getBoolean("roomVisible") ?: false
        return Tournament(
            id = id, title = title, game = game, gameLogoUrl = gameLogoUrl, gameMode = gameMode,
            mapName = mapName, perKillReward = perKillReward,
            startAtMillis = startAtMillis, registrationDeadlineAtMillis = registrationDeadlineAtMillis,
            description = desc, entryFee = entryFee, prizePool = prizePool, maxParticipants = maxSlots,
            registeredCount = regCount, numGroups = numGroups, status = status,
            firstPrize = p1, secondPrize = p2, thirdPrize = p3, championName = champ,
            roomId = roomId, roomPassword = roomPassword, roomVisible = roomVisible
        )
    }

    private fun parseMatch(doc: DocumentSnapshot): MatchFixture? {
        val id = doc.getString("id") ?: doc.id
        val tId = doc.getString("tournamentId") ?: return null
        val tTitle = doc.getString("tournamentTitle") ?: ""
        val game = doc.getString("game") ?: "Free Fire"
        val gName = doc.getString("groupName")
        val round = doc.getString("round") ?: "Match"
        val pAId = doc.getString("participantAId") ?: ""
        val pAName = doc.getString("participantAName") ?: ""
        val pBId = doc.getString("participantBId") ?: ""
        val pBName = doc.getString("participantBName") ?: ""
        val time = doc.getString("scheduledTime") ?: "Scheduled"
        val statusStr = doc.getString("status") ?: "SCHEDULED"
        val status = try { MatchStatus.valueOf(statusStr) } catch (_: Exception) { MatchStatus.SCHEDULED }
        val roomId = doc.getString("roomId")
        val roomPass = doc.getString("roomPassword")
        val sA = doc.getLong("scoreA")?.toInt()
        val sB = doc.getLong("scoreB")?.toInt()
        val wId = doc.getString("winnerId")
        val wName = doc.getString("winnerName")
        val isKo = doc.getBoolean("isKnockout") ?: false
        return MatchFixture(
            id = id, tournamentId = tId, tournamentTitle = tTitle, game = game,
            groupName = if (gName.isNullOrBlank()) null else gName, round = round,
            participantAId = pAId, participantAName = pAName,
            participantBId = pBId, participantBName = pBName,
            scheduledTime = time, status = status, roomId = roomId, roomPassword = roomPass,
            scoreA = sA, scoreB = sB, winnerId = wId, winnerName = wName, isKnockout = isKo
        )
    }

    private fun parseRegistration(doc: DocumentSnapshot): TournamentRegistration? {
        val id = doc.getString("id") ?: doc.id
        val tId = doc.getString("tournamentId") ?: return null
        val tTitle = doc.getString("tournamentTitle") ?: ""
        val uId = doc.getString("userId") ?: return null
        val uName = doc.getString("userName") ?: ""
        val gUid = doc.getString("gameUid") ?: ""
        val method = doc.getString("paymentMethod") ?: "Wallet"
        val trxId = doc.getString("transactionId") ?: ""
        val amt = doc.getDouble("amount") ?: 0.0
        return TournamentRegistration(
            id = id, tournamentId = tId, tournamentTitle = tTitle,
            userId = uId, userName = uName, gameUid = gUid,
            paymentMethod = method, transactionId = trxId, amount = amt
        )
    }

    private fun parseStanding(doc: DocumentSnapshot): GroupStanding? {
        val id = doc.getString("id") ?: doc.id
        val tId = doc.getString("tournamentId") ?: return null
        val gName = doc.getString("groupName") ?: "Group A"
        val pId = doc.getString("participantId") ?: ""
        val pName = doc.getString("participantName") ?: ""
        val played = (doc.getLong("played") ?: 0L).toInt()
        val won = (doc.getLong("won") ?: 0L).toInt()
        val drawn = (doc.getLong("drawn") ?: 0L).toInt()
        val lost = (doc.getLong("lost") ?: 0L).toInt()
        val pts = (doc.getLong("points") ?: 0L).toInt()
        val isQ = doc.getBoolean("isQualified") ?: false
        return GroupStanding(
            id = id, tournamentId = tId, groupName = gName,
            participantId = pId, participantName = pName,
            played = played, won = won, drawn = drawn, lost = lost, points = pts, isQualified = isQ
        )
    }

    private fun parseTeam(doc: DocumentSnapshot): Team? {
        val id = doc.getString("id") ?: doc.id
        val name = doc.getString("name") ?: return null
        val tag = doc.getString("tag") ?: "TAG"
        val capId = doc.getString("captainId") ?: ""
        val capName = doc.getString("captainName") ?: ""
        val matches = (doc.getLong("matches") ?: 0L).toInt()
        val wins = (doc.getLong("wins") ?: 0L).toInt()
        val losses = (doc.getLong("losses") ?: 0L).toInt()
        val points = (doc.getLong("points") ?: 0L).toInt()
        val bannerUrl = doc.getString("bannerUrl") ?: ""
        val profileImageUrl = doc.getString("profileImageUrl") ?: ""
        @Suppress("UNCHECKED_CAST")
        val joinRequests = (doc.get("joinRequests") as? List<String>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val membersRaw = (doc.get("members") as? List<Map<String, Any>>) ?: emptyList()
        val members = membersRaw.map { m ->
            TeamMember(
                userId = (m["userId"] as? String) ?: "",
                username = (m["username"] as? String) ?: "",
                gameUid = (m["gameUid"] as? String) ?: "",
                isCaptain = (m["isCaptain"] as? Boolean) ?: false
            )
        }.filter { it.userId.isNotBlank() }
        return Team(
            id = id, name = name, tag = tag, captainId = capId, captainName = capName,
            matches = matches, wins = wins, losses = losses, points = points,
            bannerUrl = bannerUrl, profileImageUrl = profileImageUrl, joinRequests = joinRequests,
            isLive = doc.getBoolean("isLive") ?: true,
            lastActiveAt = doc.getLong("lastActiveAt") ?: System.currentTimeMillis(),
            members = members
        )
    }

    private fun parseChallenge(doc: DocumentSnapshot): TeamChallenge? {
        val id = doc.getString("id") ?: doc.id
        val cAId = doc.getString("challengerTeamId") ?: return null
        val cAName = doc.getString("challengerTeamName") ?: ""
        val cBId = doc.getString("challengedTeamId") ?: ""
        val cBName = doc.getString("challengedTeamName") ?: ""
        val game = doc.getString("game") ?: "Free Fire"
        val stake = doc.getDouble("stakeAmount") ?: 0.0
        val statusStr = doc.getString("status") ?: "PENDING"
        val status = try { ChallengeStatus.valueOf(statusStr) } catch (_: Exception) { ChallengeStatus.PENDING }
        val roomId = doc.getString("roomId").takeIf { it?.isNotBlank() == true }
        val roomPassword = doc.getString("roomPassword").takeIf { it?.isNotBlank() == true }
        val acceptedAtMillis = doc.getLong("acceptedAtMillis") ?: 0L
        val roomSetAtMillis = doc.getLong("roomSetAtMillis") ?: 0L
        val proofOpenAtMillis = doc.getLong("proofOpenAtMillis") ?: 0L
        val deadlineAtMillis = doc.getLong("deadlineAtMillis") ?: 0L
        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        val challengerProofUrl = doc.getString("challengerProofUrl").takeIf { it?.isNotBlank() == true }
        val challengedProofUrl = doc.getString("challengedProofUrl").takeIf { it?.isNotBlank() == true }
        val winnerTeamId = doc.getString("winnerTeamId").takeIf { it?.isNotBlank() == true }
        return TeamChallenge(
            id = id, challengerTeamId = cAId, challengerTeamName = cAName,
            challengedTeamId = cBId, challengedTeamName = cBName,
            game = game, stakeAmount = stake, status = status,
            timestamp = timestamp,
            acceptedAtMillis = acceptedAtMillis, roomSetAtMillis = roomSetAtMillis,
            proofOpenAtMillis = proofOpenAtMillis, deadlineAtMillis = deadlineAtMillis,
            challengerProofUrl = challengerProofUrl, challengedProofUrl = challengedProofUrl,
            roomId = roomId, roomPassword = roomPassword, winnerTeamId = winnerTeamId
        )
    }

    private fun parsePayment(doc: DocumentSnapshot): PaymentTransaction? {
        val id = doc.getString("id") ?: doc.id
        val uId = doc.getString("userId") ?: return null
        val uName = doc.getString("userName") ?: ""
        val amt = doc.getDouble("amount") ?: 0.0
        val method = doc.getString("method") ?: "bKash"
        val senderNum = doc.getString("senderNumber") ?: ""
        val trxId = doc.getString("transactionId") ?: ""
        val type = doc.getString("type") ?: "WALLET_TOPUP"
        val statusStr = doc.getString("status") ?: "PENDING"
        val status = try { PaymentStatus.valueOf(statusStr) } catch (_: Exception) { PaymentStatus.PENDING }
        val time = doc.getLong("timestamp") ?: System.currentTimeMillis()
        val note = doc.getString("note") ?: ""
        return PaymentTransaction(
            id = id, userId = uId, userName = uName, amount = amt,
            method = method, senderNumber = senderNum, transactionId = trxId,
            type = type, status = status, timestamp = time, note = note
        )
    }

    private fun parseLog(doc: DocumentSnapshot): AdminActivityLog? {
        val id = doc.getString("id") ?: doc.id
        val aId = doc.getString("adminId") ?: ""
        val aName = doc.getString("adminName") ?: ""
        val action = doc.getString("action") ?: ""
        val tType = doc.getString("targetType") ?: ""
        val tId = doc.getString("targetId") ?: ""
        val details = doc.getString("details") ?: ""
        val time = doc.getLong("timestamp") ?: System.currentTimeMillis()
        return AdminActivityLog(
            id = id, adminId = aId, adminName = aName, action = action,
            targetType = tType, targetId = tId, details = details, timestamp = time
        )
    }

    private fun parseUserChallenge(doc: DocumentSnapshot): UserChallenge? {
        val id = doc.getString("id") ?: doc.id
        val challengerUid = doc.getString("challengerUid") ?: return null
        val challengerName = doc.getString("challengerName") ?: ""
        val opponentUid = doc.getString("opponentUid") ?: return null
        val opponentName = doc.getString("opponentName") ?: ""
        val game = doc.getString("game") ?: "Free Fire"
        val stakeAmount = doc.getDouble("stakeAmount") ?: 50.0
        val statusStr = doc.getString("status") ?: "PENDING"
        val status = try { UserChallengeStatus.valueOf(statusStr) } catch (_: Exception) { UserChallengeStatus.PENDING }
        val roomId = doc.getString("roomId").takeIf { it?.isNotBlank() == true }
        val roomPassword = doc.getString("roomPassword").takeIf { it?.isNotBlank() == true }
        val challengerProofUrl = doc.getString("challengerProofUrl").takeIf { it?.isNotBlank() == true }
        val opponentProofUrl = doc.getString("opponentProofUrl").takeIf { it?.isNotBlank() == true }
        val winnerUid = doc.getString("winnerUid").takeIf { it?.isNotBlank() == true }
        val winnerName = doc.getString("winnerName").takeIf { it?.isNotBlank() == true }
        val acceptedAtMillis = doc.getLong("acceptedAtMillis") ?: 0L
        val roomSetAtMillis = doc.getLong("roomSetAtMillis") ?: 0L
        val proofOpenAtMillis = doc.getLong("proofOpenAtMillis") ?: 0L
        val deadlineAtMillis = doc.getLong("deadlineAtMillis") ?: 0L
        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        return UserChallenge(
            id = id, challengerUid = challengerUid, challengerName = challengerName,
            opponentUid = opponentUid, opponentName = opponentName, game = game,
            stakeAmount = stakeAmount, status = status, roomId = roomId,
            roomPassword = roomPassword, challengerProofUrl = challengerProofUrl,
            opponentProofUrl = opponentProofUrl, winnerUid = winnerUid,
            winnerName = winnerName, timestamp = timestamp
        )
    }

    // ==================== Write Methods ====================

    fun syncUserToFirestore(user: UserProfile) {
        scope.launch {
            try {
                firestore?.collection("users")?.document(user.uid)?.set(
                    mapOf(
                        "uid" to user.uid,
                        "email" to user.email,
                        "username" to user.username,
                        "fullName" to user.fullName,
                        "role" to user.role.name,
                        "preferredGame" to user.preferredGame,
                        "inGameUid" to user.inGameUid,
                        "teamId" to (user.teamId ?: ""),
                        "teamName" to (user.teamName ?: ""),
                        "walletBalance" to user.walletBalance,
                        "matchesPlayed" to user.matchesPlayed,
                        "wins" to user.wins,
                        "losses" to user.losses,
                        "draws" to user.draws,
                        "points" to user.points,
                        "tournamentWins" to user.tournamentWins,
                        "referralCode" to user.referralCode,
                        "isBanned" to user.isBanned,
                        "profilePictureUrl" to user.profilePictureUrl,
                        "profileBannerUrl" to user.profileBannerUrl
                    ),
                    SetOptions.merge()
                )?.await()
            } catch (e: Exception) {
                Log.w(TAG, "syncUserToFirestore error: ${e.message}")
            }
        }
    }

    fun syncTournamentToFirestore(tournament: Tournament) {
        scope.launch {
            try {
                firestore?.collection("tournaments")?.document(tournament.id)?.set(
                    mapOf(
                        "id" to tournament.id, "title" to tournament.title, "game" to tournament.game,
                        "gameLogoUrl" to tournament.gameLogoUrl, "gameMode" to tournament.gameMode,
                        "mapName" to tournament.mapName, "perKillReward" to tournament.perKillReward,
                        "startAtMillis" to tournament.startAtMillis,
                        "registrationDeadlineAtMillis" to tournament.registrationDeadlineAtMillis,
                        "description" to tournament.description, "entryFee" to tournament.entryFee,
                        "prizePool" to tournament.prizePool, "maxParticipants" to tournament.maxParticipants,
                        "registeredCount" to tournament.registeredCount, "status" to tournament.status.name,
                        "numGroups" to tournament.numGroups, "firstPrize" to tournament.firstPrize,
                        "secondPrize" to tournament.secondPrize, "thirdPrize" to tournament.thirdPrize,
                        "championName" to (tournament.championName ?: ""),
                        "roomId" to (tournament.roomId ?: ""),
                        "roomPassword" to (tournament.roomPassword ?: ""),
                        "roomVisible" to tournament.roomVisible
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncTournamentToFirestore error: ${e.message}") }
        }
    }

    fun syncMatchToFirestore(match: MatchFixture) {
        scope.launch {
            try {
                firestore?.collection("matches")?.document(match.id)?.set(
                    mapOf(
                        "id" to match.id, "tournamentId" to match.tournamentId,
                        "tournamentTitle" to match.tournamentTitle, "game" to match.game,
                        "groupName" to (match.groupName ?: ""), "round" to match.round,
                        "participantAId" to match.participantAId, "participantAName" to match.participantAName,
                        "participantBId" to match.participantBId, "participantBName" to match.participantBName,
                        "scheduledTime" to match.scheduledTime, "status" to match.status.name,
                        "roomId" to (match.roomId ?: ""), "roomPassword" to (match.roomPassword ?: ""),
                        "scoreA" to (match.scoreA ?: 0), "scoreB" to (match.scoreB ?: 0),
                        "winnerId" to (match.winnerId ?: ""), "winnerName" to (match.winnerName ?: ""),
                        "isKnockout" to match.isKnockout
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncMatchToFirestore error: ${e.message}") }
        }
    }

    fun syncRegistrationToFirestore(reg: TournamentRegistration) {
        scope.launch {
            try {
                firestore?.collection("registrations")?.document(reg.id)?.set(
                    mapOf(
                        "id" to reg.id, "tournamentId" to reg.tournamentId,
                        "tournamentTitle" to reg.tournamentTitle, "userId" to reg.userId,
                        "userName" to reg.userName, "gameUid" to reg.gameUid,
                        "paymentMethod" to reg.paymentMethod, "transactionId" to reg.transactionId,
                        "amount" to reg.amount
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncRegistrationToFirestore error: ${e.message}") }
        }
    }

    fun syncStandingToFirestore(standing: GroupStanding) {
        scope.launch {
            try {
                firestore?.collection("standings")?.document(standing.id)?.set(
                    mapOf(
                        "id" to standing.id, "tournamentId" to standing.tournamentId,
                        "groupName" to standing.groupName, "participantId" to standing.participantId,
                        "participantName" to standing.participantName, "played" to standing.played,
                        "won" to standing.won, "drawn" to standing.drawn, "lost" to standing.lost,
                        "points" to standing.points, "isQualified" to standing.isQualified
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncStandingToFirestore error: ${e.message}") }
        }
    }

    fun syncTeamToFirestore(team: Team) {
        scope.launch {
            try {
                firestore?.collection("teams")?.document(team.id)?.set(
                    mapOf(
                        "id" to team.id, "name" to team.name, "tag" to team.tag,
                        "captainId" to team.captainId, "captainName" to team.captainName,
                        "isLive" to team.isLive, "lastActiveAt" to team.lastActiveAt,
                        "matches" to team.matches, "wins" to team.wins,
                        "losses" to team.losses, "points" to team.points,
                        "bannerUrl" to team.bannerUrl, "profileImageUrl" to team.profileImageUrl,
                        "joinRequests" to team.joinRequests,
                        "members" to team.members.map { m ->
                            mapOf(
                                "userId" to m.userId,
                                "username" to m.username,
                                "gameUid" to m.gameUid,
                                "isCaptain" to m.isCaptain
                            )
                        }
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncTeamToFirestore error: ${e.message}") }
        }
    }

    fun syncChallengeToFirestore(challenge: TeamChallenge) {
        scope.launch {
            try {
                firestore?.collection("challenges")?.document(challenge.id)?.set(
                    mapOf(
                        "id" to challenge.id, "challengerTeamId" to challenge.challengerTeamId,
                        "challengerTeamName" to challenge.challengerTeamName,
                        "challengedTeamId" to challenge.challengedTeamId,
                        "challengedTeamName" to challenge.challengedTeamName,
                        "game" to challenge.game, "stakeAmount" to challenge.stakeAmount,
                        "status" to challenge.status.name,
                        "roomId" to (challenge.roomId ?: ""),
                        "roomPassword" to (challenge.roomPassword ?: ""),
                        "acceptedAtMillis" to challenge.acceptedAtMillis,
                        "roomSetAtMillis" to challenge.roomSetAtMillis,
                        "proofOpenAtMillis" to challenge.proofOpenAtMillis,
                        "deadlineAtMillis" to challenge.deadlineAtMillis,
                        "challengerProofUrl" to (challenge.challengerProofUrl ?: ""),
                        "challengedProofUrl" to (challenge.challengedProofUrl ?: ""),
                        "winnerTeamId" to (challenge.winnerTeamId ?: "")
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncChallengeToFirestore error: ${e.message}") }
        }
    }

    fun syncPaymentToFirestore(payment: PaymentTransaction) {
        scope.launch {
            try {
                firestore?.collection("payments")?.document(payment.id)?.set(
                    mapOf(
                        "id" to payment.id, "userId" to payment.userId, "userName" to payment.userName,
                        "amount" to payment.amount, "method" to payment.method,
                        "senderNumber" to payment.senderNumber, "transactionId" to payment.transactionId,
                        "type" to payment.type, "status" to payment.status.name,
                        "timestamp" to payment.timestamp, "note" to payment.note
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncPaymentToFirestore error: ${e.message}") }
        }
    }

    fun syncActivityLogToFirestore(log: AdminActivityLog) {
        scope.launch {
            try {
                firestore?.collection("activity_logs")?.document(log.id)?.set(
                    mapOf(
                        "id" to log.id, "adminId" to log.adminId, "adminName" to log.adminName,
                        "action" to log.action, "targetType" to log.targetType,
                        "targetId" to log.targetId, "details" to log.details, "timestamp" to log.timestamp
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncActivityLogToFirestore error: ${e.message}") }
        }
    }

    fun syncSettingsToFirestore(settings: SystemSettings) {
        scope.launch {
            try {
                firestore?.collection("settings")?.document("system_config")?.set(
                    mapOf(
                        "bkashNumber" to settings.bkashNumber, "nagadNumber" to settings.nagadNumber,
                        "referralBonus" to settings.referralBonus, "minWithdrawal" to settings.minWithdrawal,
                        "maintenanceMode" to settings.maintenanceMode
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncSettingsToFirestore error: ${e.message}") }
        }
    }

    fun parseNotification(doc: DocumentSnapshot): NotificationItem? {
        val userId = doc.getString("userId") ?: return null
        return NotificationItem(
            id = doc.getString("id") ?: doc.id,
            userId = userId,
            title = doc.getString("title") ?: "Notification",
            message = doc.getString("message") ?: "",
            type = doc.getString("type") ?: "INFO",
            isRead = doc.getBoolean("isRead") ?: false,
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        )
    }

    fun syncNotificationToFirestore(item: NotificationItem) {
        scope.launch {
            try {
                firestore?.collection("notifications")?.document(item.id)?.set(
                    mapOf(
                        "id" to item.id,
                        "userId" to item.userId,
                        "title" to item.title,
                        "message" to item.message,
                        "type" to item.type,
                        "isRead" to item.isRead,
                        "timestamp" to item.timestamp
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncNotificationToFirestore error") }
        }
    }

    fun updateNotificationReadState(notificationId: String, isRead: Boolean) {
        scope.launch {
            try {
                firestore?.collection("notifications")?.document(notificationId)?.update("isRead", isRead)?.await()
            } catch (e: Exception) { Log.w(TAG, "updateNotificationReadState error") }
        }
    }

    fun syncUserChallengeToFirestore(challenge: UserChallenge) {
        scope.launch {
            try {
                firestore?.collection("user_challenges")?.document(challenge.id)?.set(
                    mapOf(
                        "id" to challenge.id,
                        "challengerUid" to challenge.challengerUid,
                        "challengerName" to challenge.challengerName,
                        "opponentUid" to challenge.opponentUid,
                        "opponentName" to challenge.opponentName,
                        "game" to challenge.game,
                        "stakeAmount" to challenge.stakeAmount,
                        "status" to challenge.status.name,
                        "roomId" to (challenge.roomId ?: ""),
                        "roomPassword" to (challenge.roomPassword ?: ""),
                        "challengerProofUrl" to (challenge.challengerProofUrl ?: ""),
                        "opponentProofUrl" to (challenge.opponentProofUrl ?: ""),
                        "winnerUid" to (challenge.winnerUid ?: ""),
                        "winnerName" to (challenge.winnerName ?: ""),
                        "acceptedAtMillis" to challenge.acceptedAtMillis,
                        "roomSetAtMillis" to challenge.roomSetAtMillis,
                        "proofOpenAtMillis" to challenge.proofOpenAtMillis,
                        "deadlineAtMillis" to challenge.deadlineAtMillis,
                        "timestamp" to challenge.timestamp
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncUserChallengeToFirestore error: ${e.message}") }
        }
    }

    fun syncWelcomePopupToFirestore(config: WelcomePopupConfig) {
        scope.launch {
            try {
                firestore?.collection("settings")?.document("welcome_popup")?.set(
                    mapOf(
                        "isVisible" to config.isVisible,
                        "title" to config.title,
                        "message" to config.message,
                        "imageUrl" to config.imageUrl,
                        "buttonText" to config.buttonText
                    ), SetOptions.merge()
                )?.await()
            } catch (e: Exception) { Log.w(TAG, "syncWelcomePopupToFirestore error: ${e.message}") }
        }
    }

    fun removeActiveListeners() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
    }

    // ==================== Delete Helpers (Admin CRUD) ====================

    fun deleteUser(uid: String) {
        scope.launch {
            try { firestore?.collection("users")?.document(uid)?.delete()?.await() }
            catch (e: Exception) { Log.w(TAG, "deleteUser error: ${e.message}") }
        }
    }

    fun deleteTeam(teamId: String) {
        scope.launch {
            try { firestore?.collection("teams")?.document(teamId)?.delete()?.await() }
            catch (e: Exception) { Log.w(TAG, "deleteTeam error: ${e.message}") }
        }
    }

    fun deleteTournament(tournamentId: String) {
        scope.launch {
            try { firestore?.collection("tournaments")?.document(tournamentId)?.delete()?.await() }
            catch (e: Exception) { Log.w(TAG, "deleteTournament error: ${e.message}") }
        }
    }

    fun deleteUserChallenge(challengeId: String) {
        scope.launch {
            try { firestore?.collection("user_challenges")?.document(challengeId)?.delete()?.await() }
            catch (e: Exception) { Log.w(TAG, "deleteUserChallenge error: ${e.message}") }
        }
    }
}