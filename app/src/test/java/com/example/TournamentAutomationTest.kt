package com.example

import com.example.data.model.*
import com.example.data.repository.TournamentRepository
import org.junit.Assert.*
import org.junit.Test

class TournamentAutomationTest {

    @Test
    fun testAdminNoyonAuthentication() {
        val result = TournamentRepository.authenticate("adminnoyon", "Noyon@1234")
        assertTrue(result.isSuccess)
        val admin = result.getOrThrow()
        assertEquals("adminnoyon", admin.username)
        assertEquals(UserRole.SUPER_ADMIN, admin.role)
        assertTrue(TournamentRepository.isAuthenticated.value)
    }

    @Test
    fun testRegistrationAndWalletBalanceCheck() {
        val user = TournamentRepository.currentUser.value
        val openTournament = TournamentRepository.tournaments.value.first { it.status == TournamentStatus.REGISTRATION }

        val regResult = TournamentRepository.registerForTournament(
            tournament = openTournament,
            gameUid = "FF-TEST-1234",
            paymentMethod = "Wallet",
            trxId = ""
        )

        // If user already registered in seeds or newly registered, handle properly
        if (regResult.isSuccess) {
            val registrations = TournamentRepository.registrations.value
            assertTrue(registrations.any { it.tournamentId == openTournament.id && it.userId == user.uid })
        } else {
            // Already registered check
            assertTrue(regResult.exceptionOrNull()?.message?.contains("already registered") == true)
        }
    }

    @Test
    fun testAutomationFixturesGeneration() {
        val t1 = TournamentRepository.tournaments.value.first { it.id == "tour_ff_01" }
        TournamentRepository.automateGenerateGroupsAndFixtures(t1.id)

        val updatedTour = TournamentRepository.tournaments.value.first { it.id == t1.id }
        assertEquals(TournamentStatus.GROUP_STAGE, updatedTour.status)

        val standings = TournamentRepository.standings.value.filter { it.tournamentId == t1.id }
        assertTrue("Standings must be populated", standings.isNotEmpty())

        val fixtures = TournamentRepository.matches.value.filter { it.tournamentId == t1.id }
        assertTrue("Fixtures must be generated", fixtures.isNotEmpty())
    }

    @Test
    fun testPaymentApprovalCreditsWallet() {
        val user = TournamentRepository.currentUser.value
        val initialBalance = user.walletBalance

        TournamentRepository.requestDeposit(amount = 300.0, method = "bKash", trxId = "TEST-TRX-9988")
        val pendingTrx = TournamentRepository.payments.value.first { it.transactionId == "TEST-TRX-9988" }
        assertEquals(PaymentStatus.PENDING, pendingTrx.status)

        TournamentRepository.approvePayment(pendingTrx.id)

        val approvedTrx = TournamentRepository.payments.value.first { it.id == pendingTrx.id }
        assertEquals(PaymentStatus.PAID, approvedTrx.status)

        val updatedUser = TournamentRepository.currentUser.value
        assertEquals(initialBalance + 300.0, updatedUser.walletBalance, 0.01)
    }

    @Test
    fun testRoomCredentialsDistribution() {
        val match = TournamentRepository.matches.value.first()
        TournamentRepository.setRoomCredentials(match.id, "ROOM_999", "PASS_777")

        val updatedMatch = TournamentRepository.matches.value.first { it.id == match.id }
        assertEquals("ROOM_999", updatedMatch.roomId)
        assertEquals("PASS_777", updatedMatch.roomPassword)
        assertEquals(MatchStatus.ROOM_READY, updatedMatch.status)
    }
}
