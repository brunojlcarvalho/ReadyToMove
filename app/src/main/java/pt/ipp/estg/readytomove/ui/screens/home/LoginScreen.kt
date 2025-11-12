package pt.ipp.estg.readytomove.ui.screens.home

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    windowSizeClass: WindowSizeClass,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Log.d("LoginScreen", "Window Size Class: ${windowSizeClass.widthSizeClass}")
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    val contentWidthFraction = if (isTablet) 0.5f else 0.9f
    val logoHeight = if (isTablet) 240.dp else 170.dp
    val titleFontSize = if (isTablet) 40.sp else 28.sp
    val spacingLarge = if (isTablet) 32.dp else 16.dp
    val spacingMedium = if (isTablet) 32.dp else 20.dp

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
            "ReadyToMove",
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(spacingMedium))

        LoginForm(
            modifier = Modifier.fillMaxWidth(contentWidthFraction),
            onLogin = { email, password, callback ->
                viewModel.loginUser(email, password) { success, error -> callback(success, error) }
            },
            onLoginSuccess = onLoginSuccess,
            onRegisterClick = onRegisterClick
        )
    }
}

@Composable
private fun LoginForm(
    modifier: Modifier,
    onLogin: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                onLogin(email, password) { success, error ->
                    if (success) onLoginSuccess() else errorMessage = error
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        errorMessage?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRegisterClick) {
            Text("Ainda não tens conta? Regista-te")
        }
    }
}
