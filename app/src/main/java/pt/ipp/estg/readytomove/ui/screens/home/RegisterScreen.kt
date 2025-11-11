package pt.ipp.estg.readytomove.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.viewmodel.AuthViewModel
import androidx.compose.ui.tooling.preview.Preview
import pt.ipp.estg.readytomove.ui.theme.ReadyToMoveTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    RegisterScreenContent(
        onRegister = { name, email, password, callback ->
            viewModel.registerUser(name, email, password) { success, error -> callback(success, error) }
        },
        onRegisterSuccess = onRegisterSuccess,
        onLoginClick = onLoginClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterScreenContent(
    onRegister: (String, String, String, (Boolean, String?) -> Unit) -> Unit,
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.readytomove_symbol),
            contentDescription = "Logo ReadyToMove",
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(12.dp))
        Text("ReadyToMove", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") })
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmar Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            if (password == confirmPassword) {
                onRegister(name, email, password) { success, error ->
                    if (success) onRegisterSuccess() else errorMessage = error
                }
            } else {
                errorMessage = "As passwords não coincidem"
            }
        }) {
            Text("Registar")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onLoginClick) { Text("Já tens conta? Inicia sessão") }

        errorMessage?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    ReadyToMoveTheme {
        RegisterScreenContent(
            onRegister = { _, _, _, cb -> cb(true, null) },
            onRegisterSuccess = {},
            onLoginClick = {}
        )
    }
}