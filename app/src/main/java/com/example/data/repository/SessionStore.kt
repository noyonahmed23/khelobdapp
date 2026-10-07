package com.example.data.repository

import android.content.Context
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.FirebaseApp

object SessionStore {

    private const val PREFS_NAME = "khelo_bd_session"
    private const val KEY_ACTIVE = "active"
    private const val KEY_UID = "uid"
    private const val KEY_EMAIL = "email"
    private const val KEY_USERNAME = "username"
    private const val KEY_FULL_NAME = "fullName"
    private const val KEY_ROLE = "role"
    private const val KEY_PREFERRED_GAME = "preferredGame"
    private const val KEY_GAME_UID = "inGameUid"
    private const val KEY_WALLET = "walletBalance"
    private const val KEY_PROFILE_IMAGE = "profilePictureUrl"
    private const val KEY_BANNER_IMAGE = "profileBannerUrl"
    private const val KEY_TEAM_ID = "teamId"
    private const val KEY_TEAM_NAME = "teamName"
    private const val KEY_MATCHES = "matchesPlayed"
    private const val KEY_WINS = "wins"
    private const val KEY_LOSSES = "losses"
    private const val KEY_DRAWS = "draws"
    private const val KEY_POINTS = "points"
    private const val KEY_TOURNAMENT_WINS = "tournamentWins"
    private const val KEY_REFERRAL = "referralCode"
    private const val KEY_BANNED = "isBanned"

    private val prefs by lazy {
        runCatching {
            FirebaseApp
                .getInstance()
                .applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )
        }.getOrNull()
    }

    fun save(user: UserProfile) {
        prefs?.edit()
            ?.putBoolean(KEY_ACTIVE, true)
            ?.putString(KEY_UID, user.uid)
            ?.putString(KEY_EMAIL, user.email)
            ?.putString(KEY_USERNAME, user.username)
            ?.putString(KEY_FULL_NAME, user.fullName)
            ?.putString(KEY_ROLE, user.role.name)
            ?.putString(KEY_PREFERRED_GAME, user.preferredGame)
            ?.putString(KEY_GAME_UID, user.inGameUid)
            ?.putString(KEY_WALLET, user.walletBalance.toString())
            ?.putString(KEY_PROFILE_IMAGE, user.profilePictureUrl)
            ?.putString(KEY_BANNER_IMAGE, user.profileBannerUrl)
            ?.putString(KEY_TEAM_ID, user.teamId ?: "")
            ?.putString(KEY_TEAM_NAME, user.teamName ?: "")
            ?.putInt(KEY_MATCHES, user.matchesPlayed)
            ?.putInt(KEY_WINS, user.wins)
            ?.putInt(KEY_LOSSES, user.losses)
            ?.putInt(KEY_DRAWS, user.draws)
            ?.putInt(KEY_POINTS, user.points)
            ?.putInt(KEY_TOURNAMENT_WINS, user.tournamentWins)
            ?.putString(KEY_REFERRAL, user.referralCode)
            ?.putBoolean(KEY_BANNED, user.isBanned)
            ?.apply()
    }

    fun currentUid(): String? {
        return prefs
            ?.getString(KEY_UID, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun restore(): UserProfile? {
        val p = prefs ?: return null

        if (!p.getBoolean(KEY_ACTIVE, false)) {
            return null
        }

        val uid = p.getString(KEY_UID, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val role = runCatching {
            UserRole.valueOf(
                p.getString(
                    KEY_ROLE,
                    UserRole.USER.name
                ) ?: UserRole.USER.name
            )
        }.getOrDefault(UserRole.USER)

        return UserProfile(
            uid = uid,
            email = p.getString(KEY_EMAIL, "") ?: "",
            username = p.getString(KEY_USERNAME, "") ?: "",
            fullName = p.getString(KEY_FULL_NAME, "") ?: "",
            role = role,
            preferredGame = p.getString(
                KEY_PREFERRED_GAME,
                "Free Fire"
            ) ?: "Free Fire",
            inGameUid = p.getString(KEY_GAME_UID, "") ?: "",
            walletBalance = p.getString(
                KEY_WALLET,
                "0"
            )?.toDoubleOrNull() ?: 0.0,
            profilePictureUrl = p.getString(
                KEY_PROFILE_IMAGE,
                ""
            ) ?: "",
            profileBannerUrl = p.getString(
                KEY_BANNER_IMAGE,
                ""
            ) ?: "",
            teamId = p.getString(KEY_TEAM_ID, "")
                ?.takeIf { it.isNotBlank() },
            teamName = p.getString(KEY_TEAM_NAME, "")
                ?.takeIf { it.isNotBlank() },
            matchesPlayed = p.getInt(KEY_MATCHES, 0),
            wins = p.getInt(KEY_WINS, 0),
            losses = p.getInt(KEY_LOSSES, 0),
            draws = p.getInt(KEY_DRAWS, 0),
            points = p.getInt(KEY_POINTS, 0),
            tournamentWins = p.getInt(
                KEY_TOURNAMENT_WINS,
                0
            ),
            referralCode = p.getString(
                KEY_REFERRAL,
                "KHELO-USER"
            ) ?: "KHELO-USER",
            isBanned = p.getBoolean(
                KEY_BANNED,
                false
            )
        )
    }

    fun clear() {
        prefs?.edit()
            ?.clear()
            ?.apply()
    }
}
