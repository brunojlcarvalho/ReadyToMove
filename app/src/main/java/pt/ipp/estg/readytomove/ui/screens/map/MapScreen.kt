package pt.ipp.estg.readytomove.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import pt.ipp.estg.readytomove.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission = granted
            if (granted) viewModel.getUserLocation()
        }
    )

    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                hasPermission = true
                viewModel.getUserLocation()
            }

            else -> permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val location by viewModel.currentLocation.collectAsState()
    val mapProperties = remember { mutableStateOf(MapProperties(isMyLocationEnabled = hasPermission)) }
    val mapUiSettings = remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = false)) }

    val defaultPosition = LatLng(41.367, -8.194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 13f)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Menu ReadyToMove",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Perfil") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("profile")
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Leaderboard") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("leaderboard")
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ReadyToMove 🌱") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { viewModel.getUserLocation() }) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Centrar localização")
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    properties = mapProperties.value,
                    uiSettings = mapUiSettings.value,
                    cameraPositionState = cameraPositionState
                ) {
                    location?.let {
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        Marker(
                            state = MarkerState(position = userLatLng),
                            title = "Tu estás aqui"
                        )
                        LaunchedEffect(it) {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
                                durationMs = 800
                            )
                        }
                    }

                    val operators = listOf(
                        LatLng(41.368, -8.195),
                        LatLng(41.372, -8.191),
                        LatLng(41.365, -8.198)
                    )

                    operators.forEachIndexed { i, pos ->
                        Marker(
                            state = MarkerState(position = pos),
                            title = "Operador ${i + 1}",
                            snippet = "Trotinetas disponíveis"
                        )
                    }
                }
            }
        }
    }
}
