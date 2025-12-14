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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.viewmodel.AuthViewModel

/**
 * The main Composable for the Login Screen.
 *
 * It serves as the entry point for authentication, orchestrating the interaction
 * between the UI layout and the [AuthViewModel]. It handles the login process
 * logic and navigation callbacks.
 *
 * @param viewModel The ViewModel responsible for authentication logic.
 * @param windowSizeClass The size class of the device window, used for adaptive layout adjustments.
 * @param onLoginSuccess Callback triggered when the login is successful.
 * @param onRegisterClick Callback triggered when the user taps "Create Account".
 */
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
            // Delegates the login operation to the ViewModel
            viewModel.loginUser(email, pass) { success, error ->
                cb(success, error)
            }
        },
        onLoginSuccess = onLoginSuccess,
        onRegisterClick = onRegisterClick
    )
}

/**
 * A private Composable that calculates layout parameters based on the device size.
 * It determines if the device is a Tablet, Portrait Phone, or Landscape Phone,
 * and passes these flags to the layout renderer.
 */
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

    // Determine device type for responsive design
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

/**
 * The actual UI layout implementation for the Login Screen.
 *
 * It renders the logo, input fields (Email/Password), and action buttons.
 * The sizes and spacings are dynamically adjusted based on the [isTablet]
 * and [isPortraitPhone] flags to ensure a good look on all devices.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenLayout(
    isTablet: Boolean,
    isPortraitPhone: Boolean,
    onLogin: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val context = LocalContext.current

    // --- Adaptive UI Dimensions ---
    // Adjusts width fraction, font sizes, and spacings based on device type
    val contentWidthFraction = when {
        isTablet -> 0.5f        // Use 50% width on tablets
        isPortraitPhone -> 0.9f // Use 90% width on phones
        else -> 0.75f           // Default (e.g., landscape phone)
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

    // --- State Management ---
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

        // App Logo
        Image(
            painter = painterResource(id = R.drawable.readytomove_symbol),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .fillMaxWidth(contentWidthFraction)
                .height(logoHeight),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(spacingLarge))

        // App Title
        Text(
            text = stringResource(R.string.app_name),
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(spacingLarge))

        // Input Form Container
        Column(
            modifier = Modifier.fillMaxWidth(contentWidthFraction),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email_label), fontSize = fieldFontSize) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_label), fontSize = fieldFontSize) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Login Button with Loading State
            Button(
                onClick = {
                    // Basic Validation
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = context.getString(R.string.error_fill_fields)
                        return@Button
                    }

                    isLoading = true
                    onLogin(email.trim(), password) { success, error ->
                        isLoading = false
                        if (success) {
                            errorMessage = null
                            onLoginSuccess()
                        } else {
                            errorMessage = context.getString(R.string.error_to_login)
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
                    Text(stringResource(R.string.login_button), fontSize = buttonFontSize)
                }
            }

            // Error Message Display
            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))

            // Register Link
            TextButton(onClick = onRegisterClick) {
                Text(stringResource(R.string.create_account), fontSize = if (isTablet) 16.sp else 14.sp)
            }
        }
    }
}

// --- PREVIEWS ---

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