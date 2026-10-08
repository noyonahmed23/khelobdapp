package com.example.data.model

import java.util.UUID

enum class UserRole {
    USER,
    ADMIN,
    SUPER_ADMIN
}

data class UserProfile(
    val uid: String = UUID.randomUUID().toString(),
    val email: String = "",
    val username: String = "",
    val fullName: String = "",
    val role: UserRole = UserRole.USER,
    val avatarUrl: String = "",
    val profilePictureUrl: String = "",
    val profileBannerUrl: String = "",
    val preferredGame: String = "Free Fire",
    val inGameUid: String = "",
    val teamId: String? = null,
    val teamName: String? = null,
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val points: Int = 0,
    val tournamentWins: Int = 0,
    val walletBalance: Double = 0.0, // Default ৳0.00 wallet balance
    val referralCode: String = "KHELO-${UUID.randomUUID().toString().take(6).uppercase()}",
    val referredBy: String? = null,
    val referralEarnings: Double = 0.0,
    val isBanned: Boolean = false,
    val isOnline: Boolean = false,
    val lastActiveAt: Long = System.currentTimeMillis()
) {
    val winRate: Float
        get() = if (matchesPlayed > 0) (wins.toFloat() / matchesPlayed) * 100f else 0f
}

enum class TournamentStatus {
    DRAFT,
    REGISTRATION,
    REGISTRATION_CLOSED,
    GROUP_STAGE,
    KNOCKOUT,
    FINAL,
    COMPLETED,
    CANCELLED
}

data class Tournament(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val game: String,
    val mode: String = "Solo BR",
    val bannerUrl: String = "",
    val description: String,
    val entryFee: Double = 0.0, // 0 for Free
    val prizePool: Double = 5000.0, // in BDT
    val firstPrize: Double = 3000.0,
    val secondPrize: Double = 1500.0,
    val thirdPrize: Double = 500.0,
    val maxParticipants: Int = 16,
    val registeredCount: Int = 0,
    val registrationDeadline: String = "Today, 08:00 PM",
    val startDate: String = "Tonight",
    val startTime: String = "09:00 PM",
    val format: String = "Group Stage + Knockout",
    val numGroups: Int = 2,
    val qualifiersPerGroup: Int = 2,
    val matchDurationMinutes: Int = 20,
    val status: TournamentStatus = TournamentStatus.REGISTRATION,
    val rules: String = "1. Emulators strictly prohibited.\n2. Hacks/Cheats result in immediate lifetime ban.\n3. Take screenshot of victory screen for proof.\n4. Join room within 5 mins of schedule.",
    val championName: String? = null,
    // Room credentials distributed by admin to registered participants
    val roomId: String? = null,
    val roomPassword: String? = null,
    val roomVisible: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RegistrationStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}

data class TournamentRegistration(
    val id: String = UUID.randomUUID().toString(),
    val tournamentId: String,
    val tournamentTitle: String,
    val userId: String,
    val userName: String,
    val teamId: String? = null,
    val teamName: String? = null,
    val gameUid: String,
    val paymentMethod: String = "Wallet", // Wallet, bKash, Nagad
    val transactionId: String = "",
    val amount: Double = 0.0,
    val status: RegistrationStatus = RegistrationStatus.CONFIRMED,
    val timestamp: Long = System.currentTimeMillis()
)

data class GameCategory(
    val id: String = UUID.randomUUID().toString(),
    val game: String, // Free Fire, PUBG Mobile, DLS, eFootball
    val modes: List<String> = emptyList(), // Solo BR, Duo BR, Squad BR, etc.
    val logoUrl: String = "",
    val bannerUrl: String = ""
)

data class TeamMember(
    val userId: String,
    val username: String,
    val gameUid: String,
    val isCaptain: Boolean = false
)

data class Team(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tag: String,
    val logoUrl: String = "",
    val bannerUrl: String = "",
    val profileImageUrl: String = "",
    val captainId: String,
    val captainName: String,
    val members: List<TeamMember> = emptyList(),
    val joinRequests: List<String> = emptyList(),
    val matches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val points: Int = 0,
    val isApproved: Boolean = true,
    val walletBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val winRate: Float
        get() = if (matches > 0) (wins.toFloat() / matches) * 100f else 0f
    
    val isLive: Boolean
        get() = members.isNotEmpty() && members.filter { !it.isCaptain }.size >= 1 // At least captain + 1 member
}

enum class ChallengeStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    SCHEDULED,
    LIVE,
    COMPLETED,
    EXPIRED
}

data class TeamChallenge(
    val id: String = UUID.randomUUID().toString(),
    val challengerTeamId: String,
    val challengerTeamName: String,
    val challengedTeamId: String,
    val challengedTeamName: String,
    val game: String = "Free Fire",
    val mode: String = "Squad BR",
    val stakeAmount: Double = 0.0,
    val scheduledTime: String = "Tomorrow, 08:00 PM",
    val status: ChallengeStatus = ChallengeStatus.PENDING,
    val winnerTeamId: String? = null,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val challengerProofUrl: String? = null,
    val challengedProofUrl: String? = null,
    val expiresAt: Long = System.currentTimeMillis() + (10 * 60 * 1000), // 10 minutes
    val createdAt: Long = System.currentTimeMillis()
)

enum class MatchStatus {
    SCHEDULED,
    WAITING,
    ROOM_READY,
    LIVE,
    RESULT_PENDING,
    UNDER_REVIEW,
    VERIFIED,
    COMPLETED,
    CANCELLED,
    DISPUTED
}

data class MatchFixture(
    val id: String = UUID.randomUUID().toString(),
    val tournamentId: String,
    val tournamentTitle: String,
    val game: String = "Free Fire",
    val groupName: String? = "Group A", // null for knockout
    val round: String = "Round 1", // Round 1, Quarter-Final, Semi-Final, Final
    val participantAId: String,
    val participantAName: String,
    val participantBId: String,
    val participantBName: String,
    val scheduledTime: String,
    val status: MatchStatus = MatchStatus.SCHEDULED,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val scoreA: Int? = null,
    val scoreB: Int? = null,
    val winnerId: String? = null,
    val winnerName: String? = null,
    val proofUrl: String? = null,
    val submittedBy: String? = null,
    val isKnockout: Boolean = false,
    val disputeReason: String? = null
)

data class GroupStanding(
    val id: String = UUID.randomUUID().toString(),
    val tournamentId: String,
    val groupName: String,
    val participantId: String,
    val participantName: String,
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val pointsScored: Int = 0,
    val pointsConceded: Int = 0,
    val points: Int = 0, // Win=3, Draw=1, Loss=0
    val isQualified: Boolean = false
) {
    val pointDiff: Int
        get() = pointsScored - pointsConceded
}

enum class PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    CANCELLED
}

data class PaymentTransaction(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,
    val amount: Double,
    val method: String, // "bKash", "Nagad", "Rocket", "Wallet"
    val senderNumber: String = "", // Player's bKash/Nagad/Rocket account number
    val transactionId: String,
    val type: String, // "WALLET_TOPUP", "TOURNAMENT_ENTRY", "PRIZE_PAYOUT", "TEAM_DEPOSIT", "TEAM_WITHDRAWAL"
    val status: PaymentStatus = PaymentStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val message: String,
    val type: String = "INFO", // MATCH, TOURNAMENT, PAYMENT, SYSTEM, CHALLENGE
    val isRead: Boolean = false,
    val relatedId: String? = null, // Challenge ID, Match ID, etc.
    val timestamp: Long = System.currentTimeMillis()
)

data class AdminActivityLog(
    val id: String = UUID.randomUUID().toString(),
    val adminId: String,
    val adminName: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SystemSettings(
    val bkashNumber: String = "01700123456 (Personal)",
    val nagadNumber: String = "01900654321 (Personal)",
    val referralBonus: Double = 50.0,
    val minWithdrawal: Double = 100.0,
    val maintenanceMode: Boolean = false,
    val autoAutomationEnabled: Boolean = true,
    val teamWithdrawalStartHour: Int = 19, // 7 PM
    val teamWithdrawalEndHour: Int = 23 // 11 PM
)

data class WorkerStatus(
    val isRunning: Boolean = true,
    val lastRunTimestamp: Long = System.currentTimeMillis(),
    val totalRunsCount: Int = 0,
    val activeJobsCount: Int = 0,
    val lastSummary: String = "Worker initialized and monitoring matches & tournaments."
)

// ── 1v1 User Challenges ──────────────────────────────────────────────────────

enum class ChallengePurpose {
    TEAM,
    PLAYER
}

enum class ChallengeRule {
    REGULAR,
    HEADSHOT_ONLY
}

enum class UserChallengeStatus {
    PENDING,
    ACCEPTED,
    ROOM_SET,
    PROOF_SUBMITTED,
    UNDER_REVIEW,
    COMPLETED,
    CANCELLED,
    REJECTED,
    EXPIRED
}

data class UserChallenge(
    val id: String = UUID.randomUUID().toString(),
    val challengerUid: String = "",
    val challengerName: String = "",
    val challengerImageUrl: String = "",
    val opponentUid: String = "",
    val opponentName: String = "",
    val opponentImageUrl: String = "",
    val game: String = "Free Fire",
    val mode: String = "Squad BR",
    val map: String = "Bermuda",
    val rule: ChallengeRule = ChallengeRule.REGULAR,
    val note: String = "",
    val stakeAmount: Double = 50.0, // 50-200 BDT
    val status: UserChallengeStatus = UserChallengeStatus.PENDING,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val challengerProofUrl: String? = null,
    val opponentProofUrl: String? = null,
    val winnerUid: String? = null,
    val winnerName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (10 * 60 * 1000), // 10 minutes default
    val acceptedAt: Long? = null,
    val roomSetAt: Long? = null,
    val proofDeadlineAt: Long? = null,
    val completedAt: Long? = null
)

// ── Welcome Popup ─────────────────────────────────────────────────────────

data class WelcomePopupConfig(
    val isVisible: Boolean = false,
    val title: String = "স্বাগতম Khelo BD এ!",
    val message: String = "বাংলাদেশের সেরা ই-স্পোর্টস প্ল্যাটফর্ম। প্রতিদিন প্রতিযোগিতা করুন, জিতুন এবং আয় করুন।",
    val imageUrl: String = "",
    val buttonText: String = "খেলা শুরু করুন!"
)

// ── Bengali Localization Helper ──────────────────────────────────────────────

object BengaliStrings {
    const val INSUFFICIENT_BALANCE = "অপর্যাপ্ত ব্যালেন্স। অনুগ্রহ করে আপনার ওয়ালেট টপ-আপ করুন।"
    const val INSUFFICIENT_BALANCE_WITHDRAWAL = "আপনার টিম ওয়ালেটে পর্যাপ্ত টাকা নেই।"
    const val WITHDRAWAL_TIME_RESTRICTED = "টিম উইথড্রোয়াল শুধুমাত্র সন্ধ্যা ৭টা থেকে রাত ১১টার মধ্যে সম্ভব। আপনার স্থানীয় সময় চেক করুন।"
    const val CHALLENGE_EXPIRED = "এই চ্যালেঞ্জ মেয়াদোত্তীর্ণ হয়েছে।"
    const val CHALLENGE_ACCEPTED = "চ্যালেঞ্জ গৃহীত! চ্যালেঞ্জার শীঘ্রই রুম আইডি শেয়ার করবে।"
    const val ROOM_CREDENTIALS_REQUIRED = "রুম আইডি এবং পাসওয়ার্ড উভয়ই প্রয়োজনীয়।"
    const val PROOF_SUBMITTED = "প্রমাণ সাবমিট করা হয়েছে। প্রশাসক দ্বারা যাচাইয়ের জন্য অপেক্ষা করছে।"
    const val TEAM_MINIMUM_MEMBERS = "টিম চ্যালেঞ্জের জন্য কমপক্ষে ২ জন সক্রিয় সদস্য প্রয়োজন।"
    const val CHALLENGE_AMOUNT_RANGE = "চ্যালেঞ্জ পরিমাণ ৫০ থেকে ২০০ টাকার মধ্যে হতে হবে।"
    const val WARNING_CHALLENGE = "সতর্কতা: একবার চ্যালেঞ্জ গৃহীত হলে, উভয় খেলোয়াড়ের ওয়ালেট থেকে টাকা কেটে নেওয়া হবে।"
    const val CANCELLED_CHALLENGE = "চ্যালেঞ্জ বাতিল করা হয়েছে এবং আপনার টাকা ফেরত দেওয়া হয়েছে।"
}
