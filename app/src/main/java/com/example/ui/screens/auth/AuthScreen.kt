package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.TournamentRepository
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    onLoginSuccess: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }

    // Login state
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    // Register state (Name, Email, Password, Confirm Password)
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBg,
        shape = RectangleShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Flat Brand Logo Badge (Strictly Sharp, No Rounded Corners, No Gradient)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(KheloGreen, RectangleShape)
                    .border(1.dp, KheloGreenBright, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "K",
                    color = DarkBg,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "KHELO ",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "BD",
                    color = KheloGreenBright,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "ESPORTS TOURNAMENT PLATFORM",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Form Box (Strictly Rectangular, No Border Radius)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "CREATE PLAYER ACCOUNT" else "PLAYER LOGIN",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    if (isRegisterMode) {
                        // Registration: Full Name
                        OutlinedTextField(
                            value = regName,
                            onValueChange = {
                                regName = it
                                errorMessage = null
                            },
                            label = { Text("Full Name") },
                            placeholder = { Text("Enter your full name") },
                            singleLine = true,
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KheloGreen,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = KheloGreenBright,
                                unfocusedLabelColor = TextMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input")
                        )

                        // Registration: Email
                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = {
                                regEmail = it
                                errorMessage = null
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("Enter your email") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KheloGreen,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = KheloGreenBright,
                                unfocusedLabelColor = TextMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_email_input")
                        )

                        // Registration: Password
                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = {
                                regPassword = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("At least 6 characters") },
                            singleLine = true,
                            shape = RectangleShape,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password",
                                        tint = TextMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KheloGreen,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = KheloGreenBright,
                                unfocusedLabelColor = TextMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_password_input")
                        )

                        // Registration: Confirm Password
                        OutlinedTextField(
                            value = regConfirmPassword,
                            onValueChange = {
                                regConfirmPassword = it
                                errorMessage = null
                            },
                            label = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter password") },
                            singleLine = true,
                            shape = RectangleShape,
                            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle confirm password",
                                        tint = TextMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KheloGreen,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = KheloGreenBright,
                                unfocusedLabelColor = TextMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input")
                        )
                    } else {
                        // Login: Username or Email
                        OutlinedTextField(
                            value = loginUsername,
                            onValueChange = {
                                loginUsername = it
                                errorMessage = null
                            },
                            label = { Text("Username or Email") },
                            placeholder = { Text("Enter your username or email") },
                            singleLine = true,
                            shape = RectangleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KheloGreen,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = KheloGreenBright,
                                unfocusedLabelColor = TextMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_username_input")
                        )

                        // Login: Password
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = {
                                loginPassword = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password") },
                            singleLine = true,
                            shape = RectangleShape,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password",
                                        tint = TextMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KheloGreen,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = KheloGreenBright,
                                unfocusedLabelColor = TextMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = Color(0xFF2E1212),
                            shape = RectangleShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = EsportsRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Main Sign In / Create Account Button
                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                if (regName.isBlank() || regEmail.isBlank() || regPassword.isBlank() || regConfirmPassword.isBlank()) {
                                    errorMessage = "Please fill in all registration fields."
                                    return@Button
                                }
                                if (regPassword != regConfirmPassword) {
                                    errorMessage = "Passwords do not match! Please check and try again."
                                    return@Button
                                }
                                isLoading = true
                                val result = TournamentRepository.registerUser(regName, regEmail, regPassword, regConfirmPassword)
                                isLoading = false
                                if (result.isSuccess) {
                                    onLoginSuccess(result.getOrThrow())
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Registration failed."
                                }
                            } else {
                                if (loginUsername.isBlank() || loginPassword.isBlank()) {
                                    errorMessage = "Please enter username and password."
                                    return@Button
                                }
                                isLoading = true
                                val result = TournamentRepository.authenticate(loginUsername, loginPassword)
                                isLoading = false
                                if (result.isSuccess) {
                                    onLoginSuccess(result.getOrThrow())
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Login failed."
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = KheloGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("auth_submit_button")
                    ) {
                        Text(
                            text = if (isRegisterMode) "Create Account" else "Sign In",
                            color = DarkBg,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }

                    // Toggle Register / Login
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRegisterMode) "Already have an account?" else "New player?",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        TextButton(
                            onClick = {
                                isRegisterMode = !isRegisterMode
                                errorMessage = null
                            },
                            shape = RectangleShape
                        ) {
                            Text(
                                text = if (isRegisterMode) "Sign In" else "Create Account",
                                color = KheloGreenBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Khelo BD Esports Bangladesh • 100% Secure & Automated",
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
