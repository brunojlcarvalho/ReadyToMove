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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.ui.theme.ReadyToMoveTheme
import pt.ipp.estg.readytomove.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    windowSizeClass: WindowSizeClass,
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    RegisterScreenContent(
        windowSizeClass = windowSizeClass,
        onRegister = { name, email, password, callback ->
            viewModel.registerUser(name, email, password) { success, error ->
                callback(success, error)
            }
        },
        onRegisterSuccess = onRegisterSuccess,
        onLoginClick = onLoginClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterScreenContent(
    windowSizeClass: WindowSizeClass,
    onRegister: (String, String, String, (Boolean, String?) -> Unit) -> Unit,
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass

    val isTablet = widthClass >= WindowWidthSizeClass.Medium
    val isPortraitPhone = widthClass == WindowWidthSizeClass.Compact && heightClass == WindowHeightSizeClass.Expanded

    val contentWidthFraction = when {
        isTablet -> 0.5f
        isPortraitPhone -> 0.9f
        else -> 0.75f
    }

    val logoHeight = when {
        isTablet -> 240.dp
        isPortraitPhone -> 160.dp
        else -> 140.dp
    }

    val titleFontSize = when {
        isTablet -> 40.sp
        isPortraitPhone -> 28.sp
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

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome", fontSize = fieldFontSize) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", fontSize = fieldFontSize) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", fontSize = fieldFontSize) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar Password", fontSize = fieldFontSize) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (password == confirmPassword) {
                        onRegister(name, email, password) { success, error ->
                            if (success) onRegisterSuccess() else errorMessage = error
                        }
                    } else {
                        errorMessage = "As passwords não coincidem"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registar", fontSize = buttonFontSize)
            }

            errorMessage?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onLoginClick) {
                Text("Já tens conta? Inicia sessão")
            }
        }
    }
}
