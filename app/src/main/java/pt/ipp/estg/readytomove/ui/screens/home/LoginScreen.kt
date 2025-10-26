package pt.ipp.estg.readytomove.ui.screens.home;

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import pt.ipp.estg.readytomove.data.firebase.FirebaseAuthHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ReadyToMove 🚲") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (isRegisterMode) "Criar Conta" else "Iniciar Sessão",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(20.dp))

            // Campo de username apenas no modo de registo
            if (isRegisterMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        val user: FirebaseUser? = if (isRegisterMode) {
                            FirebaseAuthHelper.register(username, email, password)
                        } else {
                            FirebaseAuthHelper.login(email, password)
                        }

                        isLoading = false

                        if (user != null) {
                            navController.navigate("map") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            errorMessage = if (isRegisterMode)
                                "Falha ao registar. Verifica o email, password ou username."
                            else
                                "Falha ao iniciar sessão. Credenciais inválidas."
                        }
                    }
                },
                enabled = !isLoading &&
                        email.isNotBlank() &&
                        password.isNotBlank() &&
                        (!isRegisterMode || username.isNotBlank()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRegisterMode) "Registar" else "Entrar")
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                Text(
                    if (isRegisterMode)
                        "Já tens conta? Inicia sessão"
                    else
                        "Não tens conta? Cria uma"
                )
            }

            if (isLoading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
