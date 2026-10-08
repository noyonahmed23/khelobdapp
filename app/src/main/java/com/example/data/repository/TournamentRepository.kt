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
import java.util.Calendar

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
    
    // Team Challenge economics
    const val TEAM_CHALLENGE_MIN = 50.0
    const val TEAM_CHALLENGE_MAX = 200.0
    
    // Team withdrawal window
    const val TEAM_WITHDRAWAL_START_HOUR = 19   // 7 PM
    const val TEAM_WITHDRAWAL_END_HOUR = 23     // 11 PM

    init {
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

    // ════════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS FOR KHELO BD WORKFLOW
    // ════════════════════════════════════════════════════════════════

    /**
     * Check if team withdrawal is allowed (7 PM–11 PM window)
     */
    fun isTeamWithdrawalAllowed(): Boolean {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour in TEAM_WITHDRAWAL_START_HOUR until TEAM_WITHDRAWAL_END_HOUR
    }

    /**
     * Get Bengali message for team withdrawal restriction
     */
    fun getTeamWithdrawalMessage(): String {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        return if (currentHour < TEAM_WITHDRAWAL_START_HOUR) {
            "টিম উইথড্রোয়াল শুধুমাত্র সন্ধ্যা ৭টা থেকে রাত ১১টার মধ্যে উপলব্ধ। কৃপয়া পরে চেষ্টা করুন।"
        } else {
            "টিম উইথড্রোয়াল সুবিধা এখন বন্ধ। আগামীকাল সন্ধ্যা ৭টায় চেষ্টা করুন।"
        }
    }

    /**
     * Expire old 1v1 challenges after 10 minutes
     */
    fun cleanupExpiredChallenges() {
        val now = System.currentTimeMillis()
        val expired = _userChallenges.value.filter { 
            it.status == UserChallengeStatus.PENDING && 
            (now - it.timestamp) > (10 * 60 * 1000)  // 10 minutes
        }

        expired.forEach { challenge ->
            // Refund challenger stake if held
            if (challenge.challengerUid.isNotBlank()) {
                applyWalletChange(challenge.challengerUid, CHALLENGE_STAKE)
            }

            val updated = challenge.copy(status = UserChallengeStatus.EXPIRED)
            _userChallenges.value = _userChallenges.value.map { 
                if (it.id == challenge.id) updated else it 
            }
            FirebaseManager.syncUserChallengeToFirestore(updated)
            
            logAdminAction(
                "CHALLENGE_EXPIRED",
                "UserChallenge",
                challenge.id,
                "Challenge between ${challenge.challengerName} and ${challenge.opponentName} expired"
            )
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ROLE SWITCH & AUTHENTICATION
    // ════════════════════════════════════════════════════════════════

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
            return Result.failure(Exception("দয়া করে আপনার নাম প্রবেশ করুন।"))
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(Exception("দয়া করে একটি বৈধ ইমেল ঠিকানা প্রবেশ করুন।"))
        }
        if (password.length < 6) {
            return Result.failure(Exception("পাসওয়ার্ড অন্তত ৬ অক্ষর হতে হবে।"))
        }
        if (password != confirmPassword) {
            return Result.failure(Exception("পাসওয়ার্ড মিলে না! দয়া করে পুনরায় চেষ্টা করুন।"))
        }

        val existing = _users.value.find { it.email.equals(cleanEmail, ignoreCase = true) }
        if (existing != null) {
            return Result.failure(Exception("এই ইমেল দিয়ে একটি অ্যাকাউন্ট ইতিমধ্যে রয়েছে।"))
        }

        val username = cleanEmail.substringBefore("@").replace(".", "_")
        val newUser = UserProfile(
            email = cleanEmail,
            username = username,
            fullName = cleanName,
            role = UserRole.USER,
            walletBalance = 0.0
        )

        _users.value = _users.value + newUser
        _currentUser.value = newUser
        _isAuthenticated.value = true
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
            FirebaseManager.syncUserToFirestore(adminProfile)
            logAdminAction("ADMIN_LOGIN", "Auth", adminProfile.uid, "Admin Noyon authenticated successfully")
            return Result.success(adminProfile)
        }

        if (password.length < 4) {
            return Result.failure(Exception("পাসওয়ার্ড অন্তত ৪ অক্ষর হতে হবে।"))
        }

        // Mandatory authentication: an account must already exist.
        val existing = _users.value.find { it.username.equals(clean, ignoreCase = true) || it.email.equals(clean, ignoreCase = true) }
            ?: return Result.failure(Exception("'$clean' এর জন্য কোনো অ্যাকাউন্ট খুঁজে পাওয়া যায়নি। প্রথমে একটি অ্যাকাউন্ট তৈরি করুন।"))

        if (existing.isBanned) {
            return Result.failure(Exception("এই অ্যাকাউন্ট স্থগিত করা হয়েছে। সাপোর্টে যোগাযোগ করুন।"))
        }

        _currentUser.value = existing
        _isAuthenticated.value = true
        logAdminAction("USER_LOGIN", "Auth", existing.uid, "Player authenticated: ${existing.username}")
        return Result.success(existing)
    }

    fun logout() {
        _isAuthenticated.value = false
    }

    // ════════════════════════════════════════════════════════════════
    // PROFILE MANAGEMENT
    // ════════════════════════════════════════════════════════════════

    fun updateProfile(fullName: String, gameUid: String, preferredGame: String) {
        val updated = _currentUser.value.copy(
            fullName = fullName,
            inGameUid = gameUid,
            preferredGame = preferredGame
        )
        _currentUser.value = updated
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
        if (_currentUser.value.uid == user.uid) _currentUser.value = user
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

    // ════════════════════════════════════════════════════════════════
    // TOURNAMENT MANAGEMENT
    // ════════════════════════════════════════════════════════════════

    // Tournament Registration
    fun registerForTournament(tournament: Tournament, gameUid: String, paymentMethod: String, trxId: String): Result<String> {
        val user = _currentUser.value
        if (tournament.status != TournamentStatus.REGISTRATION) {
            return Result.failure(Exception("এই টুর্নামেন্টের জন্য নিবন্ধন বন্ধ।"))
        }
        val isAlreadyRegistered = _registrations.value.any { it.tournamentId == tournament.id && it.userId == user.uid }
        if (isAlreadyRegistered) {
            return Result.failure(Exception("আপনি ইতিমধ্যে এই টুর্নামেন্টে নিবন্ধিত!"))
        }

        if (tournament.entryFee > 0) {
            if (paymentMethod == "Wallet") {
                if (user.walletBalance < tournament.entryFee) {
                    return Result.failure(Exception("অপর্যাপ্ত ওয়ালেট ব্যালেন্স (৳${user.walletBalance.toInt()})। দয়া করে bKash বা Nagad এর মাধ্যমে জমা করুন।"))
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
            title = "নিবন্ধন নিশ্চিত!",
            message = "${tournament.title} এর জন্য আপনি নিশ্চিত। আমার ম্যাচগুলি চেক করুন।",
            type = "TOURNAMENT"
        )

        return Result.success("নিবন্ধন সফল!")
    }

    // ════════════════════════════════════════════════════════════════
    // AUTOMATION & CRON
    // ════════════════════════════════════════════════════════════════

    private fun runCronCycle() {
        cleanupExpiredChallenges()
        // Additional automation jobs here
    }

    // ════════════════════════════════════════════════════════════════
    // NOTIFICATIONS
    // ════════════════════════════════════════════════════════════════

    fun addNotification(userId: String, title: String, message: String, type: String, relatedId: String? = null) {
        val notif = NotificationItem(
            userId = userId,
            title = title,
            message = message,
            type = type,
            isRead = false,
            relatedId = relatedId
        )
        _notifications.value = listOf(notif) + _notifications.value
        FirebaseManager.syncNotificationToFirestore(notif)
    }

    fun markNotificationRead(notificationId: String) {
        _notifications.value = _notifications.value.map { notif ->
            if (notif.id == notificationId) {
                val updated = notif.copy(isRead = true)
                FirebaseManager.syncNotificationToFirestore(updated)
                updated
            } else notif
        }
    }

    fun getUnreadNotificationCount(userId: String): Int {
        return _notifications.value.count { it.userId == userId && !it.isRead }
    }

    // ════════════════════════════════════════════════════════════════
    // LOGGING
    // ════════════════════════════════════════════════════════════════

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
}
