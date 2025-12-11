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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipp.estg.readytomove.R
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
        onRegister = { name, email, pass, cb ->
            viewModel.registerUser(name, email, pass) { success, error ->
                cb(success, error)
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
    val isPortraitPhone = widthClass == WindowWidthSizeClass.Compact &&
            heightClass == WindowHeightSizeClass.Expanded

    RegisterScreenLayout(
        isTablet = isTablet,
        isPortraitPhone = isPortraitPhone,
        onRegister = onRegister,
        onRegisterSuccess = onRegisterSuccess,
        onLoginClick = onLoginClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterScreenLayout(
    isTablet: Boolean,
    isPortraitPhone: Boolean,
    onRegister: (String, String, String, (Boolean, String?) -> Unit) -> Unit,
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current

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
        isTablet -> 38.sp
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

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
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
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .fillMaxWidth(contentWidthFraction)
                .height(logoHeight),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(spacingLarge))

        // "Criar Conta"
        Text(
            text = stringResource(R.string.create_account),
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
                label = { Text(stringResource(R.string.name_label), fontSize = fieldFontSize) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email_label), fontSize = fieldFontSize) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_label), fontSize = fieldFontSize) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.confirm_password_label), fontSize = fieldFontSize) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                        errorMessage = context.getString(R.string.error_fill_fields)
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = context.getString(R.string.error_password_short)
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = context.getString(R.string.error_passwords_match)
                        return@Button
                    }
                    isLoading = true
                    onRegister(name.trim(), email.trim(), password) { success, error ->
                        isLoading = false
                        if (success) {
                            errorMessage = null
                            onRegisterSuccess()
                        } else {
                            errorMessage = error ?: context.getString(R.string.error_registering)
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
                    Text(stringResource(R.string.create_account), fontSize = buttonFontSize)
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onLoginClick) {
                Text(stringResource(R.string.already_have_account), fontSize = if (isTablet) 16.sp else 14.sp)
            }
        }
    }
}

@Preview(showBackground = true, name = "Register - Phone")
@Composable
private fun RegisterScreenPreview_Phone() {
    MaterialTheme {
        RegisterScreenLayout(
            isTablet = false,
            isPortraitPhone = true,
            onRegister = { _, _, _, cb -> cb(true, null) },
            onRegisterSuccess = {},
            onLoginClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Register - Tablet")
@Composable
private fun RegisterScreenPreview_Tablet() {
    MaterialTheme {
        RegisterScreenLayout(
            isTablet = true,
            isPortraitPhone = false,
            onRegister = { _, _, _, cb -> cb(true, null) },
            onRegisterSuccess = {},
            onLoginClick = {}
        )
    }
}