package pt.ipp.estg.readytomove.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import pt.ipp.estg.readytomove.viewmodel.ProfileViewModel
import androidx.compose.ui.tooling.preview.Preview
import pt.ipp.estg.readytomove.ui.theme.ReadyToMoveTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val viewModel: ProfileViewModel = viewModel()
    val user by viewModel.user.collectAsState()

    if (user == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("O meu Perfil") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    } else {
        ProfileScreenScaffold(
            userName = user!!.name,
            userEmail = user!!.email,
            avatarUrl = user!!.avatarUrl,
            points = user!!.points,
            totalTrips = user!!.totalTrips,
            onBack = { navController.navigateUp() },
            onUpdateName = { viewModel.updateName(it) },
            onLogout = {
                viewModel.logout()
                navController.navigate("login") {
                    popUpTo("map") { inclusive = true }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreenScaffold(
    userName: String,
    userEmail: String,
    avatarUrl: String?,
    points: Int,
    totalTrips: Int,
    onBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onLogout: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("O meu Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Novo nome") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    if (tempName.isNotBlank()) onUpdateName(tempName)
                    isEditing = false
                }) { Text("Guardar") }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        tempName = userName
                        isEditing = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar nome")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(userEmail, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard("Pontos", points.toString())
                StatCard("Viagens", totalTrips.toString())
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Sair")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Terminar sessão")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    ReadyToMoveTheme {
        ProfileScreenScaffold(
            userName = "Maria Silva",
            userEmail = "maria@example.com",
            avatarUrl = null,
            points = 1200,
            totalTrips = 42,
            onBack = {},
            onUpdateName = {},
            onLogout = {}
        )
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier.size(120.dp, 100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(text = title, textAlign = TextAlign.Center)
        }
    }
}
