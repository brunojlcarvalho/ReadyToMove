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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import pt.ipp.estg.readytomove.util.bitmapDescriptorFromVector
import pt.ipp.estg.readytomove.viewmodel.MapViewModel
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.ui.theme.Theme

enum class TripState {
    IDLE, RUNNING, PAUSED
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var tripState by remember { mutableStateOf(TripState.IDLE) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPermission = granted
            if (granted) {
                viewModel.getUserLocation()
                viewModel.loadOperators()
            }
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
                viewModel.loadOperators()
            }

            else -> permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val location by viewModel.currentLocation.collectAsState()
    val operatorPoints by viewModel.operatorPoints.collectAsState()

    val mapProperties = remember { mutableStateOf(MapProperties(isMyLocationEnabled = hasPermission)) }
    val mapUiSettings = remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = false)) }

    val defaultPosition = LatLng(41.367, -8.194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 13f)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp) // opcional: limitar largura da navbar
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    IconButton(onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate("map")
                        }
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "Fechar e ir para mapa")
                    }

                    Text(
                        "Menu ReadyToMove",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

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

                NavigationDrawerItem(
                    label = { Text("Travel History") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("travel_history")
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                    // .padding(padding),
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

                    operatorPoints.forEachIndexed { i, op ->
                        val iconRes = when (op.type.name) {
                            "BUS" -> R.drawable.ic_bus
                            "BIKE" -> R.drawable.ic_bike
                            else -> R.drawable.ic_scooter
                        }

                        val markerColor = when (op.type.name) {
                            "BUS" -> Theme.markerColors.bus.toArgb()
                            "BIKE" -> Theme.markerColors.bike.toArgb()
                            else -> Theme.markerColors.scooter.toArgb()
                        }

                        Marker(
                            state = MarkerState(LatLng(op.lat, op.lon)),
                            title = op.name,
                            snippet = "Ponto ${i + 1}",
                            icon = bitmapDescriptorFromVector(
                                context,
                                iconRes,
                                markerColor,
                                0.1f
                            )
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(padding)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (tripState) {
                        TripState.IDLE -> {
                            Button(
                                onClick = { tripState = TripState.RUNNING },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Iniciar Viagem")
                            }
                        }

                        TripState.RUNNING -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { tripState = TripState.PAUSED },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text("Pausar")
                                }
                                Button(
                                    onClick = { tripState = TripState.IDLE },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Parar")
                                }
                            }
                        }

                        TripState.PAUSED -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { tripState = TripState.RUNNING },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Retomar")
                                }
                                Button(
                                    onClick = { tripState = TripState.IDLE },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Parar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
