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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.coroutines.launch
import pt.ipp.estg.readytomove.util.bitmapDescriptorFromVector
import pt.ipp.estg.readytomove.viewmodel.MapViewModel
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.ui.theme.Theme
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput


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

    val systemDark = isSystemInDarkTheme()
    var isDarkTheme by rememberSaveable { mutableStateOf(systemDark) }

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

    val mapProperties = remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = hasPermission,
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
            )
        )
    }

    val mapUiSettings = remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = false)) }

    val defaultPosition = LatLng(41.367, -8.194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 13f)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            AppDrawer(
                isDarkTheme = isDarkTheme,
                onToggleTheme = { enabled ->
                    isDarkTheme = enabled
                    AppCompatDelegate.setDefaultNightMode(
                        if (enabled) AppCompatDelegate.MODE_NIGHT_YES
                        else AppCompatDelegate.MODE_NIGHT_NO
                    )
                },
                onNavigate = { route ->
                    scope.launch {
                        drawerState.close()
                        navController.navigate(route)
                    }
                },
                onClose = {
                    scope.launch { drawerState.close() }
                }
            )
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

@Composable
private fun AppDrawer(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onClose: () -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.readytomove_symbol),
                    contentDescription = "Logo ReadyToMove",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f))
                        .padding(8.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "ReadyToMove",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        "Explora a cidade",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        NavigationDrawerItem(
            label = { Text("Mapa") },
            selected = false,
            onClick = { onNavigate("map") },
            icon = { Icon(Icons.Default.MyLocation, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text("Perfil") },
            selected = false,
            onClick = { onNavigate("profile") },
            icon = { Icon(Icons.Default.Person, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text("Leaderboard") },
            selected = false,
            onClick = { onNavigate("leaderboard") },
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text("Histórico de Viagens") },
            selected = false,
            onClick = { onNavigate("travel_history") },
            icon = { Icon(Icons.Default.History, contentDescription = null) }
        )

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        Spacer(modifier = Modifier.weight(1f))

        ThemeToggle(
            isDark = isDarkTheme,
            onToggle = { onToggleTheme(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun ThemeToggle(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackWidth = 120.dp
    val thumbSize = 40.dp
    val padding = 8.dp
    val endOffset = trackWidth - thumbSize - padding
    val animatedOffset by animateDpAsState(
        targetValue = if (isDark) endOffset else padding,
        label = ""
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .width(trackWidth)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(isDark) { detectTapGestures { onToggle(!isDark) } }
            .padding(horizontal = 0.dp, vertical = 8.dp)
    ) {
        Icon(
            Icons.Default.LightMode,
            contentDescription = null,
            tint = if (!isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )
        Icon(
            Icons.Default.DarkMode,
            contentDescription = null,
            tint = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )
        Box(
            modifier = Modifier
                .size(thumbSize)
                .offset(x = animatedOffset)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onToggle(!isDark) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
