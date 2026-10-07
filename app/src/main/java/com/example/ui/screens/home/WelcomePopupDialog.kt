package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.WelcomePopupConfig
import com.example.ui.components.borderStroke
import com.example.ui.theme.*

@Composable
fun WelcomePopupDialog(
    config: WelcomePopupConfig,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            color = DarkSurfaceElevated,
            border = borderStroke(1.dp, KheloGreen)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Banner image (if set)
                if (config.imageUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        AsyncImage(
                            model = config.imageUrl,
                            contentDescription = "Welcome Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Dark overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBg.copy(alpha = 0.5f), RectangleShape)
                        )
                        // Close button overlay
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Fallback header with icon
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(KheloGreenContainer, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = KheloGreenBright,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "KHELO ",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    "BD",
                                    color = KheloGreenBright,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Text content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = config.title,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = config.message,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onDismiss,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("welcome_popup_cta_btn")
                    ) {
                        Text(
                            text = config.buttonText,
                            color = DarkBg,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        shape = RectangleShape
                    ) {
                        Text(
                            "Dismiss",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
