package pt.ipp.estg.readytomove.ui.screens.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.data.model.TripState
import pt.ipp.estg.readytomove.ui.theme.Theme
import pt.ipp.estg.readytomove.util.bitmapDescriptorFromVector
import pt.ipp.estg.readytomove.util.bitmapDescriptorFromVectorOriginal
import pt.ipp.estg.readytomove.util.rememberAmbientLightLevel
import pt.ipp.estg.readytomove.viewmodel.MapViewModel

/**
 * The main screen of the application containing the Google Map.
 *
 * This screen handles:
 * - Displaying the user's location and nearby operator points (Bus/Bike).
 * - Trip management (Start, Pause, Stop) via QR Code or manual interaction.
 * - Adaptive UI layout for Phones and Tablets.
 * - Permission handling for Location and Notifications.
 * - Ambient light detection to suggest Dark Mode.
 *
 * @param navController The navigation controller to handle screen transitions.
 * @param isDarkTheme Boolean indicating if the app is currently in Dark Mode.
 * @param onToggleTheme Callback to toggle the application theme.
 * @param windowSizeClass Class used to determine the device's screen size (Compact, Medium, Expanded).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    windowSizeClass: WindowSizeClass
) {
    // --- Responsive Layout Logic ---
    val width = windowSizeClass.widthSizeClass
    val height = windowSizeClass.heightSizeClass

    // Determine device type to adjust dimensions dynamically
    val isTablet = width >= WindowWidthSizeClass.Medium
    val isPortraitPhone = width == WindowWidthSizeClass.Compact &&
            height == WindowHeightSizeClass.Expanded

    // Define dimensions based on the device type
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

    // --- Context & ViewModel ---
    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel()
    val scope = rememberCoroutineScope()

    // --- State Management ---
    var hasPermission by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var tripState by remember { mutableStateOf(TripState.IDLE) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }

    // Collecting flows from ViewModel
    val location by viewModel.currentLocation.collectAsState()
    val operatorPoints by viewModel.operatorPoints.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val isTrackingState by viewModel.isTrackingState.collectAsState()

    // --- Light Sensor Logic ---
    val currentLightLux = rememberAmbientLightLevel()
    val isLowLight = currentLightLux < 10f
    var showLowLightMessage by remember { mutableStateOf(false) }

    // Sync local trip state with ViewModel state
    LaunchedEffect(isTrackingState) {
        tripState = if (isTrackingState) TripState.RUNNING else TripState.IDLE
    }

    // Show a message when low light is detected (e.g., entering a tunnel or night time)
    LaunchedEffect(isLowLight) {
        if (isLowLight) {
            showLowLightMessage = true
            delay(2000)
            showLowLightMessage = false
        } else {
            showLowLightMessage = false
        }
    }

    // Display Toast messages triggered by the ViewModel
    LaunchedEffect(uiMessage) {
        uiMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearUiMessage()
        }
    }

    // Automatically toggle theme based on light sensor (optional feature)
    LaunchedEffect(isLowLight) {
        if (isDarkTheme != isLowLight) {
            onToggleTheme(isLowLight)
        }
    }

    // --- Permissions Handling ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        // Notification permission is optional but recommended for Android 13+ foreground services
        // val notificationGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false

        if (locationGranted) {
            hasPermission = true
            viewModel.startLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Request POST_NOTIFICATIONS only on Android 13 (Tiramisu) and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            hasPermission = true
            viewModel.startLocationUpdates()
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // --- Map Configuration ---
    // Load custom JSON style for the map based on the current theme (Dark/Light)
    val mapProperties = remember(isDarkTheme, hasPermission) {
        val mapStyleRes = if (isDarkTheme) {
            R.raw.map_style_dark
        } else {
            R.raw.map_style
        }

        MapProperties(
            isMyLocationEnabled = hasPermission,
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(context, mapStyleRes)
            } catch (e: Exception) {
                null
            }
        )
    }

    // Disable default map UI controls to use custom buttons
    val mapUiSettings = remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            )
        )
    }

    val defaultPosition = LatLng(41.367, -8.194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 13f)
    }

    // --- UI Structure (Drawer + Scaffold) ---
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
                // Transparent TopBar just to hold the Menu button
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
                                contentDescription = stringResource(R.string.menu_desc),
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
                // --- Google Map Component ---
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    properties = mapProperties,
                    uiSettings = mapUiSettings.value,
                    cameraPositionState = cameraPositionState
                ) {
                    // Draw the route path if available
                    if (routePoints.isNotEmpty()) {
                        Polyline(
                            points = routePoints,
                            color = Color.Blue,
                            width = 10f
                        )
                    }

                    // User Location Marker
                    location?.let {
                        val userLatLng = LatLng(it.latitude, it.longitude)

                        Marker(
                            state = MarkerState(position = userLatLng),
                            title = stringResource(R.string.me_marker_title),
                            anchor = Offset(0.5f, 0.5f),
                            icon = bitmapDescriptorFromVectorOriginal(context, R.drawable.ic_user_puck),
                            zIndex = 1.0f
                        )

                        // Camera Animation Logic
                        LaunchedEffect(it) {
                            if (!hasCenteredOnUser) {
                                // Initial center
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
                                    durationMs = 1000
                                )
                                hasCenteredOnUser = true
                            } else if (tripState == TripState.RUNNING) {
                                // Follow user during trip
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLng(userLatLng),
                                    durationMs = 500
                                )
                            }
                        }
                    }

                    // Operator Points Markers (Bus/Bike)
                    val pointPrefix = stringResource(R.string.point_marker_snippet)
                    operatorPoints.forEachIndexed { i, op ->
                        val iconRes = when (op.type.name) {
                            "BUS" -> R.drawable.ic_bus
                            "BIKE" -> R.drawable.ic_bike
                            else -> R.drawable.ic_scooter
                        }
                        // Adjust marker color based on theme
                        val markerColor = if (isDarkTheme) {
                            when (op.type.name) {
                                "BUS" -> Color(0xFF90CAF9).toArgb()
                                "BIKE" -> Color(0xFFA5D6A7).toArgb()
                                else -> Color(0xFFFFF59D).toArgb()
                            }
                        } else {
                            when (op.type.name) {
                                "BUS" -> Theme.markerColors.bus.toArgb()
                                "BIKE" -> Theme.markerColors.bike.toArgb()
                                else -> Theme.markerColors.scooter.toArgb()
                            }
                        }
                        Marker(
                            state = MarkerState(LatLng(op.lat, op.lon)),
                            title = op.name,
                            snippet = "$pointPrefix ${i + 1}",
                            icon = bitmapDescriptorFromVector(context, iconRes, markerColor, 0.1f)
                        )
                    }
                }

                // --- Floating Action Button (Center Map) ---
                FloatingActionButton(
                    onClick = {
                        location?.let { loc ->
                            scope.launch {
                                cameraPositionState.animate(
                                    update = CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude),
                                        16f
                                    ),
                                    durationMs = 1000
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 65.dp, end = 16.dp)
                        .size(40.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.center_map_desc),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // --- Bottom Controls Area ---
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(padding)
                        .padding(bottom = bottomPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Low Light warning bubble
                    if (showLowLightMessage) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            contentColor = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.night_mode_sensor), fontSize = 12.sp)
                            }
                        }
                    }

                    // Trip State Controls
                    when (tripState) {
                        TripState.IDLE -> {
                            Button(
                                onClick = {
                                    // 1. Open ML Kit Camera Scanner
                                    scanQrCode(context) { qrResult ->
                                        // 2. If successful, pass data to ViewModel to validate and start
                                        viewModel.startTripWithQr(qrResult)
                                    }
                                },
                                modifier = Modifier.height(bottomButtonHeight)
                            ) {
                                Text(
                                    stringResource(R.string.start_trip),
                                    fontSize = mapButtonFontSize
                                )
                            }
                        }
                        TripState.RUNNING -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        tripState = TripState.PAUSED
                                    },
                                    modifier = Modifier.height(bottomButtonHeight)
                                ) {
                                    Text(
                                        stringResource(R.string.pause_trip),
                                        fontSize = mapButtonFontSize
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.stopTrip()
                                    },
                                    modifier = Modifier.height(bottomButtonHeight)
                                ) {
                                    Text(
                                        stringResource(R.string.stop_trip),
                                        fontSize = mapButtonFontSize
                                    )
                                }
                            }
                        }
                        TripState.PAUSED -> {
                            Button(
                                onClick = { tripState = TripState.RUNNING },
                                modifier = Modifier.height(bottomButtonHeight)
                            ) {
                                Text(
                                    stringResource(R.string.resume_trip),
                                    fontSize = mapButtonFontSize
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The side navigation drawer for the application.
 *
 * @param isDarkTheme Current theme state.
 * @param onToggleTheme Callback to switch theme.
 * @param onNavigate Callback to handle navigation to different routes.
 * @param onClose Callback to close the drawer.
 * @param drawerWidth Width of the drawer based on device size.
 * @param drawerHeaderHeight Height of the header area.
 * @param drawerIconSize Size of the icons in the header.
 */
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
        // Drawer Header
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
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        stringResource(R.string.explore_city),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }

            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_drawer_desc),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Navigation Items
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_map)) },
            selected = false,
            onClick = { onNavigate("map") },
            icon = { Icon(Icons.Default.MyLocation, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_profile)) },
            selected = false,
            onClick = { onNavigate("profile") },
            icon = { Icon(Icons.Default.Person, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.leaderboard_title)) },
            selected = false,
            onClick = { onNavigate("leaderboard") },
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) }
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_travel_history)) },
            selected = false,
            onClick = { onNavigate("travel_history") },
            icon = { Icon(Icons.Default.History, contentDescription = null) }
        )

        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.height(12.dp))
        Spacer(modifier = Modifier.weight(1f))

        // Theme Toggle at the bottom
        ThemeToggle(
            isDark = isDarkTheme,
            onToggle = onToggleTheme,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

/**
 * A custom animated toggle switch for Day/Night theme.
 *
 * @param isDark Current state of the theme.
 * @param onToggle Callback when the user clicks the toggle.
 * @param modifier Modifier for styling.
 */
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

/**
 * Helper function to create a fake WindowSizeClass for Previews.
 */
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

/**
 * Initiates the Google ML Kit QR Code Scanner.
 *
 * @param context The current context.
 * @param onSuccess Callback function invoked with the QR code raw value upon successful scan.
 */
fun scanQrCode(context: Context, onSuccess: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAutoZoom()
        .build()

    val scanner = GmsBarcodeScanning.getClient(context, options)

    scanner.startScan()
        .addOnSuccessListener { barcode ->
            barcode.rawValue?.let { onSuccess(it) }
        }
        .addOnFailureListener {
            Toast.makeText(context, context.getString(R.string.scanner_error), Toast.LENGTH_SHORT).show()
        }
}