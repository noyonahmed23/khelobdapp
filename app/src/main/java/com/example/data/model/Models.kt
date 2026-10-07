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
    val isBanned: Boolean = false
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
    val bannerUrl: String = "",
    val gameLogoUrl: String = "",
    val gameMode: String = "Squad",
    val startAtMillis: Long = 0L,
    val registrationDeadlineAtMillis: Long = 0L,
    val description: String,
    val entryFee: Double = 0.0, // 0 for Free
    val prizePool: Double = 5000.0, // in BDT
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
    val firstPrize: Double = 3000.0,
    val secondPrize: Double = 1500.0,
    val thirdPrize: Double = 500.0,
    val championName: String? = null,
    // Room credentials distributed by admin to registered participants
    val roomId: String? = null,
    val roomPassword: String? = null,
    val roomVisible: Boolean = false
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
    val isLive: Boolean = true,
    val lastActiveAt: Long = System.currentTimeMillis(),
    val matches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val points: Int = 0,
    val isApproved: Boolean = true
) {
    val winRate: Float
        get() = if (matches > 0) (wins.toFloat() / matches) * 100f else 0f
}

enum class ChallengeStatus {
    PENDING,
    ACCEPTED,
    ROOM_SET,
    REJECTED,
    CANCELLED,
    SCHEDULED,
    LIVE,
    COMPLETED
}

data class TeamChallenge(
    val id: String = UUID.randomUUID().toString(),
    val challengerTeamId: String,
    val challengerTeamName: String,
    val challengedTeamId: String,
    val challengedTeamName: String,
    val game: String = "Free Fire",
    val stakeAmount: Double = 0.0,
    val scheduledTime: String = "Tomorrow, 08:00 PM",
    val status: ChallengeStatus = ChallengeStatus.PENDING,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val winnerTeamId: String? = null
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
    val type: String, // "WALLET_TOPUP", "TOURNAMENT_ENTRY", "PRIZE_PAYOUT"
    val status: PaymentStatus = PaymentStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val message: String,
    val type: String = "INFO", // MATCH, TOURNAMENT, PAYMENT, SYSTEM
    val isRead: Boolean = false,
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
    val autoAutomationEnabled: Boolean = true
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

enum class UserChallengeStatus {
    PENDING,
    ACCEPTED,
    ROOM_SET,
    PROOF_SUBMITTED,
    UNDER_REVIEW,
    COMPLETED,
    CANCELLED,
    REJECTED
}

data class UserChallenge(
    val id: String = UUID.randomUUID().toString(),
    val challengerUid: String = "",
    val challengerName: String = "",
    val opponentUid: String = "",
    val opponentName: String = "",
    val game: String = "Free Fire",
    val stakeAmount: Double = 50.0, // Fixed 50 TK each = 100 TK pool
    val status: UserChallengeStatus = UserChallengeStatus.PENDING,
    val roomId: String? = null,
    val roomPassword: String? = null,
    val challengerProofUrl: String? = null,
    val opponentProofUrl: String? = null,
    val winnerUid: String? = null,
    val winnerName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// ── Welcome Popup ────────────────────────────────────────────────────────────

data class WelcomePopupConfig(
    val isVisible: Boolean = false,
    val title: String = "Welcome to Khelo BD!",
    val message: String = "Bangladesh's #1 Esports Tournament Platform. Compete, Win & Earn daily.",
    val imageUrl: String = "",
    val buttonText: String = "Let's Play!"
)