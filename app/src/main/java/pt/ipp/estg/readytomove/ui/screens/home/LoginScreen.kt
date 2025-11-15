package pt.ipp.estg.readytomove.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    windowSizeClass: WindowSizeClass,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    LoginScreenContent(
        windowSizeClass = windowSizeClass,
        onLogin = { email, pass, cb ->
            viewModel.loginUser(email, pass) { success, error ->
                cb(success, error)
            }
        },
        onLoginSuccess = onLoginSuccess,
        onRegisterClick = onRegisterClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenContent(
    windowSizeClass: WindowSizeClass,
    onLogin: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass

    val isTablet = widthClass >= WindowWidthSizeClass.Medium
    val isPortraitPhone = widthClass == WindowWidthSizeClass.Compact &&
            heightClass == WindowHeightSizeClass.Expanded

    LoginScreenLayout(
        isTablet = isTablet,
        isPortraitPhone = isPortraitPhone,
        onLogin = onLogin,
        onLoginSuccess = onLoginSuccess,
        onRegisterClick = onRegisterClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenLayout(
    isTablet: Boolean,
    isPortraitPhone: Boolean,
    onLogin: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val contentWidthFraction = when {
        isTablet -> 0.5f
        isPortraitPhone -> 0.9f
        else -> 0.75f
    }
    val logoHeight = when {
        isTablet -> 220.dp
        isPortraitPhone -> 150.dp
        else -> 130.dp
    }
    val titleFontSize = when {
        isTablet -> 40.sp
        isPortraitPhone -> 30.sp
        else -> 26.sp
    }
    val fieldFontSize = when {
        isTablet -> 18.sp
        else -> 14.sp
    }
    val buttonFontSize = when {
        isTablet -> 20.sp
        else -> 16.sp
    }
    val spacingLarge = when {
        isTablet -> 32.dp
        isPortraitPhone -> 20.dp
        else -> 16.dp
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isTablet) 32.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.readytomove_symbol),
            contentDescription = "Logo ReadyToMove",
            modifier = Modifier
                .fillMaxWidth(contentWidthFraction)
                .height(logoHeight),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(spacingLarge))

        Text(
            text = "ReadyToMove",
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(spacingLarge))

        Column(
            modifier = Modifier.fillMaxWidth(contentWidthFraction),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", fontSize = fieldFontSize) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", fontSize = fieldFontSize) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Preenche todos os campos"
                        return@Button
                    }
                    isLoading = true
                    onLogin(email.trim(), password) { success, error ->
                        isLoading = false
                        if (success) {
                            errorMessage = null
                            onLoginSuccess()
                        } else {
                            errorMessage = error ?: "Erro ao iniciar sessão"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(if (isTablet) 28.dp else 20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Iniciar Sessão", fontSize = buttonFontSize)
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onRegisterClick) {
                Text("Criar conta", fontSize = if (isTablet) 16.sp else 14.sp)
            }
        }
    }
}

@Preview(showBackground = true, name = "Login - Phone")
@Composable
private fun LoginScreenPreview_Phone() {
    MaterialTheme {
        LoginScreenLayout(
            isTablet = false,
            isPortraitPhone = true,
            onLogin = { _, _, cb -> cb(true, null) },
            onLoginSuccess = {},
            onRegisterClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Login - Tablet")
@Composable
private fun LoginScreenPreview_Tablet() {
    MaterialTheme {
        LoginScreenLayout(
            isTablet = true,
            isPortraitPhone = false,
            onLogin = { _, _, cb -> cb(true, null) },
            onLoginSuccess = {},
            onRegisterClick = {}
        )
    }
}
