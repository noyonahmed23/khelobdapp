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

    fun challengeTeam(challengedTeamId: String, game: String, stakeAmount: Double): Result<Unit> {
        val myTeamId = _currentUser.value.teamId
            ?: return Result.failure(Exception("You must be in a team to challenge."))

        val challengedTeam = _teams.value.find { it.id == challengedTeamId }
            ?: return Result.failure(Exception("Opponent team not found."))
        val myTeam = _teams.value.find { it.id == myTeamId }
            ?: return Result.failure(Exception("Your team was not found."))

        if (challengedTeam.id == myTeam.id) {
            return Result.failure(Exception("You cannot challenge your own team."))
        }
        if (!challengedTeam.isLive) {
            return Result.failure(Exception("This team is not live/available for a challenge."))
        }
        if (stakeAmount <= 0.0) {
            return Result.failure(Exception("Challenge stake must be greater than zero."))
        }
        if (_currentUser.value.walletBalance < stakeAmount) {
            return Result.failure(Exception("Your wallet does not have enough balance for this challenge."))
        }
        val duplicate = _challenges.value.any {
            it.status == ChallengeStatus.PENDING &&
                ((it.challengerTeamId == myTeam.id && it.challengedTeamId == challengedTeam.id) ||
                 (it.challengerTeamId == challengedTeam.id && it.challengedTeamId == myTeam.id))
        }
        if (duplicate) {
            return Result.failure(Exception("An active challenge already exists between these teams."))
        }

        val challenge = TeamChallenge(
            challengerTeamId = myTeam.id,
            challengerTeamName = myTeam.name,
            challengedTeamId = challengedTeam.id,
            challengedTeamName = challengedTeam.name,
            game = game,
            stakeAmount = stakeAmount,
            status = ChallengeStatus.PENDING
        )
        _challenges.value = listOf(challenge) + _challenges.value
        FirebaseManager.syncChallengeToFirestore(challenge)
        addNotification(
            userId = challengedTeam.captainId,
            title = "Team Challenge Received",
            message = myTeam.name + " challenged your team for ৳" + stakeAmount.toInt() + ".",
            type = "MATCH"
        )
        return Result.success(Unit)
    }

    fun acceptChallenge(challengeId: String): Result<Unit> {
        val challenge = _challenges.value.find { it.id == challengeId }
            ?: return Result.failure(Exception("Challenge not found."))
        if (challenge.status != ChallengeStatus.PENDING) {
            return Result.failure(Exception("This challenge is no longer pending."))
        }

        val myTeamId = _currentUser.value.teamId
            ?: return Result.failure(Exception("You are not in a team."))
        if (myTeamId != challenge.challengedTeamId) {
            return Result.failure(Exception("Only the challenged team can accept this challenge."))
        }

        val myTeam = _teams.value.find { it.id == myTeamId }
            ?: return Result.failure(Exception("Your team was not found."))
        if (_currentUser.value.uid != myTeam.captainId) {
            return Result.failure(Exception("Only the team captain can accept a team challenge."))
        }
        if (_currentUser.value.walletBalance < challenge.stakeAmount) {
            return Result.failure(Exception("Your team captain wallet does not have enough balance."))
        }

        val updatedChal = challenge.copy(status = ChallengeStatus.ACCEPTED)
        _challenges.value = _challenges.value.map {
            if (it.id == challengeId) updatedChal else it
        }
        FirebaseManager.syncChallengeToFirestore(updatedChal)

        addNotification(
            userId = challenge.challengerTeamId,
            title = "Team Challenge Accepted",
            message = challenge.challengedTeamName + " accepted. Waiting for room credentials.",
            type = "MATCH"
        )
        return Result.success(Unit)
    }

    fun setTeamChallengeRoom(challengeId: String, roomId: String, password: String): Result<Unit> {
        val challenge = _challenges.value.find { it.id == challengeId }
            ?: return Result.failure(Exception("Challenge not found."))
        if (challenge.status != ChallengeStatus.ACCEPTED) {
            return Result.failure(Exception("Accept the challenge before setting the room."))
        }
        val myTeamId = _currentUser.value.teamId
            ?: return Result.failure(Exception("You are not in a team."))
        val myTeam = _teams.value.find { it.id == myTeamId }
        if (myTeamId != challenge.challengerTeamId || _currentUser.value.uid != myTeam?.captainId) {
            return Result.failure(Exception("Only the challenging team captain can set the room."))
        }
        if (roomId.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Room ID and password are required."))
        }
        val updated = challenge.copy(
            roomId = roomId.trim(),
            roomPassword = password.trim(),
            status = ChallengeStatus.ROOM_SET
        )
        _challenges.value = _challenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncChallengeToFirestore(updated)
        val opponentCaptain = _teams.value.find { it.id == challenge.challengedTeamId }?.captainId
        if (!opponentCaptain.isNullOrBlank()) {
            addNotification(
                userId = opponentCaptain,
                title = "Team Room Ready",
                message = "Room ID: " + roomId + " | Password: " + password,
                type = "MATCH"
            )
        }
        return Result.success(Unit)
    }

    fun cancelChallenge(challengeId: String): Result<Unit> {
        val challenge = _challenges.value.find { it.id == challengeId }
            ?: return Result.failure(Exception("Challenge not found."))
        if (challenge.status != ChallengeStatus.PENDING &&
            challenge.status != ChallengeStatus.ACCEPTED &&
            challenge.status != ChallengeStatus.ROOM_SET) {
            return Result.failure(Exception("This challenge cannot be cancelled."))
        }
        val myTeamId = _currentUser.value.teamId
            ?: return Result.failure(Exception("You are not in a team."))
        if (myTeamId != challenge.challengerTeamId && myTeamId != challenge.challengedTeamId) {
            return Result.failure(Exception("You are not part of this challenge."))
        }
        val updated = challenge.copy(status = ChallengeStatus.CANCELLED)
        _challenges.value = _challenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncChallengeToFirestore(updated)
        return Result.success(Unit)
    }

    // ==================== 1v1 User Challenges ====================

    /**
     * Step 1 — Challenger sends a 1v1 request. Requires at least CHALLENGE_STAKE in wallet.
     * No money is deducted yet; deduction happens when the opponent accepts.
     */
    fun sendUserChallenge(opponentUid: String, opponentName: String): Result<Unit> {
        val challenger = _currentUser.value
        if (opponentUid == challenger.uid) {
            return Result.failure(Exception("You cannot challenge yourself."))
        }
        val opponent = _users.value.find { it.uid == opponentUid }
            ?: return Result.failure(Exception("Opponent not found."))
        if (challenger.teamId != null && challenger.teamId == opponent.teamId) {
            return Result.failure(Exception("You cannot challenge your own team member."))
        }
        if (challenger.walletBalance < CHALLENGE_STAKE) {
            return Result.failure(Exception("Insufficient balance. You need at least ৳${CHALLENGE_STAKE.toInt()} to send a challenge."))
        }
        // Prevent duplicate open challenges between the same two players
        val alreadyOpen = _userChallenges.value.any {
            it.status == UserChallengeStatus.PENDING &&
                ((it.challengerUid == challenger.uid && it.opponentUid == opponentUid) ||
                    (it.challengerUid == opponentUid && it.opponentUid == challenger.uid))
        }
        if (alreadyOpen) {
            return Result.failure(Exception("You already have an open challenge with this player."))
        }

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
        addNotification(
            userId = opponentUid,
            title = "1v1 Challenge Received!",
            message = "${challenger.username} challenged you to a Free Fire 1v1 for ৳${CHALLENGE_STAKE.toInt()}.",
            type = "MATCH"
        )
        return Result.success(Unit)
    }

    /**
     * Step 2 — Opponent accepts. CHALLENGE_STAKE is deducted from BOTH wallets (total pool = 100 TK).
     */
    fun acceptUserChallenge(challengeId: String): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId }
            ?: return Result.failure(Exception("Challenge not found."))
        if (challenge.status != UserChallengeStatus.PENDING) {
            return Result.failure(Exception("This challenge is no longer pending."))
        }
        val accepter = _currentUser.value
        if (accepter.uid != challenge.opponentUid) {
            return Result.failure(Exception("Only the invited opponent can accept this challenge."))
        }
        if (accepter.walletBalance < CHALLENGE_STAKE) {
            return Result.failure(Exception("Insufficient balance. You need at least ৳${CHALLENGE_STAKE.toInt()} to accept."))
        }
        val challenger = _users.value.find { it.uid == challenge.challengerUid }
        if (challenger == null || challenger.walletBalance < CHALLENGE_STAKE) {
            return Result.failure(Exception("Challenger no longer has enough balance."))
        }

        // Deduct stake from both players
        if (!applyWalletChange(challenge.challengerUid, -CHALLENGE_STAKE)) {
            return Result.failure(Exception("Could not deduct challenger stake."))
        }
        if (!applyWalletChange(challenge.opponentUid, -CHALLENGE_STAKE)) {
            // Roll back the challenger deduction to stay consistent
            applyWalletChange(challenge.challengerUid, CHALLENGE_STAKE)
            return Result.failure(Exception("Could not deduct your stake."))
        }

        val updated = challenge.copy(status = UserChallengeStatus.ACCEPTED)
        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncUserChallengeToFirestore(updated)
        listOf(challenge.challengerUid to challenge.challengerName, challenge.opponentUid to accepter.username).forEach { (uid, name) ->
            addNotification(
                userId = uid,
                title = "Challenge Accepted — ৳${CHALLENGE_STAKE.toInt()} deducted",
                message = "$name, the 1v1 is on. The challenger will share the Room ID & password shortly.",
                type = "MATCH"
            )
        }
        return Result.success(Unit)
    }

    fun cancelUserChallenge(challengeId: String): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId }
            ?: return Result.failure(Exception("Challenge not found."))
        if (challenge.status != UserChallengeStatus.PENDING &&
            challenge.status != UserChallengeStatus.ACCEPTED &&
            challenge.status != UserChallengeStatus.ROOM_SET) {
            return Result.failure(Exception("This challenge cannot be cancelled now."))
        }
        val me = _currentUser.value.uid
        if (me != challenge.challengerUid && me != challenge.opponentUid) {
            return Result.failure(Exception("You are not part of this challenge."))
        }

        // If money was already locked after acceptance, refund both players on cancellation.
        if (challenge.status == UserChallengeStatus.ACCEPTED || challenge.status == UserChallengeStatus.ROOM_SET) {
            applyWalletChange(challenge.challengerUid, CHALLENGE_STAKE)
            applyWalletChange(challenge.opponentUid, CHALLENGE_STAKE)
        }

        val updated = challenge.copy(status = UserChallengeStatus.CANCELLED)
        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncUserChallengeToFirestore(updated)

        val otherUid = if (me == challenge.challengerUid) challenge.opponentUid else challenge.challengerUid
        addNotification(otherUid, "1v1 Challenge Cancelled", "The challenge was cancelled and any locked stake was refunded.", "MATCH")
        return Result.success(Unit)
    }

    /** Opponent rejects the challenge. Nothing is deducted. */
    fun rejectUserChallenge(challengeId: String) {
        val challenge = _userChallenges.value.find { it.id == challengeId } ?: return
        if (challenge.status != UserChallengeStatus.PENDING) return
        val updated = challenge.copy(status = UserChallengeStatus.REJECTED)
        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncUserChallengeToFirestore(updated)
        addNotification(
            userId = challenge.challengerUid,
            title = "Challenge Declined",
            message = "${challenge.opponentName} declined your 1v1 challenge.",
            type = "MATCH"
        )
    }

    /** Step 3 — Challenger submits the game Room ID & password; becomes visible to the opponent. */
    fun setUserChallengeRoom(challengeId: String, roomId: String, password: String): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId }
            ?: return Result.failure(Exception("Challenge not found."))
        if (challenge.status != UserChallengeStatus.ACCEPTED) {
            return Result.failure(Exception("Room can only be set after the challenge is accepted."))
        }
        if (_currentUser.value.uid != challenge.challengerUid) {
            return Result.failure(Exception("Only the challenger can set the room credentials."))
        }
        if (roomId.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Room ID and password are both required."))
        }
        val updated = challenge.copy(roomId = roomId.trim(), roomPassword = password.trim(), status = UserChallengeStatus.ROOM_SET)
        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncUserChallengeToFirestore(updated)
        addNotification(
            userId = challenge.opponentUid,
            title = "Room Ready — Join Now!",
            message = "Room ID: $roomId | Password: $password",
            type = "MATCH"
        )
        return Result.success(Unit)
    }

    /** Step 4 — A player uploads their end-match screenshot as proof. */
    fun submitUserChallengeProof(challengeId: String, proofUrl: String): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId }
            ?: return Result.failure(Exception("Challenge not found."))
        if (challenge.status != UserChallengeStatus.ROOM_SET && challenge.status != UserChallengeStatus.PROOF_SUBMITTED) {
            return Result.failure(Exception("Proof can only be submitted after the room is set."))
        }
        if (proofUrl.isBlank()) return Result.failure(Exception("Please upload a screenshot."))
        val me = _currentUser.value.uid

        val updated = when (me) {
            challenge.challengerUid -> challenge.copy(challengerProofUrl = proofUrl)
            challenge.opponentUid -> challenge.copy(opponentProofUrl = proofUrl)
            else -> return Result.failure(Exception("You are not part of this challenge."))
        }

        // Once both proofs are in, move to UNDER_REVIEW for admin moderation
        val bothSubmitted = updated.challengerProofUrl != null && updated.opponentProofUrl != null
        val finalStatus = if (bothSubmitted) UserChallengeStatus.UNDER_REVIEW
            else if (updated.status == UserChallengeStatus.ROOM_SET) UserChallengeStatus.PROOF_SUBMITTED
            else updated.status
        val toSave = updated.copy(status = finalStatus)

        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) toSave else it }
        FirebaseManager.syncUserChallengeToFirestore(toSave)
        return Result.success(Unit)
    }

    /**
     * Step 5 — Admin reviews both screenshots and declares the winner.
     * Winner is credited CHALLENGE_WINNER_PAYOUT (৳85); loser gets 0. Stats are updated.
     */
    fun declareUserChallengeWinner(challengeId: String, winnerUid: String): Result<Unit> {
        val challenge = _userChallenges.value.find { it.id == challengeId }
            ?: return Result.failure(Exception("Challenge not found."))
        if (challenge.status == UserChallengeStatus.COMPLETED) {
            return Result.failure(Exception("This challenge is already completed."))
        }
        if (winnerUid != challenge.challengerUid && winnerUid != challenge.opponentUid) {
            return Result.failure(Exception("Winner must be one of the two players."))
        }
        val loserUid = if (winnerUid == challenge.challengerUid) challenge.opponentUid else challenge.challengerUid
        val winnerName = if (winnerUid == challenge.challengerUid) challenge.challengerName else challenge.opponentName

        // Credit the winner (loser receives 0)
        applyWalletChange(winnerUid, CHALLENGE_WINNER_PAYOUT)

        val updated = challenge.copy(
            status = UserChallengeStatus.COMPLETED,
            winnerUid = winnerUid,
            winnerName = winnerName
        )
        _userChallenges.value = _userChallenges.value.map { if (it.id == challengeId) updated else it }
        FirebaseManager.syncUserChallengeToFirestore(updated)

        // Update win/loss stats for both players
        _users.value.forEach { u ->
            when (u.uid) {
                winnerUid -> upsertUser(u.copy(wins = u.wins + 1, matchesPlayed = u.matchesPlayed + 1, points = u.points + 3))
                loserUid -> upsertUser(u.copy(losses = u.losses + 1, matchesPlayed = u.matchesPlayed + 1))
            }
        }

        addNotification(winnerUid, "YOU WON! 🏆", "You won the 1v1 vs your opponent. ৳${CHALLENGE_WINNER_PAYOUT.toInt()} credited to your wallet.", "MATCH")
        addNotification(loserUid, "Match Result", "You lost the 1v1 challenge. Better luck next time!", "MATCH")
        logAdminAction("CHALLENGE_WINNER_DECLARED", "UserChallenge", challengeId, "Winner: $winnerName credited ৳${CHALLENGE_WINNER_PAYOUT.toInt()}")
        return Result.success(Unit)
    }

    /** Admin: remove/clean up a challenge document. */
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
            if (it.userId == uid) it.copy(isRead = true) else it
        }
    }

    private fun addNotification(userId: String, title: String, message: String, type: String) {
        val item = NotificationItem(userId = userId, title = title, message = message, type = type)
        _notifications.value = listOf(item) + _notifications.value
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