package pt.ipp.estg.readytomove.ui.screens.travel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.data.model.Trip
import pt.ipp.estg.readytomove.viewmodel.TravelHistoryViewModel

/**
 * Screen that displays the detailed information of a specific trip.
 *
 * This screen shows:
 * - Total points earned and the date of the trip.
 * - Start and End locations with timestamps.
 * - A static map view visualizing the route taken.
 * - Statistics such as total distance and duration.
 * - An option to share the trip summary via SMS.
 *
 * @param navController Controller for navigation interactions (e.g., going back).
 * @param tripId The unique identifier of the trip to display.
 * @param viewModel The ViewModel containing the trip data.
 * @param windowSizeClass Used for adaptive layout decisions (Tablet vs Phone).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    navController: NavHostController,
    tripId: String,
    viewModel: TravelHistoryViewModel,
    windowSizeClass: WindowSizeClass
) {
    // Observe the list of trips from the ViewModel
    val trips by viewModel.trips.collectAsState()

    // Find the specific trip by ID.
    // 'remember' ensures we don't re-search the list on every recomposition unless 'trips' changes.
    val trip = remember(trips, tripId) {
        trips.find { it.id == tripId }
    }

    // --- Responsive Layout Calculations ---
    val widthClass = windowSizeClass.widthSizeClass
    val isTablet = widthClass >= WindowWidthSizeClass.Medium
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.trip_details_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_description)
                        )
                    }
                },
                actions = {
                    // Show Share button only if trip data is available
                    if (trip != null) {
                        IconButton(onClick = { shareTripViaSMS(context, trip) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share_sms_desc)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (trip == null) {
            // Error/Loading State: If trip is not found or list is empty
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (trips.isEmpty()) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.trip_not_found))
                }
            }
        } else {
            // Success State: Display content
            TripDetailsContent(
                trip = trip,
                modifier = Modifier.padding(padding),
                isTablet = isTablet
            )
        }
    }
}

/**
 * Helper function to create an intent for sharing trip details via SMS.
 *
 * @param context The current context to launch the activity.
 * @param trip The trip object containing the data to share.
 */
private fun shareTripViaSMS(context: Context, trip: Trip) {
    val distanceStr = String.format("%.2f km", trip.distanceKm)

    // Format the message using a string resource template
    val message = context.getString(
        R.string.share_trip_message,
        trip.date,
        distanceStr,
        trip.duration,
        trip.points
    )

    // Create an intent with ACTION_SENDTO to target SMS apps specifically
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:") // Only SMS apps should handle this
        putExtra("sms_body", message)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * The content section of the details screen.
 *
 * It parses the route string to coordinates and renders the Google Map
 * and statistical cards.
 */
@Composable
private fun TripDetailsContent(
    trip: Trip,
    modifier: Modifier = Modifier,
    isTablet: Boolean
) {
    // Parse the route string (format: "lat,lon;lat,lon") into a List of LatLng objects.
    // If parsing fails or string is empty, returns an empty list.
    val routePoints = remember(trip.routePath) {
        if (trip.routePath.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                trip.routePath.split(";").mapNotNull {
                    val parts = it.split(",")
                    if (parts.size == 2) {
                        LatLng(parts[0].toDouble(), parts[1].toDouble())
                    } else null
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(if (isTablet) 32.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header Card (Points & Date)
        PointsHeaderCard(points = trip.points, date = trip.date)

        Text(
            stringResource(R.string.trip_route_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Start and End Location Cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LocationInfoCard(
                title = stringResource(R.string.trip_start_label),
                address = trip.startLocation ?: stringResource(R.string.unknown_location),
                time = trip.startTimeFormatted,
                icon = Icons.Default.TripOrigin,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )

            LocationInfoCard(
                title = stringResource(R.string.trip_end_label),
                address = trip.endLocation ?: stringResource(R.string.unknown_location),
                time = trip.endTimeFormatted.ifEmpty { stringResource(R.string.trip_ongoing) },
                icon = Icons.Default.LocationOn,
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
        }

        // Map Section
        if (routePoints.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                // Initialize camera on the first point
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(routePoints.first(), 15f)
                }

                // Adjust camera to fit the entire route bounds once loaded
                LaunchedEffect(routePoints) {
                    if (routePoints.isNotEmpty()) {
                        val boundsBuilder = LatLngBounds.builder()
                        routePoints.forEach { boundsBuilder.include(it) }
                        try {
                            cameraPositionState.move(
                                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100)
                            )
                        } catch (e: Exception) {
                            // Handle potential layout/bounds errors silently
                        }
                    }
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    // Disable gestures to keep it as a static preview
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        scrollGesturesEnabled = false,
                        zoomGesturesEnabled = true,
                        rotationGesturesEnabled = false,
                        tiltGesturesEnabled = false,
                        myLocationButtonEnabled = false
                    )
                ) {
                    // Draw the path
                    Polyline(
                        points = routePoints,
                        color = Color.Blue,
                        width = 12f
                    )

                    // Start Marker (Green)
                    Marker(
                        state = MarkerState(position = routePoints.first()),
                        title = stringResource(R.string.trip_start_label),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )

                    // End Marker (Red)
                    Marker(
                        state = MarkerState(position = routePoints.last()),
                        title = stringResource(R.string.trip_end_label),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }
        } else {
            // Fallback UI if map data is missing
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.map_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 4. Statistics Section
        Text(
            stringResource(R.string.statistics_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Adaptive layout for stats cards
        if (isTablet) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailCard(
                    title = stringResource(R.string.distance_label),
                    value = String.format("%.2f km", trip.distanceKm),
                    icon = Icons.Default.Straighten,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                DetailCard(
                    title = stringResource(R.string.duration_label),
                    value = trip.duration,
                    icon = Icons.Default.Timer,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            DetailCard(
                title = stringResource(R.string.distance_label),
                value = String.format("%.2f km", trip.distanceKm),
                icon = Icons.Default.Straighten,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
            DetailCard(
                title = stringResource(R.string.duration_label),
                value = trip.duration,
                icon = Icons.Default.Timer,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * A visually prominent card displaying the points earned and the trip date.
 */
@Composable
private fun PointsHeaderCard(points: Int, date: String) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "$points ${stringResource(R.string.points_label)}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * A generic card to display a statistic with an icon, title, and value.
 */
@Composable
private fun DetailCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * A card specialized for displaying location information (Start/End points).
 */
@Composable
private fun LocationInfoCard(
    title: String,
    address: String,
    time: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}