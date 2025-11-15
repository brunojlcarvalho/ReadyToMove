package pt.ipp.estg.readytomove.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.ui.theme.Theme
import pt.ipp.estg.readytomove.util.bitmapDescriptorFromVector
import pt.ipp.estg.readytomove.viewmodel.MapViewModel

enum class TripState { IDLE, RUNNING, PAUSED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    windowSizeClass: WindowSizeClass
) {
    val width = windowSizeClass.widthSizeClass
    val height = windowSizeClass.heightSizeClass

    val isTablet = width >= WindowWidthSizeClass.Medium
    val isPortraitPhone = width == WindowWidthSizeClass.Compact &&
            height == WindowHeightSizeClass.Expanded

    val topBarIconSize = when {
        isTablet -> 36.dp
        isPortraitPhone -> 30.dp
        else -> 26.dp
    }

    val drawerWidth = when {
        isTablet -> 380.dp
        else -> 300.dp
    }

    val drawerHeaderHeight = when {
        isTablet -> 220.dp
        else -> 170.dp
    }

    val drawerIconSize = when {
        isTablet -> 72.dp
        else -> 64.dp
    }

    val bottomButtonHeight = when {
        isTablet -> 64.dp
        else -> 52.dp
    }

    val bottomPadding = when {
        isTablet -> 42.dp
        isPortraitPhone -> 32.dp
        else -> 24.dp
    }

    val mapButtonFontSize = when {
        isTablet -> 18.sp
        else -> 16.sp
    }

    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var tripState by remember { mutableStateOf(TripState.IDLE) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.getUserLocation()
            viewModel.loadOperators()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = true
            viewModel.getUserLocation()
            viewModel.loadOperators()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val location by viewModel.currentLocation.collectAsState()
    val operatorPoints by viewModel.operatorPoints.collectAsState()

    val mapProperties = remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = hasPermission,
                mapStyleOptions = try {
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
                } catch (e: Exception) {
                    null
                }
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
                onToggleTheme = onToggleTheme,
                onNavigate = { route ->
                    scope.launch {
                        drawerState.close()
                        navController.navigate(route)
                    }
                },
                onClose = { scope.launch { drawerState.close() } },
                drawerWidth = drawerWidth,
                drawerHeaderHeight = drawerHeaderHeight,
                drawerIconSize = drawerIconSize
            )
        }
    ) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(topBarIconSize)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize(),
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
                        Marker(state = MarkerState(position = userLatLng), title = "Tu estás aqui")
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
                            icon = bitmapDescriptorFromVector(context, iconRes, markerColor, 0.1f)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(padding)
                        .padding(bottom = bottomPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (tripState) {
                        TripState.IDLE -> {
                            Button(
                                onClick = { tripState = TripState.RUNNING },
                                modifier = Modifier
                                    .height(bottomButtonHeight)
                            ) {
                                Text("Iniciar Viagem", fontSize = mapButtonFontSize)
                            }
                        }
                        TripState.RUNNING -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { tripState = TripState.PAUSED },
                                    modifier = Modifier.height(bottomButtonHeight)
                                ) {
                                    Text("Pausar", fontSize = mapButtonFontSize)
                                }
                                Button(
                                    onClick = { tripState = TripState.IDLE },
                                    modifier = Modifier.height(bottomButtonHeight)
                                ) {
                                    Text("Parar", fontSize = mapButtonFontSize)
                                }
                            }
                        }
                        TripState.PAUSED -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { tripState = TripState.RUNNING },
                                    modifier = Modifier.height(bottomButtonHeight)
                                ) {
                                    Text("Retomar", fontSize = mapButtonFontSize)
                                }
                                Button(
                                    onClick = { tripState = TripState.IDLE },
                                    modifier = Modifier.height(bottomButtonHeight)
                                ) {
                                    Text("Parar", fontSize = mapButtonFontSize)
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
    onClose: () -> Unit,
    drawerWidth: Dp,
    drawerHeaderHeight: Dp,
    drawerIconSize: Dp
) {
    ModalDrawerSheet(modifier = Modifier.width(drawerWidth)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(drawerHeaderHeight)
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
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(drawerIconSize)
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

            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onPrimary)
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
        Spacer(Modifier.height(12.dp))
        Spacer(modifier = Modifier.weight(1f))

        ThemeToggle(
            isDark = isDarkTheme,
            onToggle = onToggleTheme,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun ThemeToggle(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackWidth = 72.dp
    val trackHeight = 32.dp
    val thumbSize = 28.dp
    val innerPad = 2.dp

    val travel = trackWidth - thumbSize - innerPad * 2
    val offsetX by animateDpAsState(
        targetValue = if (isDark) travel else 0.dp,
        label = ""
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val thumbColor = MaterialTheme.colorScheme.primary
    val activeIconTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    val inactiveIconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(trackColor)
            .clickable { onToggle(!isDark) }
            .padding(all = innerPad)
    ) {
        Icon(
            imageVector = Icons.Default.LightMode,
            contentDescription = null,
            tint = if (isDark) inactiveIconTint else activeIconTint,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(14.dp)
        )
        Icon(
            imageVector = Icons.Default.DarkMode,
            contentDescription = null,
            tint = if (isDark) activeIconTint else inactiveIconTint,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(14.dp)
        )

        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
                .clickable { onToggle(!isDark) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun fakeWindowSizeClass(
    width: WindowWidthSizeClass,
    height: WindowHeightSizeClass
): WindowSizeClass {
    val w = when (width) {
        WindowWidthSizeClass.Compact -> 360.dp
        WindowWidthSizeClass.Medium -> 800.dp
        WindowWidthSizeClass.Expanded -> 1200.dp
        else -> 360.dp
    }
    val h = when (height) {
        WindowHeightSizeClass.Compact -> 360.dp
        WindowHeightSizeClass.Medium -> 640.dp
        WindowHeightSizeClass.Expanded -> 900.dp
        else -> 640.dp
    }
    return WindowSizeClass.calculateFromSize(DpSize(w, h))
}

@Preview(showBackground = true, name = "Map - Phone")
@Composable
private fun MapScreenPreview_Phone() {
    var dark by remember { mutableStateOf(false) }
    MaterialTheme {
        val nav = rememberNavController()
        MapScreen(
            navController = nav,
            isDarkTheme = dark,
            onToggleTheme = { dark = it },
            windowSizeClass = fakeWindowSizeClass(
                WindowWidthSizeClass.Compact,
                WindowHeightSizeClass.Medium
            )
        )
    }
}

@Preview(showBackground = true, name = "Map - Tablet")
@Composable
private fun MapScreenPreview_Tablet() {
    var dark by remember { mutableStateOf(true) }
    MaterialTheme {
        val nav = rememberNavController()
        MapScreen(
            navController = nav,
            isDarkTheme = dark,
            onToggleTheme = { dark = it },
            windowSizeClass = fakeWindowSizeClass(
                WindowWidthSizeClass.Medium,
                WindowHeightSizeClass.Medium
            )
        )
    }
}
