package com.tubes1.purritify.features.auth.presentation.login

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tubes1.purritify.R
import com.tubes1.purritify.core.common.navigation.Screen
import com.tubes1.purritify.core.common.navigation.isLandscape
import com.tubes1.purritify.features.auth.presentation.login.components.rememberImeState
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginPage(
    navController: NavController,
    loginStateViewModel: LoginStateViewModel = koinViewModel()
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val state = loginStateViewModel.state.collectAsState().value
    val imeState = rememberImeState()
    val scrollState = rememberScrollState()
    val showPassword = remember { mutableStateOf(false) }

    LaunchedEffect(imeState.value) {
        if (imeState.value) {
            scrollState.animateScrollTo(scrollState.maxValue, tween(300))
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
            loginStateViewModel.resetSuccess()
        }
    }

    if (isLandscape()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            // Background Image
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart)
            )

            // Logo dan judul
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Logo dan judul
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(Color(0xFF121212))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Jutaan lagu tersedia,",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "hanya di Purritify.",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp
                    )
                }

                Column {
                    // Form
                    Column(
                        modifier = Modifier
                            .padding(bottom = 30.dp)
                            .padding(horizontal = 32.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Email Input
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Email",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = email.value,
                                onValueChange = { email.value = it },
                                shape = RoundedCornerShape(8.dp),
                                placeholder = {
                                    Text(
                                        text = "Email",
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Email input" },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = TextFieldColors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    disabledTextColor = Color.Gray,
                                    errorTextColor = Color.Red,

                                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                                    disabledContainerColor = Color.White.copy(alpha = 0.15f),
                                    errorContainerColor = Color(0xFFFFEBEE),

                                    cursorColor = Color.White,
                                    errorCursorColor = Color.Red,

                                    textSelectionColors = TextSelectionColors(Color.White, Color.Black),

                                    focusedIndicatorColor = Color.LightGray,
                                    unfocusedIndicatorColor = Color.LightGray,
                                    disabledIndicatorColor = Color.Gray,
                                    errorIndicatorColor = Color.Red,

                                    focusedLeadingIconColor = Color.White,
                                    unfocusedLeadingIconColor = Color.LightGray,
                                    disabledLeadingIconColor = Color.Gray,
                                    errorLeadingIconColor = Color.Red,

                                    focusedTrailingIconColor = Color.White,
                                    unfocusedTrailingIconColor = Color.LightGray,
                                    disabledTrailingIconColor = Color.Gray,
                                    errorTrailingIconColor = Color.Red,

                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.LightGray,
                                    disabledLabelColor = Color.Gray,
                                    errorLabelColor = Color.Red,

                                    focusedPlaceholderColor = Color.LightGray,
                                    unfocusedPlaceholderColor = Color.Gray,
                                    disabledPlaceholderColor = Color.DarkGray,
                                    errorPlaceholderColor = Color.Red,

                                    focusedSupportingTextColor = Color.White,
                                    unfocusedSupportingTextColor = Color.LightGray,
                                    disabledSupportingTextColor = Color.Gray,
                                    errorSupportingTextColor = Color.Red,

                                    focusedPrefixColor = Color.White,
                                    unfocusedPrefixColor = Color.LightGray,
                                    disabledPrefixColor = Color.Gray,
                                    errorPrefixColor = Color.Red,

                                    focusedSuffixColor = Color.White,
                                    unfocusedSuffixColor = Color.LightGray,
                                    disabledSuffixColor = Color.Gray,
                                    errorSuffixColor = Color.Red
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Password Input
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Kata Sandi",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = password.value,
                                onValueChange = { password.value = it },
                                shape = RoundedCornerShape(8.dp),
                                placeholder = {
                                    Text(
                                        text = "Kata Sandi",
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Password input" },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                visualTransformation = if (showPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
                                colors = TextFieldColors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    disabledTextColor = Color.Gray,
                                    errorTextColor = Color.Red,

                                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                                    disabledContainerColor = Color.White.copy(alpha = 0.15f),
                                    errorContainerColor = Color(0xFFFFEBEE),

                                    cursorColor = Color.White,
                                    errorCursorColor = Color.Red,

                                    textSelectionColors = TextSelectionColors(Color.White, Color.Black),

                                    focusedIndicatorColor = Color.LightGray,
                                    unfocusedIndicatorColor = Color.LightGray,
                                    disabledIndicatorColor = Color.Gray,
                                    errorIndicatorColor = Color.Red,

                                    focusedLeadingIconColor = Color.White,
                                    unfocusedLeadingIconColor = Color.LightGray,
                                    disabledLeadingIconColor = Color.Gray,
                                    errorLeadingIconColor = Color.Red,

                                    focusedTrailingIconColor = Color.White,
                                    unfocusedTrailingIconColor = Color.LightGray,
                                    disabledTrailingIconColor = Color.Gray,
                                    errorTrailingIconColor = Color.Red,

                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.LightGray,
                                    disabledLabelColor = Color.Gray,
                                    errorLabelColor = Color.Red,

                                    focusedPlaceholderColor = Color.LightGray,
                                    unfocusedPlaceholderColor = Color.Gray,
                                    disabledPlaceholderColor = Color.DarkGray,
                                    errorPlaceholderColor = Color.Red,

                                    focusedSupportingTextColor = Color.White,
                                    unfocusedSupportingTextColor = Color.LightGray,
                                    disabledSupportingTextColor = Color.Gray,
                                    errorSupportingTextColor = Color.Red,

                                    focusedPrefixColor = Color.White,
                                    unfocusedPrefixColor = Color.LightGray,
                                    disabledPrefixColor = Color.Gray,
                                    errorPrefixColor = Color.Red,

                                    focusedSuffixColor = Color.White,
                                    unfocusedSuffixColor = Color.LightGray,
                                    disabledSuffixColor = Color.Gray,
                                    errorSuffixColor = Color.Red
                                ),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showPassword.value = !showPassword.value
                                    }) {
                                        Icon(
                                            imageVector = if (showPassword.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showPassword.value) "Hide password" else "Show password",
                                            tint = Color.White
                                        )
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = {
                                loginStateViewModel.sendLogin(email.value, password.value)
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier
                                .semantics { contentDescription = "Login button" }
                                .fillMaxWidth(),
                            colors = ButtonColors(
                                contentColor = Color.White,
                                containerColor = Color(0xFF1DB955),
                                disabledContentColor = Color.Black,
                                disabledContainerColor = Color.Green
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.semantics { contentDescription = "Loading to login" }
                                )
                            } else {
                                Text(
                                    text = "Login",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        // Pesan error
                        if (state.error.isNotBlank()) {
                            Text(
                                text = state.error,
                                color = Color(0xffff4242),
                                modifier = Modifier.padding(top = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        } else {
                            Text("")
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            // Background Image
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.Top)
                    .align(Alignment.TopStart)
            )

            // Logo dan judul
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo dan judul
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(Color(0xFF121212))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Jutaan lagu tersedia,",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "hanya di Purritify.",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp
                )
            }

            Spacer(modifier = Modifier.height(25.dp))

            // Form
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Email Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Email",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email.value,
                        onValueChange = { email.value = it },
                        shape = RoundedCornerShape(8.dp),
                        placeholder = {
                            Text(
                                text = "Email",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Email input" },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = TextFieldColors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.Gray,
                            errorTextColor = Color.Red,

                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                            disabledContainerColor = Color.White.copy(alpha = 0.15f),
                            errorContainerColor = Color(0xFFFFEBEE),

                            cursorColor = Color.White,
                            errorCursorColor = Color.Red,

                            textSelectionColors = TextSelectionColors(Color.White, Color.Black),

                            focusedIndicatorColor = Color.LightGray,
                            unfocusedIndicatorColor = Color.LightGray,
                            disabledIndicatorColor = Color.Gray,
                            errorIndicatorColor = Color.Red,

                            focusedLeadingIconColor = Color.White,
                            unfocusedLeadingIconColor = Color.LightGray,
                            disabledLeadingIconColor = Color.Gray,
                            errorLeadingIconColor = Color.Red,

                            focusedTrailingIconColor = Color.White,
                            unfocusedTrailingIconColor = Color.LightGray,
                            disabledTrailingIconColor = Color.Gray,
                            errorTrailingIconColor = Color.Red,

                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.LightGray,
                            disabledLabelColor = Color.Gray,
                            errorLabelColor = Color.Red,

                            focusedPlaceholderColor = Color.LightGray,
                            unfocusedPlaceholderColor = Color.Gray,
                            disabledPlaceholderColor = Color.DarkGray,
                            errorPlaceholderColor = Color.Red,

                            focusedSupportingTextColor = Color.White,
                            unfocusedSupportingTextColor = Color.LightGray,
                            disabledSupportingTextColor = Color.Gray,
                            errorSupportingTextColor = Color.Red,

                            focusedPrefixColor = Color.White,
                            unfocusedPrefixColor = Color.LightGray,
                            disabledPrefixColor = Color.Gray,
                            errorPrefixColor = Color.Red,

                            focusedSuffixColor = Color.White,
                            unfocusedSuffixColor = Color.LightGray,
                            disabledSuffixColor = Color.Gray,
                            errorSuffixColor = Color.Red
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Password Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Kata Sandi",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password.value,
                        onValueChange = { password.value = it },
                        shape = RoundedCornerShape(8.dp),
                        placeholder = {
                            Text(
                                text = "Kata Sandi",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Password input" },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (showPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = TextFieldColors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.Gray,
                            errorTextColor = Color.Red,

                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                            disabledContainerColor = Color.White.copy(alpha = 0.15f),
                            errorContainerColor = Color(0xFFFFEBEE),

                            cursorColor = Color.White,
                            errorCursorColor = Color.Red,

                            textSelectionColors = TextSelectionColors(Color.White, Color.Black),

                            focusedIndicatorColor = Color.LightGray,
                            unfocusedIndicatorColor = Color.LightGray,
                            disabledIndicatorColor = Color.Gray,
                            errorIndicatorColor = Color.Red,

                            focusedLeadingIconColor = Color.White,
                            unfocusedLeadingIconColor = Color.LightGray,
                            disabledLeadingIconColor = Color.Gray,
                            errorLeadingIconColor = Color.Red,

                            focusedTrailingIconColor = Color.White,
                            unfocusedTrailingIconColor = Color.LightGray,
                            disabledTrailingIconColor = Color.Gray,
                            errorTrailingIconColor = Color.Red,

                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.LightGray,
                            disabledLabelColor = Color.Gray,
                            errorLabelColor = Color.Red,

                            focusedPlaceholderColor = Color.LightGray,
                            unfocusedPlaceholderColor = Color.Gray,
                            disabledPlaceholderColor = Color.DarkGray,
                            errorPlaceholderColor = Color.Red,

                            focusedSupportingTextColor = Color.White,
                            unfocusedSupportingTextColor = Color.LightGray,
                            disabledSupportingTextColor = Color.Gray,
                            errorSupportingTextColor = Color.Red,

                            focusedPrefixColor = Color.White,
                            unfocusedPrefixColor = Color.LightGray,
                            disabledPrefixColor = Color.Gray,
                            errorPrefixColor = Color.Red,

                            focusedSuffixColor = Color.White,
                            unfocusedSuffixColor = Color.LightGray,
                            disabledSuffixColor = Color.Gray,
                            errorSuffixColor = Color.Red
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                showPassword.value = !showPassword.value
                            }) {
                                Icon(
                                    imageVector = if (showPassword.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showPassword.value) "Hide password" else "Show password",
                                    tint = Color.White
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        loginStateViewModel.sendLogin(email.value, password.value)
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .semantics { contentDescription = "Login button" }
                        .fillMaxWidth(),
                    colors = ButtonColors(
                        contentColor = Color.White,
                        containerColor = Color(0xFF1DB955),
                        disabledContentColor = Color.Black,
                        disabledContainerColor = Color.Green
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.semantics { contentDescription = "Loading to login" }
                        )
                    } else {
                        Text(
                            text = "Login",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }

                // Pesan error
                if (state.error.isNotBlank()) {
                    Text(
                        text = state.error,
                        color = Color(0xffff4242),
                        modifier = Modifier.padding(top = 8.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Text("")
                }
            }
        }
    }
}