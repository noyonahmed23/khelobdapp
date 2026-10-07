package com.example.data.repository

import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

object TournamentRepository {

    // Authentication State
    private val _isAuthenticated = MutableStateFlow<Boolean>(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Default master admin credentials
    val ADMIN_USERNAME = "adminnoyon"
    val ADMIN_PASSWORD = "Noyon@1234"

    // Current Logged-in user
    private val _currentUser = MutableStateFlow<UserProfile>(
        UserProfile(
            uid = "admin_noyon_root",
            email = "adminnoyon@khelobd.com",
            username = "adminnoyon",
            fullName = "Noyon (Platform Admin)",
            role = UserRole.SUPER_ADMIN,
            walletBalance = 50000.0,
            referralCode = "KHELO-ADMIN"
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    // Real All Users from Database
    private val _users = MutableStateFlow<List<UserProfile>>(emptyList())
    val users: StateFlow<List<UserProfile>> = _users.asStateFlow()

    // Real Tournaments from Database
    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

    // Real Registrations from Database
    private val _registrations = MutableStateFlow<List<TournamentRegistration>>(emptyList())
    val registrations: StateFlow<List<TournamentRegistration>> = _registrations.asStateFlow()

    // Real Matches from Database
    private val _matches = MutableStateFlow<List<MatchFixture>>(emptyList())
    val matches: StateFlow<List<MatchFixture>> = _matches.asStateFlow()

    // Real Standings from Database
    private val _standings = MutableStateFlow<List<GroupStanding>>(emptyList())
    val standings: StateFlow<List<GroupStanding>> = _standings.asStateFlow()

    // Real Teams from Database
    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    // Real Challenges from Database
    private val _challenges = MutableStateFlow<List<TeamChallenge>>(emptyList())
    val challenges: StateFlow<List<TeamChallenge>> = _challenges.asStateFlow()

    // Real Payments from Database
    private val _payments = MutableStateFlow<List<PaymentTransaction>>(emptyList())
    val payments: StateFlow<List<PaymentTransaction>> = _payments.asStateFlow()

    // Real Notifications from Database
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // Real Activity Logs from Database
    private val _activityLogs = MutableStateFlow<List<AdminActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<AdminActivityLog>> = _activityLogs.asStateFlow()

    // Real System Settings from Database
    private val _settings = MutableStateFlow(SystemSettings())
    val settings: StateFlow<SystemSettings> = _settings.asStateFlow()

    // Real Background Worker / Cron Automation Status
    private val _workerStatus = MutableStateFlow(WorkerStatus())
    val workerStatus: StateFlow<WorkerStatus> = _workerStatus.asStateFlow()

    // Real 1v1 User Challenges from Database
    private val _userChallenges = MutableStateFlow<List<UserChallenge>>(emptyList())
    val userChallenges: StateFlow<List<UserChallenge>> = _userChallenges.asStateFlow()

    // Admin-controlled Welcome Popup config
    private val _welcomePopup = MutableStateFlow(WelcomePopupConfig())
    val welcomePopup: StateFlow<WelcomePopupConfig> = _welcomePopup.asStateFlow()

    // Global loading flag for smooth loading states across both apps
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 1v1 Challenge economics (fixed by product rules)
    const val CHALLENGE_STAKE = 50.0      // deducted from each player on acceptance
    const val CHALLENGE_WINNER_PAYOUT = 85.0 // credited to winner
    const val CHALLENGE_PLATFORM_FEE = 15.0  // retained by platform
    const val TEAM_WINNER_PAYOUT = 85.0

    init {
        SessionStore.restore()?.let { restored ->
            _currentUser.value = restored
            _isAuthenticated.value = true
        }

        // Master admin initialized in database
        val masterAdmin = _currentUser.value
        _users.value = listOf(masterAdmin)
        FirebaseManager.syncUserToFirestore(masterAdmin)

        // Realtime Firestore synchronization
        FirebaseManager.startRealtimeSync(
            onTournamentsUpdate = { _tournaments.value = it },
            onMatchesUpdate = { _matches.value = it },
            onRegistrationsUpdate = { _registrations.value = it },
            onStandingsUpdate = { _standings.value = it },
            onTeamsUpdate = { _teams.value = it },
            onChallengesUpdate = { _challenges.value = it },
            onPaymentsUpdate = { _payments.value = it },
            onActivityLogsUpdate = { _activityLogs.value = it },
            onSettingsUpdate = { _settings.value = it },
            onUserChallengesUpdate = { _userChallenges.value = it },
            onWelcomePopupUpdate = { _welcomePopup.value = it },
            onNotificationsUpdate = { dbNotifications -> _notifications.value = dbNotifications },
            onUsersUpdate = { dbUsers ->
                _users.value = dbUsers
                // Keep the logged-in profile fresh (wallet, stats, team, images) from DB
                dbUsers.find { it.uid == _currentUser.value.uid }?.let { fresh ->
                    _currentUser.value = fresh
                    if (_isAuthenticated.value) SessionStore.save(fresh)
                }
            }
        )

        // Launch real automated background Worker / Cron ticker (evaluates every 60s)
        CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(60_000)
                try {
                    runCronCycle()
                } catch (e: Exception) {
                    Log.e("TournamentCron", "Error running automation worker cycle", e)
                }
            }
        }
    }

    // Role Switch
    fun switchUserRole(role: UserRole) {
        _currentUser.value = _currentUser.value.copy(role = role)
        FirebaseManager.syncUserToFirestore(_currentUser.value)
        logAdminAction("ROLE_SWITCH", "User", _currentUser.value.uid, "Switched active role to $role")
    }

    // User Registration with Name, Email, Password, Confirm Password
    fun registerUser(name: String, email: String, password: String, confirmPassword: String): Result<UserProfile> {
        val cleanName = name.trim()
        val cleanEmail = email.trim()

        if (cleanName.isBlank()) {
            return Result.failure(Exception("Please enter your name."))
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(Exception("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(Exception("Password must be at least 6 characters."))
        }
        if (password != confirmPassword) {
            return Result.failure(Exception("Passwords do not match! Please check and try again."))
        }

        val existing = _users.value.find { it.email.equals(cleanEmail, ignoreCase = true) }
        if (existing != null) {
            return Result.failure(Exception("An account with this email already exists."))
        }

        val username = cleanEmail.substringBefore("@").replace(".", "_")
        val newUser = UserProfile(
            email = cleanEmail,
            username = username,
            fullName = cleanName,
            role = UserRole.USER,
            walletBalance = 0.0 // Default 0.0 balance as requested
        )

        _users.value = _users.value + newUser
        _currentUser.value = newUser
        _isAuthenticated.value = true
        SessionStore.save(newUser)
        FirebaseManager.syncUserToFirestore(newUser)
        logAdminAction("USER_REGISTERED", "User", newUser.uid, "New player registered: $cleanName ($cleanEmail)")
        return Result.success(newUser)
    }

    // Authentication (adminnoyon / Noyon@1234 or player)
    fun authenticate(usernameOrEmail: String, password: String): Result<UserProfile> {
        val clean = usernameOrEmail.trim()
        val isMasterAdmin = (clean.equals("adminnoyon", ignoreCase = true) ||
                clean.equals("adminnoyon@khelobd.com", ignoreCase = true) ||
                clean.equals("adminnoyon@gmail.com", ignoreCase = true)) &&
                password == "Noyon@1234"

        if (isMasterAdmin) {
            val adminProfile = UserProfile(
                uid = "admin_noyon_root",
                email = "adminnoyon@khelobd.com",
                username = "adminnoyon",
                fullName = "Noyon (Platform Admin)",
                role = UserRole.SUPER_ADMIN,
                walletBalance = 50000.0,
                referralCode = "KHELO-ADMIN"
            )
            _currentUser.value = adminProfile
            _isAuthenticated.value = true
            SessionStore.save(adminProfile)
            FirebaseManager.syncUserToFirestore(adminProfile)
            logAdminAction("ADMIN_LOGIN", "Auth", adminProfile.uid, "Admin Noyon authenticated successfully")
            return Result.success(adminProfile)
        }

        if (password.length < 4) {
            return Result.failure(Exception("Password must be at least 4 characters"))
        }

        // Mandatory authentication: an account must already exist. Guest/auto-login is disabled.
        val existing = _users.value.find { it.username.equals(clean, ignoreCase = true) || it.email.equals(clean, ignoreCase = true) }
            ?: return Result.failure(Exception("No account found for '$clean'. Please create an account first."))

        if (existing.isBanned) {
            return Result.failure(Exception("This account has been suspended. Contact support."))
        }

        _currentUser.value = existing
        _isAuthenticated.value = true
        SessionStore.save(existing)
        logAdminAction("USER_LOGIN", "Auth", existing.uid, "Player authenticated: ${existing.username}")
        return Result.success(existing)
    }

    fun logout() {
        _isAuthenticated.value = false
        SessionStore.clear()
    }

    fun updateProfile(fullName: String, gameUid: String, preferredGame: String) {
        val updated = _currentUser.value.copy(
            fullName = fullName,
            inGameUid = gameUid,
            preferredGame = preferredGame
        )
        _currentUser.value = updated
        if (_isAuthenticated.value) SessionStore.save(updated)
        _users.value = _users.value.map {
            if (it.uid == updated.uid) updated else it
        }
        FirebaseManager.syncUserToFirestore(updated)
    }

    /** Update the logged-in user's profile picture and/or banner (pass null to leave unchanged). */
    fun updateProfileImages(profilePictureUrl: String?, profileBannerUrl: String?) {
        val current = _currentUser.value
        val updated = current.copy(
            profilePictureUrl = profilePictureUrl ?: current.profilePictureUrl,
            profileBannerUrl = profileBannerUrl ?: current.profileBannerUrl
        )
        upsertUser(updated)
    }

    /** Persist a user to local state + Firestore, keeping currentUser aligned. */
    private fun upsertUser(user: UserProfile) {
        _users.value = if (_users.value.any { it.uid == user.uid }) {
            _users.value.map { if (it.uid == user.uid) user else it }
        } else {
            _users.value + user
        }
        if (_currentUser.value.uid == user.uid) {
            _currentUser.value = user
            if (_isAuthenticated.value) SessionStore.save(user)
        }
        FirebaseManager.syncUserToFirestore(user)
    }

    /**
     * Apply a wallet delta to a user by uid (local + Firestore).
     * Returns false if the user does not exist or the result would go negative.
     */
    private fun applyWalletChange(uid: String, delta: Double): Boolean {
        val target = _users.value.find { it.uid == uid } ?: _currentUser.value.takeIf { it.uid == uid }
            ?: return false
        val newBalance = target.walletBalance + delta
        if (newBalance < 0) return false
        upsertUser(target.copy(walletBalance = newBalance))
        return true
    }

    // Tournament Registration
    fun registerForTournament(tournament: Tournament, gameUid: String, paymentMethod: String, trxId: String): Result<String> {
        val user = _currentUser.value
        if (tournament.status != TournamentStatus.REGISTRATION) {
            return Result.failure(Exception("Registration is closed for this tournament."))
        }
        val isAlreadyRegistered = _registrations.value.any { it.tournamentId == tournament.id && it.userId == user.uid }
        if (isAlreadyRegistered) {
            return Result.failure(Exception("You have already registered for this tournament!"))
        }

        if (tournament.entryFee > 0) {
            if (paymentMethod == "Wallet") {
                if (user.walletBalance < tournament.entryFee) {
                    return Result.failure(Exception("Insufficient wallet balance (৳${user.walletBalance}). Please deposit via bKash or Nagad."))
                }
                _currentUser.value = user.copy(walletBalance = user.walletBalance - tournament.entryFee)
                FirebaseManager.syncUserToFirestore(_currentUser.value)
            } else {
                val payment = PaymentTransaction(
                    userId = user.uid,
                    userName = user.username,
                    amount = tournament.entryFee,
                    method = paymentMethod,
                    transactionId = trxId.ifBlank { "TRX-${UUID.randomUUID().toString().take(8).uppercase()}" },
                    type = "TOURNAMENT_ENTRY",
                    status = PaymentStatus.PENDING,
                    note = "Tournament fee for ${tournament.title}"
                )
                _payments.value = listOf(payment) + _payments.value
                FirebaseManager.syncPaymentToFirestore(payment)
            }
        }

        val reg = TournamentRegistration(
            tournamentId = tournament.id,
            tournamentTitle = tournament.title,
            userId = user.uid,
            userName = user.username,
            teamId = user.teamId,
            teamName = user.teamName,
            gameUid = gameUid.ifBlank { user.inGameUid },
            paymentMethod = paymentMethod,
            transactionId = trxId,
            amount = tournament.entryFee,
            status = RegistrationStatus.CONFIRMED
        )
        _registrations.value = _registrations.value + reg
        FirebaseManager.syncRegistrationToFirestore(reg)

        val updatedTour = tournament.copy(registeredCount = tournament.registeredCount + 1)
        _tournaments.value = _tournaments.value.map {
            if (it.id == tournament.id) updatedTour else it
        }
        FirebaseManager.syncTournamentToFirestore(updatedTour)

        addNotification(
            userId = user.uid,
            title = "Registration Confirmed!",
            message = "You are confirmed for ${tournament.title}. Check My Matches for room schedules.",
            type = "TOURNAMENT"
        )

        return Result.success("Registration confirmed successfully!")
    }

    // Automation Engine: Generate Groups & Fixtures
    fun automateGenerateGroupsAndFixtures(tournamentId: String) {
        val tour = _tournaments.value.find { it.id == tournamentId } ?: return
        val participants = _registrations.value.filter { it.tournamentId == tournamentId && it.status == RegistrationStatus.CONFIRMED }

        if (participants.isEmpty()) return

        val numGroups = tour.numGroups.coerceAtLeast(1)
        val groups = (0 until numGroups).map { "Group " + ('A' + it) }

        val newStandings = mutableListOf<GroupStanding>()
        val newFixtures = mutableListOf<MatchFixture>()

        participants.forEachIndexed { index, participant ->
            val assignedGroup = groups[index % numGroups]
            newStandings.add(
                GroupStanding(
                    tournamentId = tournamentId,
                    groupName = assignedGroup,
                    participantId = participant.userId,
                    participantName = participant.userName
                )
            )
        }

        groups.forEach { groupName ->
            val groupMembers = newStandings.filter { it.groupName == groupName }
            var roundCount = 1
            for (i in 0 until groupMembers.size) {
                for (j in i + 1 until groupMembers.size) {
                    val pA = groupMembers[i]
                    val pB = groupMembers[j]
                    val fixture = MatchFixture(
                        tournamentId = tournamentId,
                        tournamentTitle = tour.title,
                        game = tour.game,
                        groupName = groupName,
                        round = "Matchday $roundCount",
                        participantAId = pA.participantId,
                        participantAName = pA.participantName,
                        participantBId = pB.participantId,
                        participantBName = pB.participantName,
                        scheduledTime = "Scheduled",
                        status = MatchStatus.SCHEDULED
                    )
                    newFixtures.add(fixture)
                    roundCount++
                }
            }
        }

        _standings.value = _standings.value.filter { it.tournamentId != tournamentId } + newStandings
        _matches.value = _matches.value.filter { it.tournamentId != tournamentId } + newFixtures

        val updatedTour = tour.copy(status = TournamentStatus.GROUP_STAGE)
        _tournaments.value = _tournaments.value.map {
            if (it.id == tournamentId) updatedTour else it
        }

        // Real Firestore sync
        newFixtures.forEach { FirebaseManager.syncMatchToFirestore(it) }
        newStandings.forEach { FirebaseManager.syncStandingToFirestore(it) }
        FirebaseManager.syncTournamentToFirestore(updatedTour)

        logAdminAction(
            "FIXTURES_GENERATED",
            "Tournament",
            tournamentId,
            "Auto-generated ${newFixtures.size} fixtures across ${numGroups} groups"
        )
    }

    // Room Credentials Distribution
    fun setRoomCredentials(matchId: String, roomId: String, password: String) {
        _matches.value = _matches.value.map { match ->
            if (match.id == matchId) {
                match.copy(
                    roomId = roomId,
                    roomPassword = password,
                    status = MatchStatus.ROOM_READY
                )
            } else match
        }

        val updatedMatch = _matches.value.find { it.id == matchId }
        if (updatedMatch != null) {
            FirebaseManager.syncMatchToFirestore(updatedMatch)
            listOf(updatedMatch.participantAId, updatedMatch.participantBId).forEach { uid ->
                addNotification(
                    userId = uid,
                    title = "Room Ready: ${updatedMatch.tournamentTitle}",
                    message = "Room ID: $roomId | Password: $password. Join immediately!",
                    type = "MATCH"
                )
            }
            logAdminAction("ROOM_CREDENTIALS_SET", "Match", matchId, "Room ID: $roomId assigned")
        }
    }

    // Submit Match Score
    fun submitMatchScore(matchId: String, scoreA: Int, scoreB: Int, winnerId: String, proofUrl: String = "") {
        val user = _currentUser.value
        _matches.value = _matches.value.map { match ->
            if (match.id == matchId) {
                val winnerName = if (winnerId == match.participantAId) match.participantAName else match.participantBName
                val updated = match.copy(
                    scoreA = scoreA,
                    scoreB = scoreB,
                    winnerId = winnerId,
                    winnerName = winnerName,
                    proofUrl = proofUrl,
                    submittedBy = user.username,
                    status = MatchStatus.UNDER_REVIEW
                )
                FirebaseManager.syncMatchToFirestore(updated)
                updated
            } else match
        }
        logAdminAction("SCORE_SUBMITTED", "Match", matchId, "Player ${user.username} submitted score: $scoreA - $scoreB")
    }

    // Verify Match Result
    fun verifyMatchResult(matchId: String, overrideWinnerId: String? = null) {
        val match = _matches.value.find { it.id == matchId } ?: return
        val finalWinnerId = overrideWinnerId ?: match.winnerId ?: match.participantAId
        val finalWinnerName = if (finalWinnerId == match.participantAId) match.participantAName else match.participantBName
        val sA = match.scoreA ?: if (finalWinnerId == match.participantAId) 2 else 0
        val sB = match.scoreB ?: if (finalWinnerId == match.participantBId) 2 else 0

        val updatedMatch = match.copy(
            winnerId = finalWinnerId,
            winnerName = finalWinnerName,
            scoreA = sA,
            scoreB = sB,
            status = MatchStatus.VERIFIED
        )

        _matches.value = _matches.value.map {
            if (it.id == matchId) updatedMatch else it
        }
        FirebaseManager.syncMatchToFirestore(updatedMatch)

        // Update group standings if group stage
        if (match.groupName != null) {
            _standings.value = _standings.value.map { st ->
                if (st.tournamentId == match.tournamentId && st.groupName == match.groupName) {
                    val updatedStanding = when (st.participantId) {
                        match.participantAId -> {
                            val won = sA > sB
                            val draw = sA == sB
                            st.copy(
                                played = st.played + 1,
                                won = st.won + if (won) 1 else 0,
                                drawn = st.drawn + if (draw) 1 else 0,
                                lost = st.lost + if (!won && !draw) 1 else 0,
                                points = st.points + (if (won) 3 else if (draw) 1 else 0)
                            )
                        }
                        match.participantBId -> {
                            val won = sB > sA
                            val draw = sA == sB
                            st.copy(
                                played = st.played + 1,
                                won = st.won + if (won) 1 else 0,
                                drawn = st.drawn + if (draw) 1 else 0,
                                lost = st.lost + if (!won && !draw) 1 else 0,
                                points = st.points + (if (won) 3 else if (draw) 1 else 0)
                            )
                        }
                        else -> st
                    }
                    FirebaseManager.syncStandingToFirestore(updatedStanding)
                    updatedStanding
                } else st
            }

            checkAndAdvanceKnockout(match.tournamentId)
        } else if (match.isKnockout) {
            if (match.round.contains("Final", ignoreCase = true) && !match.round.contains("Semi", ignoreCase = true)) {
                completeTournament(match.tournamentId, finalWinnerName)
            }
        }

        // Update user stats in database
        _users.value = _users.value.map { u ->
            val updatedUser = if (u.uid == finalWinnerId) {
                u.copy(wins = u.wins + 1, matchesPlayed = u.matchesPlayed + 1, points = u.points + 3)
            } else if (u.uid == match.participantAId || u.uid == match.participantBId) {
                u.copy(losses = u.losses + 1, matchesPlayed = u.matchesPlayed + 1)
            } else u
            FirebaseManager.syncUserToFirestore(updatedUser)
            updatedUser
        }

        logAdminAction("RESULT_VERIFIED", "Match", matchId, "Result verified for $finalWinnerName ($sA - $sB)")
    }

    private fun checkAndAdvanceKnockout(tournamentId: String) {
        val tour = _tournaments.value.find { it.id == tournamentId } ?: return
        val groupMatches = _matches.value.filter { it.tournamentId == tournamentId && it.groupName != null }
        val allFinished = groupMatches.all { it.status == MatchStatus.VERIFIED || it.status == MatchStatus.COMPLETED }

        if (allFinished && groupMatches.isNotEmpty()) {
            val standingsForTour = _standings.value.filter { it.tournamentId == tournamentId }
            val groups = standingsForTour.map { it.groupName }.distinct()
            val qualifiers = mutableListOf<GroupStanding>()

            groups.forEach { g ->
                val topInGroup = standingsForTour
                    .filter { it.groupName == g }
                    .sortedWith(compareByDescending<GroupStanding> { it.points }.thenByDescending { it.pointDiff })
                    .take(tour.qualifiersPerGroup)
                qualifiers.addAll(topInGroup)
            }

            _standings.value = _standings.value.map {
                val isQ = qualifiers.any { q -> q.participantId == it.participantId }
                val updated = it.copy(isQualified = isQ)
                FirebaseManager.syncStandingToFirestore(updated)
                updated
            }

            if (qualifiers.size >= 2) {
                val knockoutMatch = MatchFixture(
                    tournamentId = tournamentId,
                    tournamentTitle = tour.title,
                    game = tour.game,
                    groupName = null,
                    round = if (qualifiers.size > 2) "Semi-Final" else "Grand Final",
                    participantAId = qualifiers[0].participantId,
                    participantAName = qualifiers[0].participantName,
                    participantBId = qualifiers[1].participantId,
                    participantBName = qualifiers[1].participantName,
                    scheduledTime = "Scheduled",
                    status = MatchStatus.SCHEDULED,
                    isKnockout = true
                )
                _matches.value = _matches.value + knockoutMatch
                FirebaseManager.syncMatchToFirestore(knockoutMatch)

                val updatedTour = tour.copy(status = TournamentStatus.KNOCKOUT)
                _tournaments.value = _tournaments.value.map {
                    if (it.id == tournamentId) updatedTour else it
                }
                FirebaseManager.syncTournamentToFirestore(updatedTour)
                logAdminAction("KNOCKOUT_GENERATED", "Tournament", tournamentId, "Knockout stage seeded with ${qualifiers.size} qualifiers")
            }
        }
    }

    private fun completeTournament(tournamentId: String, championName: String) {
        val tour = _tournaments.value.find { it.id == tournamentId } ?: return
        val updatedTour = tour.copy(status = TournamentStatus.COMPLETED, championName = championName)
        _tournaments.value = _tournaments.value.map {
            if (it.id == tournamentId) updatedTour else it
        }
        FirebaseManager.syncTournamentToFirestore(updatedTour)

        val champUser = _users.value.find { it.username == championName }
        if (champUser != null) {
            val updatedChamp = champUser.copy(
                walletBalance = champUser.walletBalance + tour.firstPrize,
                tournamentWins = champUser.tournamentWins + 1
            )
            _users.value = _users.value.map {
                if (it.uid == champUser.uid) updatedChamp else it
            }
            if (_currentUser.value.uid == champUser.uid) {
                _currentUser.value = updatedChamp
            }
            FirebaseManager.syncUserToFirestore(updatedChamp)

            addNotification(
                userId = champUser.uid,
                title = "CHAMPION! 🏆",
                message = "Congratulations! You won ${tour.title}. ৳${tour.firstPrize} credited to your wallet!",
                type = "TOURNAMENT"
            )
        }

        logAdminAction("TOURNAMENT_COMPLETED", "Tournament", tournamentId, "Champion: $championName, ৳${tour.firstPrize} awarded")
    }

    // Payment Operations
    fun requestDeposit(amount: Double, method: String, senderNumber: String, trxId: String) {
        val user = _currentUser.value
        val payment = PaymentTransaction(
            userId = user.uid,
            userName = user.username,
            amount = amount,
            method = method,
            senderNumber = senderNumber,
            transactionId = trxId,
            type = "WALLET_TOPUP",
            status = PaymentStatus.PENDING,
            note = "Deposit from $senderNumber"
        )
        _payments.value = listOf(payment) + _payments.value
        FirebaseManager.syncPaymentToFirestore(payment)

        addNotification(
            userId = user.uid,
            title = "Deposit Submitted: ৳${amount.toInt()}",
            message = "Your deposit of ৳$amount via $method (Sender: $senderNumber, TrxID: $trxId) is pending admin verification.",
            type = "PAYMENT"
        )
    }

    fun approvePayment(paymentId: String) {
        val payment = _payments.value.find { it.id == paymentId } ?: return
        val updatedPayment = payment.copy(status = PaymentStatus.PAID, note = "Verified & Approved")
        _payments.value = _payments.value.map {
            if (it.id == paymentId) updatedPayment else it
        }
        FirebaseManager.syncPaymentToFirestore(updatedPayment)

        _users.value = _users.value.map {
            if (it.uid == payment.userId) {
                val updated = it.copy(walletBalance = it.walletBalance + payment.amount)
                FirebaseManager.syncUserToFirestore(updated)
                updated
            } else it
        }
        if (_currentUser.value.uid == payment.userId) {
            _currentUser.value = _currentUser.value.copy(walletBalance = _currentUser.value.walletBalance + payment.amount)
        }

        addNotification(
            userId = payment.userId,
            title = "Payment Approved! ৳${payment.amount}",
            message = "Your deposit has been verified and added to your wallet.",
            type = "PAYMENT"
        )
        logAdminAction("PAYMENT_APPROVED", "Payment", paymentId, "Approved ৳${payment.amount} for ${payment.userName}")
    }

    fun rejectPayment(paymentId: String, reason: String) {
        val payment = _payments.value.find { it.id == paymentId } ?: return
        val updated = payment.copy(status = PaymentStatus.FAILED, note = "Rejected: $reason")
        _payments.value = _payments.value.map {
            if (it.id == paymentId) updated else it
        }
        FirebaseManager.syncPaymentToFirestore(updated)

        addNotification(
            userId = payment.userId,
            title = "Payment Rejected",
            message = "Deposit of ৳${payment.amount} was rejected. Reason: $reason",
            type = "PAYMENT"
        )
        logAdminAction("PAYMENT_REJECTED", "Payment", paymentId, "Rejected ৳${payment.amount}. Reason: $reason")
    }

    // Team Operations
    fun createTeam(name: String, tag: String, profileImageUrl: String = "", bannerUrl: String = ""): Result<Unit> {
        val user = _currentUser.value
        if (user.teamId != null) {
            return Result.failure(Exception("You are already in a team. Leave it before creating a new one."))
        }
        if (name.isBlank() || tag.isBlank()) {
            return Result.failure(Exception("Team name and tag are required."))
        }
        val newTeam = Team(
            name = name.trim(),
            tag = tag.trim().uppercase(),
            logoUrl = profileImageUrl,
            profileImageUrl = profileImageUrl,
            bannerUrl = bannerUrl,
            captainId = user.uid,
            captainName = user.username,
            members = listOf(
                TeamMember(userId = user.uid, username = user.username, gameUid = user.inGameUid, isCaptain = true)
            )
        )
        _teams.value = _teams.value + newTeam
        FirebaseManager.syncTeamToFirestore(newTeam)

        upsertUser(user.copy(teamId = newTeam.id, teamName = newTeam.name))
        logAdminAction("TEAM_CREATED", "Team", newTeam.id, "Team ${newTeam.name} created by ${user.username}")
        return Result.success(Unit)
    }

    /** A player sends a join request to a team. The team captain later accepts/rejects it. */
    fun sendJoinRequest(teamId: String): Result<Unit> {
        val user = _currentUser.value
        if (user.teamId != null) {
            return Result.failure(Exception("You are already in a team."))
        }
        val team = _teams.value.find { it.id == teamId }
            ?: return Result.failure(Exception("Team not found."))
        if (team.members.any { it.userId == user.uid }) {
            return Result.failure(Exception("You are already a member of this team."))
        }
        if (team.joinRequests.contains(user.uid)) {
            return Result.failure(Exception("You have already sent a request to this team."))
        }
        val updated = team.copy(joinRequests = team.joinRequests + user.uid)
        _teams.value = _teams.value.map { if (it.id == teamId) updated else it }
        FirebaseManager.syncTeamToFirestore(updated)
        addNotification(
            userId = team.captainId,
            title = "New Join Request",
            message = "${user.username} wants to join ${team.name}.",
            type = "SYSTEM"
        )
        return Result.success(Unit)
    }

    /** Captain accepts or rejects a pending join request. */
    fun respondJoinRequest(teamId: String, userId: String, accept: Boolean) {
        val team = _teams.value.find { it.id == teamId } ?: return
        val requester = _users.value.find { it.uid == userId } ?: return
        val remainingRequests = team.joinRequests.filter { it != userId }

        if (!accept) {
            val updated = team.copy(joinRequests = remainingRequests)
            _teams.value = _teams.value.map { if (it.id == teamId) updated else it }
            FirebaseManager.syncTeamToFirestore(updated)
            addNotification(userId, "Request Declined", "Your request to join ${team.name} was declined.", "SYSTEM")
            return
        }

        val newMember = TeamMember(
            userId = requester.uid,
            username = requester.username,
            gameUid = requester.inGameUid,
            isCaptain = false
        )
        val updated = team.copy(
            members = team.members.filter { it.userId != userId } + newMember,
            joinRequests = remainingRequests
        )
        _teams.value = _teams.value.map { if (it.id == teamId) updated else it }
        FirebaseManager.syncTeamToFirestore(updated)

        // Attach the requester to the team
        upsertUser(requester.copy(teamId = team.id, teamName = team.name))
        addNotification(userId, "Request Accepted!", "You are now a member of ${team.name}.", "SYSTEM")
        logAdminAction("TEAM_MEMBER_ADDED", "Team", teamId, "${requester.username} joined ${team.name}")
    }

    /** Remove a member from a team (captain action). */
    fun removeTeamMember(teamId: String, userId: String) {
        val team = _teams.value.find { it.id == teamId } ?: return
        if (team.captainId == userId) return // cannot remove the captain here
        val updated = team.copy(members = team.members.filter { it.userId != userId })
        _teams.value = _teams.value.map { if (it.id == teamId) updated else it }
        FirebaseManager.syncTeamToFirestore(updated)
        _users.value.find { it.uid == userId }?.let { upsertUser(it.copy(teamId = null, teamName = null)) }
    }

    // ==================== Team vs Team Challenges ====================

    private fun teamCaptain(teamId: String): UserProfile? {
        val team = _teams.value.find { it.id == teamId } ?: return null
        return _users.value.find { it.uid == team.captainId }
    }

    fun challengeTeam(challengedTeamId: String, game: String, stakeAmount: Double): Result<Unit> {
        val me = _currentUser.value
        val myTeamId = me.teamId ?: return Result.failure(Exception("আপনাকে আগে একটি টিমে থাকতে হবে।"))
        val myTeam = _teams.value.find { it.id == myTeamId } ?: return Result.failure(Exception("আপনার টিম পাওয়া যায়নি।"))
        val opponent = _teams.value.find { it.id == challengedTeamId } ?: return Result.failure(Exception("প্রতিপক্ষ টিম পাওয়া যায়নি।"))

        if (myTeam.captainId != me.uid) return Result.failure(Exception("শুধু টিম ক্যাপ্টেনই টিম চ্যালেঞ্জ পাঠাতে পারবেন।"))
        if (myTeam.id == opponent.id) return Result.failure(Exception("নিজের টিমকে চ্যালেঞ্জ করা যাবে না।"))
        if (!opponent.isLive) return Result.failure(Exception("প্রতিপক্ষ টিমটি এখন লাইভ নেই।"))
        if (stakeAmount + 0.01 < CHALLENGE_STAKE) return Result.failure(Exception("টিম চ্যালেঞ্জের জন্য ন্যূনতম ৫০ টাকা প্রয়োজন।"))
        if (me.walletBalance < CHALLENGE_STAKE) return Result.failure(Exception("আপনার অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই। আগে ডিপোজিট করুন।"))

        val duplicate = _challenges.value.any { ch ->
            ch.status in setOf(
                ChallengeStatus.PENDING,
                ChallengeStatus.ACCEPTED,
                ChallengeStatus.ROOM_SET,
                ChallengeStatus.PROOF_SUBMITTED,
                ChallengeStatus.UNDER_REVIEW
            ) && (
                (ch.challengerTeamId == myTeam.id && ch.challengedTeamId == opponent.id) ||
                    (ch.challengerTeamId == opponent.id && ch.challengedTeamId == myTeam.id)
                )
        }
        if (duplicate) return Result.failure(Exception("এই দুই টিমের মধ্যে একটি চলমান চ্যালেঞ্জ আগে থেকেই আছে।"))

        val challenge = TeamChallenge(
            challengerTeamId = myTeam.id,
            challengerTeamName = myTeam.name,
            challengedTeamId = opponent.id,
            challengedTeamName = opponent.name,
            game = game.ifBlank { "Free Fire" },
            stakeAmount = CHALLENGE_STAKE,
            status = ChallengeStatus.PENDING
        )
        _challenges.value = listOf(challenge) + _challenges.value
        FirebaseManager.syncChallengeToFirestore(challenge)

        addNotification(
            opponent.captainId,
            "টিম চ্যালেঞ্জ এসেছে",
            myTeam.name + " আপনাদের " + opponent.name + "-কে ৫০ টাকার " + challenge.game + " চ্যালেঞ্জ দিয়েছে।",
            "TEAM_CHALLENGE"
        )
        return Result.success(Unit)
    }

    fun acceptChallenge(challengeId: String): Result<Unit> {
        val challenge = _challenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status != ChallengeStatus.PENDING) return Result.failure(Exception("এই চ্যালেঞ্জটি আর গ্রহণযোগ্য নয়।"))

        val me = _currentUser.value
        val myTeamId = me.teamId ?: return Result.failure(Exception("আপনি কোনো টিমে নেই।"))
        if (myTeamId != challenge.challengedTeamId) return Result.failure(Exception("শুধু যাকে চ্যালেঞ্জ করা হয়েছে সেই টিমই Accept করতে পারবে।"))

        val myTeam = _teams.value.find { it.id == myTeamId } ?: return Result.failure(Exception("আপনার টিম পাওয়া যায়নি।"))
        if (myTeam.captainId != me.uid) return Result.failure(Exception("শুধু টিম ক্যাপ্টেনই Accept করতে পারবেন।"))

        val challengerCaptain = teamCaptain(challenge.challengerTeamId)
            ?: return Result.failure(Exception("চ্যালেঞ্জার টিমের ক্যাপ্টেন পাওয়া যায়নি।"))
        val challengedCaptain = teamCaptain(challenge.challengedTeamId)
            ?: return Result.failure(Exception("আপনার টিমের ক্যাপ্টেন পাওয়া যায়নি।"))

        if (challengerCaptain.walletBalance < CHALLENGE_STAKE) return Result.failure(Exception("চ্যালেঞ্জার টিমের অ্যাকাউন্টে ৫০ টাকা নেই।"))
        if (challengedCaptain.walletBalance < CHALLENGE_STAKE) return Result.failure(Exception("আপনার টিমের অ্যাকাউন্টে ৫০ টাকা নেই। আগে ডিপোজিট করুন।"))

        if (!applyWalletChange(challengerCaptain.uid, -CHALLENGE_STAKE)) {
            return Result.failure(Exception("চ্যালেঞ্জার টিমের ৫০ টাকা লক করা যায়নি।"))
        }
        if (!applyWalletChange(challengedCaptain.uid, -CHALLENGE_STAKE)) {
            applyWalletChange(challengerCaptain.uid, CHALLENGE_STAKE)
            return Result.failure(Exception("আপনার ৫০ টাকা লক করা যায়নি।"))
        }

        val acceptedAt = System.currentTimeMillis()
        val updated = challenge.copy(
            stakeAmount = CHALLENGE_STAKE,
            status = ChallengeStatus.ACCEPTED,
            acceptedAtMillis = acceptedAt,
            deadlineAtMillis = acceptedAt + 60 * 60 * 1000L
        )
        _challenges.value = _challenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncChallengeToFirestore(updated)
        addNotification(
            challengerCaptain.uid,
            "টিম চ্যালেঞ্জ Accept হয়েছে",
            challenge.challengedTeamName + " Accept করেছে। ২০ মিনিটের মধ্যে Room ID ও Password দিন।",
            "TEAM_CHALLENGE"
        )
        return Result.success(Unit)
    }

    fun setTeamChallengeRoom(challengeId: String, roomId: String, password: String): Result<Unit> {
        val challenge = _challenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status != ChallengeStatus.ACCEPTED) return Result.failure(Exception("Accept হওয়ার পরেই Room ID ও Password দেওয়া যাবে।"))

        val me = _currentUser.value
        val myTeamId = me.teamId ?: return Result.failure(Exception("আপনি কোনো টিমে নেই।"))
        val myTeam = _teams.value.find { it.id == myTeamId }
        if (myTeamId != challenge.challengerTeamId || myTeam?.captainId != me.uid) {
            return Result.failure(Exception("শুধু চ্যালেঞ্জ পাঠানো টিমের ক্যাপ্টেন Room দিতে পারবেন।"))
        }
        if (roomId.isBlank() || password.isBlank()) return Result.failure(Exception("Room ID এবং Password দুটোই দিতে হবে।"))

        val acceptedAt = challenge.acceptedAtMillis.takeIf { it > 0L } ?: challenge.timestamp
        if (System.currentTimeMillis() > acceptedAt + 20 * 60 * 1000L) {
            return Result.failure(Exception("Room দেওয়ার ২০ মিনিটের সময় শেষ হয়ে গেছে।"))
        }

        val now = System.currentTimeMillis()
        val updated = challenge.copy(
            roomId = roomId.trim(),
            roomPassword = password.trim(),
            status = ChallengeStatus.ROOM_SET,
            roomSetAtMillis = now,
            proofOpenAtMillis = now + 10 * 60 * 1000L,
            deadlineAtMillis = challenge.deadlineAtMillis.takeIf { it > 0L } ?: (acceptedAt + 60 * 60 * 1000L)
        )
        _challenges.value = _challenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncChallengeToFirestore(updated)

        teamCaptain(challenge.challengedTeamId)?.let {
            addNotification(it.uid, "টিম Room Ready", "Room ID ও Password এসেছে। ভুল হলে “Wrong Room” রিপোর্ট করুন।", "TEAM_CHALLENGE")
        }
        return Result.success(Unit)
    }

    fun submitTeamChallengeProof(challengeId: String, proofUrl: String): Result<Unit> {
        val challenge = _challenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status != ChallengeStatus.ROOM_SET && challenge.status != ChallengeStatus.PROOF_SUBMITTED) {
            return Result.failure(Exception("Room সেট হওয়ার পরেই Screenshot/Proof দেওয়া যাবে।"))
        }
        if (proofUrl.isBlank()) return Result.failure(Exception("একটি Screenshot upload করুন।"))
        if (challenge.proofOpenAtMillis > 0L && System.currentTimeMillis() < challenge.proofOpenAtMillis) {
            val minutes = ((challenge.proofOpenAtMillis - System.currentTimeMillis() + 59_999L) / 60_000L)
            return Result.failure(Exception("Proof option " + minutes + " মিনিট পরে খুলবে।"))
        }

        val me = _currentUser.value
        val myTeamId = me.teamId ?: return Result.failure(Exception("আপনি কোনো টিমে নেই।"))
        val updated = when {
            myTeamId == challenge.challengerTeamId -> challenge.copy(challengerProofUrl = proofUrl)
            myTeamId == challenge.challengedTeamId -> challenge.copy(challengedProofUrl = proofUrl)
            else -> return Result.failure(Exception("আপনি এই চ্যালেঞ্জের অংশ নন।"))
        }
        val finalStatus = if (!updated.challengerProofUrl.isNullOrBlank() && !updated.challengedProofUrl.isNullOrBlank()) {
            ChallengeStatus.UNDER_REVIEW
        } else ChallengeStatus.PROOF_SUBMITTED

        val saved = updated.copy(status = finalStatus)
        _challenges.value = _challenges.value.map { if (it.id == challengeId) saved else it }
        FirebaseManager.syncChallengeToFirestore(saved)
        return Result.success(Unit)
    }

    fun reportTeamChallengeRoomInvalid(challengeId: String): Result<Unit> {
        val challenge = _challenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status != ChallengeStatus.ROOM_SET && challenge.status != ChallengeStatus.PROOF_SUBMITTED) {
            return Result.failure(Exception("এই সময় Wrong Room রিপোর্ট করা যাবে না।"))
        }
        val myTeamId = _currentUser.value.teamId ?: return Result.failure(Exception("আপনি কোনো টিমে নেই।"))
        if (myTeamId != challenge.challengedTeamId) return Result.failure(Exception("Wrong Room শুধু প্রতিপক্ষ টিম রিপোর্ট করতে পারবে।"))
        return declareTeamChallengeWinner(challengeId, challenge.challengedTeamId, "ভুল Room ID/Password রিপোর্ট করা হয়েছে।")
    }

    fun declareTeamChallengeWinner(challengeId: String, winnerTeamId: String, reason: String = "Admin result"): Result<Unit> {
        val challenge = _challenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status == ChallengeStatus.COMPLETED || challenge.status == ChallengeStatus.CANCELLED) return Result.failure(Exception("এই চ্যালেঞ্জ ইতিমধ্যে শেষ হয়েছে।"))
        if (winnerTeamId != challenge.challengerTeamId && winnerTeamId != challenge.challengedTeamId) return Result.failure(Exception("Winner অবশ্যই অংশ নেওয়া একটি টিম হতে হবে।"))

        val winnerCaptain = teamCaptain(winnerTeamId) ?: return Result.failure(Exception("Winner টিমের ক্যাপ্টেন পাওয়া যায়নি।"))
        val loserTeamId = if (winnerTeamId == challenge.challengerTeamId) challenge.challengedTeamId else challenge.challengerTeamId
        applyWalletChange(winnerCaptain.uid, TEAM_WINNER_PAYOUT)

        val updated = challenge.copy(status = ChallengeStatus.COMPLETED, winnerTeamId = winnerTeamId)
        _challenges.value = _challenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncChallengeToFirestore(updated)

        _teams.value = _teams.value.map { team ->
            when (team.id) {
                winnerTeamId -> {
                    val t = team.copy(matches = team.matches + 1, wins = team.wins + 1, points = team.points + 3)
                    FirebaseManager.syncTeamToFirestore(t)
                    t
                }
                loserTeamId -> {
                    val t = team.copy(matches = team.matches + 1, losses = team.losses + 1)
                    FirebaseManager.syncTeamToFirestore(t)
                    t
                }
                else -> team
            }
        }
        teamCaptain(loserTeamId)?.let {
            addNotification(it.uid, "টিম ম্যাচ শেষ", "এই টিম ম্যাচে আপনি হেরে গেছেন।", "TEAM_CHALLENGE")
        }
        addNotification(winnerCaptain.uid, "টিম ম্যাচ জয়", "আপনার টিম জিতেছে। ৮৫ টাকা Wallet-এ যোগ হয়েছে।", "TEAM_CHALLENGE")
        logAdminAction("TEAM_CHALLENGE_COMPLETED", "TeamChallenge", challengeId, reason + " Winner=" + winnerTeamId)
        return Result.success(Unit)
    }

    fun cancelChallenge(challengeId: String): Result<Unit> {
        val challenge = _challenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status !in setOf(
                ChallengeStatus.PENDING,
                ChallengeStatus.ACCEPTED,
                ChallengeStatus.ROOM_SET,
                ChallengeStatus.PROOF_SUBMITTED
            )) return Result.failure(Exception("এই চ্যালেঞ্জ এখন Cancel করা যাবে না।"))

        val myTeamId = _currentUser.value.teamId ?: return Result.failure(Exception("আপনি কোনো টিমে নেই।"))
        if (myTeamId != challenge.challengerTeamId && myTeamId != challenge.challengedTeamId) {
            return Result.failure(Exception("আপনি এই চ্যালেঞ্জের অংশ নন।"))
        }

        if (challenge.status != ChallengeStatus.PENDING) {
            teamCaptain(challenge.challengerTeamId)?.let { applyWalletChange(it.uid, CHALLENGE_STAKE) }
            teamCaptain(challenge.challengedTeamId)?.let { applyWalletChange(it.uid, CHALLENGE_STAKE) }
        }

        val updated = challenge.copy(status = ChallengeStatus.CANCELLED)
        _challenges.value = _challenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncChallengeToFirestore(updated)
        return Result.success(Unit)
    }

    // ==================== 1v1 Player Challenges ====================

    fun sendUserChallenge(opponentUid: String, opponentName: String): Result<Unit> {
        val challenger = _currentUser.value
        if (opponentUid == challenger.uid) return Result.failure(Exception("নিজেকে চ্যালেঞ্জ করা যাবে না।"))
        val opponent = _users.value.find { it.uid == opponentUid } ?: return Result.failure(Exception("প্রতিপক্ষ খেলোয়াড় পাওয়া যায়নি।"))
        if (challenger.teamId != null && challenger.teamId == opponent.teamId) return Result.failure(Exception("নিজের টিমের সদস্যকে চ্যালেঞ্জ করা যাবে না।"))
        if (challenger.walletBalance < CHALLENGE_STAKE) return Result.failure(Exception("আপনার অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই। আগে ডিপোজিট করুন।"))

        val openStatuses = setOf(
            UserChallengeStatus.PENDING,
            UserChallengeStatus.ACCEPTED,
            UserChallengeStatus.ROOM_SET,
            UserChallengeStatus.PROOF_SUBMITTED,
            UserChallengeStatus.UNDER_REVIEW
        )
        if (_userChallenges.value.any {
                it.status in openStatuses &&
                    ((it.challengerUid == challenger.uid && it.opponentUid == opponentUid) ||
                        (it.challengerUid == opponentUid && it.opponentUid == challenger.uid))
            }) return Result.failure(Exception("এই খেলোয়াড়ের সাথে একটি চলমান চ্যালেঞ্জ আগে থেকেই আছে।"))

        val challenge = UserChallenge(
            challengerUid = challenger.uid,
            challengerName = challenger.username,
            opponentUid = opponentUid,
            opponentName = opponentName,
            stakeAmount = CHALLENGE_STAKE,
            status = UserChallengeStatus.PENDING
        )
        _userChallenges.value = listOf(challenge) + _userChallenges.value
        FirebaseManager.syncUserChallengeToFirestore(challenge)
        addNotification(opponentUid, "1v1 চ্যালেঞ্জ এসেছে", challenger.username + " আপনাকে ৫০ টাকার Free Fire 1v1 চ্যালেঞ্জ দিয়েছে।", "PLAYER_CHALLENGE")
        return Result.success(Unit)
    }

    fun acceptUserChallenge(challengeId: String): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status != UserChallengeStatus.PENDING) return Result.failure(Exception("এই চ্যালেঞ্জটি আর Accept করা যাবে না."))
        val me = _currentUser.value
        if (me.uid != challenge.opponentUid) return Result.failure(Exception("শুধু আমন্ত্রিত খেলোয়াড় Accept করতে পারবেন।"))
        if (me.walletBalance < CHALLENGE_STAKE) return Result.failure(Exception("আপনার অ্যাকাউন্টে পর্যাপ্ত ব্যালেন্স নেই। আগে ডিপোজিট করুন।"))

        val challenger = _users.value.find { it.uid == challenge.challengerUid } ?: return Result.failure(Exception("চ্যালেঞ্জার খেলোয়াড় পাওয়া যায়নি।"))
        if (challenger.walletBalance < CHALLENGE_STAKE) return Result.failure(Exception("চ্যালেঞ্জার খেলোয়াড়ের ব্যালেন্স কমে গেছে।"))

        if (!applyWalletChange(challenge.challengerUid, -CHALLENGE_STAKE)) return Result.failure(Exception("চ্যালেঞ্জারের ৫০ টাকা লক করা যায়নি।"))
        if (!applyWalletChange(challenge.opponentUid, -CHALLENGE_STAKE)) {
            applyWalletChange(challenge.challengerUid, CHALLENGE_STAKE)
            return Result.failure(Exception("আপনার ৫০ টাকা লক করা যায়নি।"))
        }

        val acceptedAt = System.currentTimeMillis()
        val updated = challenge.copy(status = UserChallengeStatus.ACCEPTED, acceptedAtMillis = acceptedAt, deadlineAtMillis = acceptedAt + 60 * 60 * 1000L)
        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncUserChallengeToFirestore(updated)
        addNotification(challenge.challengerUid, "1v1 Accept হয়েছে", me.username + " Accept করেছে। ২০ মিনিটের মধ্যে Room ID ও Password দিন.", "PLAYER_CHALLENGE")
        return Result.success(Unit)
    }

    fun cancelUserChallenge(challengeId: String): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status !in setOf(
                UserChallengeStatus.PENDING,
                UserChallengeStatus.ACCEPTED,
                UserChallengeStatus.ROOM_SET,
                UserChallengeStatus.PROOF_SUBMITTED
            )) return Result.failure(Exception("এই চ্যালেঞ্জ এখন Cancel করা যাবে না।"))
        val me = _currentUser.value.uid
        if (me != challenge.challengerUid && me != challenge.opponentUid) return Result.failure(Exception("আপনি এই চ্যালেঞ্জের অংশ নন।"))
        if (challenge.status != UserChallengeStatus.PENDING) {
            applyWalletChange(challenge.challengerUid, CHALLENGE_STAKE)
            applyWalletChange(challenge.opponentUid, CHALLENGE_STAKE)
        }
        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) it.copy(status = UserChallengeStatus.CANCELLED) else it }
        FirebaseManager.syncUserChallengeToFirestore(_userChallenges.value.first { it.id == challengeId })
        val otherUid = if (me == challenge.challengerUid) challenge.opponentUid else challenge.challengerUid
        addNotification(otherUid, "1v1 Cancelled", "চ্যালেঞ্জ Cancel হয়েছে এবং লক করা ৫০ টাকা ফেরত দেওয়া হয়েছে।", "PLAYER_CHALLENGE")
        return Result.success(Unit)
    }

    fun rejectUserChallenge(challengeId: String): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status != UserChallengeStatus.PENDING) return Result.failure(Exception("এই চ্যালেঞ্জটি Pending নেই।"))
        if (_currentUser.value.uid != challenge.opponentUid) return Result.failure(Exception("শুধু আমন্ত্রিত খেলোয়াড় Reject করতে পারবেন।"))
        val updated = challenge.copy(status = UserChallengeStatus.REJECTED)
        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncUserChallengeToFirestore(updated)
        addNotification(challenge.challengerUid, "1v1 Reject হয়েছে", challenge.opponentName + " আপনার Challenge Reject করেছে।", "PLAYER_CHALLENGE")
        return Result.success(Unit)
    }

    fun setUserChallengeRoom(challengeId: String, roomId: String, password: String): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status != UserChallengeStatus.ACCEPTED) return Result.failure(Exception("Accept হওয়ার পরেই Room দেওয়া যাবে।"))
        if (_currentUser.value.uid != challenge.challengerUid) return Result.failure(Exception("শুধু চ্যালেঞ্জ পাঠানো খেলোয়াড় Room দিতে পারবেন।"))
        if (roomId.isBlank() || password.isBlank()) return Result.failure(Exception("Room ID এবং Password দুটোই দিতে হবে।"))

        val acceptedAt = challenge.acceptedAtMillis.takeIf { it > 0L } ?: challenge.timestamp
        if (System.currentTimeMillis() > acceptedAt + 20 * 60 * 1000L) return Result.failure(Exception("Room দেওয়ার ২০ মিনিটের সময় শেষ হয়ে গেছে।"))

        val now = System.currentTimeMillis()
        val updated = challenge.copy(
            roomId = roomId.trim(),
            roomPassword = password.trim(),
            status = UserChallengeStatus.ROOM_SET,
            roomSetAtMillis = now,
            proofOpenAtMillis = now + 10 * 60 * 1000L,
            deadlineAtMillis = challenge.deadlineAtMillis.takeIf { it > 0L } ?: (acceptedAt + 60 * 60 * 1000L)
        )
        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncUserChallengeToFirestore(updated)
        addNotification(challenge.opponentUid, "1v1 Room Ready", "Room ID ও Password এসেছে। ভুল হলে “Wrong Room” রিপোর্ট করুন।", "PLAYER_CHALLENGE")
        return Result.success(Unit)
    }

    fun submitUserChallengeProof(challengeId: String, proofUrl: String): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status != UserChallengeStatus.ROOM_SET && challenge.status != UserChallengeStatus.PROOF_SUBMITTED) {
            return Result.failure(Exception("Room সেট হওয়ার পরে Proof দেওয়া যাবে।"))
        }
        if (proofUrl.isBlank()) return Result.failure(Exception("একটি Screenshot upload করুন।"))
        if (challenge.proofOpenAtMillis > 0L && System.currentTimeMillis() < challenge.proofOpenAtMillis) {
            val minutes = ((challenge.proofOpenAtMillis - System.currentTimeMillis() + 59_999L) / 60_000L)
            return Result.failure(Exception("Proof option " + minutes + " মিনিট পরে খুলবে।"))
        }

        val me = _currentUser.value.uid
        val updated = when (me) {
            challenge.challengerUid -> challenge.copy(challengerProofUrl = proofUrl)
            challenge.opponentUid -> challenge.copy(opponentProofUrl = proofUrl)
            else -> return Result.failure(Exception("আপনি এই চ্যালেঞ্জের অংশ নন।"))
        }
        val finalStatus = if (!updated.challengerProofUrl.isNullOrBlank() && !updated.opponentProofUrl.isNullOrBlank()) UserChallengeStatus.UNDER_REVIEW else UserChallengeStatus.PROOF_SUBMITTED
        val saved = updated.copy(status = finalStatus)
        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) saved else it }
        FirebaseManager.syncUserChallengeToFirestore(saved)
        val otherUid = if (me == challenge.challengerUid) challenge.opponentUid else challenge.challengerUid
        addNotification(otherUid, "1v1 Proof Update", "প্রতিপক্ষ Screenshot/Proof জমা দিয়েছে।", "PLAYER_CHALLENGE")
        return Result.success(Unit)
    }

    fun reportUserChallengeRoomInvalid(challengeId: String): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status != UserChallengeStatus.ROOM_SET && challenge.status != UserChallengeStatus.PROOF_SUBMITTED) return Result.failure(Exception("এই সময় Wrong Room রিপোর্ট করা যাবে না।"))
        if (_currentUser.value.uid != challenge.opponentUid) return Result.failure(Exception("Wrong Room শুধু প্রতিপক্ষ রিপোর্ট করতে পারবে।"))
        return declareUserChallengeWinner(challengeId, challenge.opponentUid, "প্রতিপক্ষ Wrong Room রিপোর্ট করেছে।")
    }

    fun declareUserChallengeWinner(challengeId: String, winnerUid: String, reason: String = "Admin result"): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId } ?: return Result.failure(Exception("চ্যালেঞ্জ পাওয়া যায়নি।"))
        if (challenge.status == UserChallengeStatus.COMPLETED || challenge.status == UserChallengeStatus.CANCELLED) return Result.failure(Exception("এই চ্যালেঞ্জ ইতিমধ্যে শেষ হয়েছে।"))
        if (winnerUid != challenge.challengerUid && winnerUid != challenge.opponentUid) return Result.failure(Exception("Winner অবশ্যই অংশ নেওয়া একজন খেলোয়াড় হতে হবে।"))

        val winnerName = if (winnerUid == challenge.challengerUid) challenge.challengerName else challenge.opponentName
        applyWalletChange(winnerUid, CHALLENGE_WINNER_PAYOUT)
        val loserUid = if (winnerUid == challenge.challengerUid) challenge.opponentUid else challenge.challengerUid

        val updated = challenge.copy(status = UserChallengeStatus.COMPLETED, winnerUid = winnerUid, winnerName = winnerName)
        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncUserChallengeToFirestore(updated)

        _users.value = _users.value.map { user ->
            when (user.uid) {
                winnerUid -> {
                    val u = user.copy(wins = user.wins + 1, matchesPlayed = user.matchesPlayed + 1, points = user.points + 3)
                    FirebaseManager.syncUserToFirestore(u)
                    u
                }
                loserUid -> {
                    val u = user.copy(losses = user.losses + 1, matchesPlayed = user.matchesPlayed + 1)
                    FirebaseManager.syncUserToFirestore(u)
                    u
                }
                else -> user
            }
        }
        addNotification(winnerUid, "1v1 ম্যাচ জয়", "আপনি জিতেছেন। ৮৫ টাকা Wallet-এ যোগ হয়েছে।", "PLAYER_CHALLENGE")
        addNotification(loserUid, "1v1 ম্যাচ শেষ", "আপনি হেরে গেছেন।", "PLAYER_CHALLENGE")
        logAdminAction("CHALLENGE_WINNER_DECLARED", "UserChallenge", challengeId, reason + " Winner=" + winnerUid)
        return Result.success(Unit)
    }

    private fun refundIncompleteUserChallenge(challenge: UserChallenge, message: String) {
        applyWalletChange(challenge.challengerUid, 30.0)
        applyWalletChange(challenge.opponentUid, 30.0)
        val updated = challenge.copy(status = UserChallengeStatus.COMPLETED, winnerUid = null, winnerName = null)
        _userChallenges.value = _userChallenges.value.map { if (it.id == challenge.id) updated else it }
        FirebaseManager.syncUserChallengeToFirestore(updated)
        addNotification(challenge.challengerUid, "1v1 Disqualified", message, "PLAYER_CHALLENGE")
        addNotification(challenge.opponentUid, "1v1 Disqualified", message, "PLAYER_CHALLENGE")
    }

    private fun refundIncompleteTeamChallenge(challenge: TeamChallenge, message: String) {
        teamCaptain(challenge.challengerTeamId)?.let { applyWalletChange(it.uid, 30.0) }
        teamCaptain(challenge.challengedTeamId)?.let { applyWalletChange(it.uid, 30.0) }
        val updated = challenge.copy(status = ChallengeStatus.COMPLETED, winnerTeamId = null)
        _challenges.value = _challenges.value.map { if (it.id == challenge.id) updated else it }
        FirebaseManager.syncChallengeToFirestore(updated)
        teamCaptain(challenge.challengerTeamId)?.let { addNotification(it.uid, "টিম ম্যাচ Disqualified", message, "TEAM_CHALLENGE") }
        teamCaptain(challenge.challengedTeamId)?.let { addNotification(it.uid, "টিম ম্যাচ Disqualified", message, "TEAM_CHALLENGE") }
    }

    private fun processChallengeDeadlines() {
        val now = System.currentTimeMillis()

        _userChallenges.value.toList().forEach { challenge ->
            if (challenge.status == UserChallengeStatus.ACCEPTED) {
                val accepted = challenge.acceptedAtMillis.takeIf { it > 0L } ?: challenge.timestamp
                if (now >= accepted + 20 * 60 * 1000L) {
                    declareUserChallengeWinner(challenge.id, challenge.opponentUid, "২০ মিনিটে Challenger Room দিতে পারেনি — Opponent auto win.")
                }
            } else if (challenge.status == UserChallengeStatus.ROOM_SET || challenge.status == UserChallengeStatus.PROOF_SUBMITTED) {
                if (!challenge.challengerProofUrl.isNullOrBlank() && !challenge.opponentProofUrl.isNullOrBlank()) return@forEach
                val deadline = challenge.deadlineAtMillis.takeIf { it > 0L } ?: (challenge.acceptedAtMillis.takeIf { it > 0L } ?: challenge.timestamp) + 60 * 60 * 1000L
                if (now >= deadline) {
                    when {
                        !challenge.challengerProofUrl.isNullOrBlank() -> declareUserChallengeWinner(challenge.id, challenge.challengerUid, "Opponent Proof দেয়নি — auto win.")
                        !challenge.opponentProofUrl.isNullOrBlank() -> declareUserChallengeWinner(challenge.id, challenge.opponentUid, "Challenger Proof দেয়নি — auto win.")
                        else -> refundIncompleteUserChallenge(challenge, "১ ঘণ্টার মধ্যে কেউ Proof দেয়নি। দুজনেই Disqualified; ২০ টাকা fee রেখে ৩০ টাকা করে ফেরত দেওয়া হয়েছে।")
                    }
                }
            }
        }

        _challenges.value.toList().forEach { challenge ->
            if (challenge.status == ChallengeStatus.ACCEPTED) {
                val accepted = challenge.acceptedAtMillis.takeIf { it > 0L } ?: challenge.timestamp
                if (now >= accepted + 20 * 60 * 1000L) {
                    declareTeamChallengeWinner(challenge.id, challenge.challengedTeamId, "২০ মিনিটে Challenger Team Room দিতে পারেনি — Opponent auto win.")
                }
            } else if (challenge.status == ChallengeStatus.ROOM_SET || challenge.status == ChallengeStatus.PROOF_SUBMITTED) {
                if (!challenge.challengerProofUrl.isNullOrBlank() && !challenge.challengedProofUrl.isNullOrBlank()) return@forEach
                val deadline = challenge.deadlineAtMillis.takeIf { it > 0L } ?: (challenge.acceptedAtMillis.takeIf { it > 0L } ?: challenge.timestamp) + 60 * 60 * 1000L
                if (now >= deadline) {
                    when {
                        !challenge.challengerProofUrl.isNullOrBlank() -> declareTeamChallengeWinner(challenge.id, challenge.challengerTeamId, "Opponent Team Proof দেয়নি — auto win.")
                        !challenge.challengedProofUrl.isNullOrBlank() -> declareTeamChallengeWinner(challenge.id, challenge.challengedTeamId, "Challenger Team Proof দেয়নি — auto win.")
                        else -> refundIncompleteTeamChallenge(challenge, "১ ঘণ্টার মধ্যে কোনো Team Proof আসেনি। দুপক্ষই Disqualified; ২০ টাকা fee রেখে ৩০ টাকা করে ফেরত দেওয়া হয়েছে।")
                    }
                }
            }
        }
    }

    fun deleteUserChallenge(challengeId: String) {
        _userChallenges.value = _userChallenges.value.filter { it.id != challengeId }
        FirebaseManager.deleteUserChallenge(challengeId)
        logAdminAction("CHALLENGE_DELETED", "UserChallenge", challengeId, "Challenge removed by admin")
    }

    // Admin Operations
    fun createTournament(tournament: Tournament) {
        _tournaments.value = listOf(tournament) + _tournaments.value
        FirebaseManager.syncTournamentToFirestore(tournament)
        logAdminAction("TOURNAMENT_CREATED", "Tournament", tournament.id, "Created ${tournament.title}")
    }

    fun deleteTournament(tournamentId: String) {
        _tournaments.value = _tournaments.value.filter { it.id != tournamentId }
        FirebaseManager.deleteTournament(tournamentId)
        logAdminAction("TOURNAMENT_DELETED", "Tournament", tournamentId, "Tournament removed")
    }

    /** Admin updates a tournament lifecycle status (Upcoming/Ongoing/Completed map to enum). */
    fun updateTournamentStatus(tournamentId: String, status: TournamentStatus) {
        val tour = _tournaments.value.find { it.id == tournamentId } ?: return
        val updated = tour.copy(status = status)
        _tournaments.value = _tournaments.value.map { if (it.id == tournamentId) updated else it }
        FirebaseManager.syncTournamentToFirestore(updated)
        logAdminAction("TOURNAMENT_STATUS_UPDATED", "Tournament", tournamentId, "Status set to ${status.name}")
    }

    /** Admin sets the tournament Room ID/password and toggles visibility to registered participants. */
    fun setTournamentRoom(tournamentId: String, roomId: String, password: String, visible: Boolean) {
        val tour = _tournaments.value.find { it.id == tournamentId } ?: return
        val updated = tour.copy(
            roomId = roomId.ifBlank { tour.roomId },
            roomPassword = password.ifBlank { tour.roomPassword },
            roomVisible = visible
        )
        _tournaments.value = _tournaments.value.map { if (it.id == tournamentId) updated else it }
        FirebaseManager.syncTournamentToFirestore(updated)
        logAdminAction("TOURNAMENT_ROOM_UPDATED", "Tournament", tournamentId, "Room visibility=$visible")
    }

    /** Admin controls the dynamic welcome popup shown in the User App. */
    fun updateWelcomePopup(config: WelcomePopupConfig) {
        _welcomePopup.value = config
        FirebaseManager.syncWelcomePopupToFirestore(config)
        logAdminAction("WELCOME_POPUP_UPDATED", "System", "welcome_popup", "visible=${config.isVisible}")
    }

    /** Admin adjusts a user's wallet balance directly (positive or negative delta). */
    fun adjustUserWallet(uid: String, delta: Double): Result<Unit> {
        val user = _users.value.find { it.uid == uid } ?: return Result.failure(Exception("User not found."))
        if (user.walletBalance + delta < 0) {
            return Result.failure(Exception("Adjustment would make the balance negative."))
        }
        upsertUser(user.copy(walletBalance = user.walletBalance + delta))
        logAdminAction("WALLET_ADJUSTED", "User", uid, "Adjusted ${user.username} wallet by ৳$delta")
        return Result.success(Unit)
    }

    /** Admin bans/unbans a user. */
    fun setUserBanned(uid: String, banned: Boolean) {
        _users.value.find { it.uid == uid }?.let { upsertUser(it.copy(isBanned = banned)) }
        logAdminAction("USER_BAN_TOGGLED", "User", uid, "banned=$banned")
    }

    /** Admin deletes a user document. */
    fun deleteUser(uid: String) {
        if (uid == _currentUser.value.uid) return
        _users.value = _users.value.filter { it.uid != uid }
        FirebaseManager.deleteUser(uid)
        logAdminAction("USER_DELETED", "User", uid, "User removed from platform")
    }

    /** Admin deletes a team document. */
    fun deleteTeam(teamId: String) {
        _teams.value = _teams.value.filter { it.id != teamId }
        FirebaseManager.deleteTeam(teamId)
        logAdminAction("TEAM_DELETED", "Team", teamId, "Team removed from platform")
    }

    fun updateSystemSettings(settings: SystemSettings) {
        _settings.value = settings
        FirebaseManager.syncSettingsToFirestore(settings)
        logAdminAction("SETTINGS_UPDATED", "System", "config", "Updated bKash/Nagad config")
    }

    fun setUserRole(targetUserId: String, newRole: UserRole) {
        _users.value = _users.value.map {
            if (it.uid == targetUserId) {
                val updated = it.copy(role = newRole)
                FirebaseManager.syncUserToFirestore(updated)
                updated
            } else it
        }
        if (_currentUser.value.uid == targetUserId) {
            _currentUser.value = _currentUser.value.copy(role = newRole)
        }
        logAdminAction("USER_ROLE_CHANGED", "User", targetUserId, "Assigned role $newRole")
    }

    fun markNotificationsAsRead() {
        val uid = _currentUser.value.uid
        _notifications.value = _notifications.value.map {
            if (it.userId == uid) {
                val updated = it.copy(isRead = true)
                FirebaseManager.updateNotificationReadState(updated.id, true)
                updated
            } else it
        }
    }

    private fun addNotification(userId: String, title: String, message: String, type: String) {
        val item = NotificationItem(userId = userId, title = title, message = message, type = type)
        _notifications.value = listOf(item) + _notifications.value
        FirebaseManager.syncNotificationToFirestore(item)
    }

    private fun logAdminAction(action: String, targetType: String, targetId: String, details: String) {
        val log = AdminActivityLog(
            adminId = _currentUser.value.uid,
            adminName = _currentUser.value.username,
            action = action,
            targetType = targetType,
            targetId = targetId,
            details = details
        )
        _activityLogs.value = listOf(log) + _activityLogs.value
        FirebaseManager.syncActivityLogToFirestore(log)
    }

    // Cron / Worker Automation Engine
    fun runCronCycle(): String {
        var actionsDone = 0

        processChallengeDeadlines()

        // 1. Scan tournaments in REGISTRATION: auto-seed if capacity full
        val tours = _tournaments.value
        tours.filter { it.status == TournamentStatus.REGISTRATION && it.registeredCount >= it.maxParticipants }.forEach { tour ->
            automateGenerateGroupsAndFixtures(tour.id)
            actionsDone++
        }

        // 2. Scan tournaments in GROUP_STAGE: auto-seed knockouts if all group matches are verified
        tours.filter { it.status == TournamentStatus.GROUP_STAGE }.forEach { tour ->
            checkAndAdvanceKnockout(tour.id)
        }

        val currentRuns = _workerStatus.value.totalRunsCount + 1
        val timeStr = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
        val summary = "Cron cycle #$currentRuns completed at $timeStr. $actionsDone auto-tasks executed."

        _workerStatus.value = _workerStatus.value.copy(
            isRunning = true,
            lastRunTimestamp = System.currentTimeMillis(),
            totalRunsCount = currentRuns,
            activeJobsCount = _matches.value.count { it.status == MatchStatus.ROOM_READY || it.status == MatchStatus.LIVE || it.status == MatchStatus.SCHEDULED },
            lastSummary = summary
        )

        return summary
    }

    fun triggerWorkerNow(): String {
        val summary = runCronCycle()
        logAdminAction("CRON_TRIGGERED_MANUAL", "Worker", "cron_job", summary)
        return summary
    }
}