@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.data.model.*
import com.example.ui.components.Avatar
import com.example.ui.components.LoadingOverlay
import com.example.ui.components.MatchStatusBadge
import com.example.ui.components.TournamentStatusBadge
import com.example.ui.components.borderStroke
import com.example.ui.components.rememberImagePicker
import com.example.data.firebase.StorageManager
import com.example.ui.screens.profile.ApkDownloadDialog
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen(
    tournaments: List<Tournament>,
    matches: List<MatchFixture>,
    payments: List<PaymentTransaction>,