package pt.ipp.estg.readytomove.ui.screens.travel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.viewmodel.TravelHistoryViewModel
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelHistoryScreen(navController: NavHostController, windowSizeClass: WindowSizeClass, viewModel: TravelHistoryViewModel = viewModel()) {
    val viewModel: TravelHistoryViewModel = viewModel()
    val trips by viewModel.trips.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserTrips()
    }

    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass

    val isTablet = widthClass >= WindowWidthSizeClass.Medium
    val isPortraitPhone = widthClass == WindowWidthSizeClass.Compact &&
            heightClass == WindowHeightSizeClass.Expanded

    val pagePadding = when {
        isTablet -> 32.dp
        isPortraitPhone -> 22.dp
        else -> 16.dp
    }
    val contentPaddingBottom = when {
        isTablet -> 32.dp
        isPortraitPhone -> 28.dp
        else -> 24.dp
    }
    val itemSpacing = when {
        isTablet -> 16.dp
        isPortraitPhone -> 14.dp
        else -> 12.dp
    }
    val summaryMinHeight = when {
        isTablet -> 180.dp
        isPortraitPhone -> 160.dp
        else -> 140.dp
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.travel_history_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_description))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (trips.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = pagePadding)
            )
        } else {
            val totalKm = trips.sumOf { it.distanceKm }
            val totalPoints = trips.sumOf { it.points }
            val totalTrips = trips.size
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(itemSpacing),
                contentPadding = PaddingValues(bottom = contentPaddingBottom)
            ) {
                item {
                    SummaryHeader(
                        tripCount = totalTrips,
                        totalKm = totalKm,
                        totalPoints = totalPoints,
                        isTablet = isTablet,
                        minHeight = summaryMinHeight
                    )
                }
                items(trips, key = { it.id }) { trip ->
                    TripCard(
                        date = trip.date,
                        distanceKm = trip.distanceKm,
                        points = trip.points,
                        duration = trip.duration,
                        isTablet = isTablet,
                        onClick= {
                            navController.navigate("tripDetails/${trip.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(
    tripCount: Int,
    totalKm: Double,
    totalPoints: Int,
    isTablet: Boolean,
    minHeight: androidx.compose.ui.unit.Dp
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(bottom = 8.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(
                    horizontal = if (isTablet) 28.dp else 20.dp,
                    vertical = if (isTablet) 20.dp else 16.dp
                )
        ) {
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsWalk,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.travel_history_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                stringResource(R.string.travel_summary_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatChip(
                            label = stringResource(R.string.trips_label),
                            value = "$tripCount",
                            isTablet = isTablet,
                            modifier = Modifier.width(140.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        StatChip(
                            label = stringResource(R.string.distance_label),
                            value = String.format("%.1f km", totalKm),
                            isTablet = isTablet,
                            modifier = Modifier.width(160.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        StatChip(
                            label = stringResource(R.string.points_label),
                            value = "$totalPoints",
                            isTablet = isTablet,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsWalk,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.travel_history_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                stringResource(R.string.travel_summary_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatChip(
                            label = stringResource(R.string.trips_label),
                            value = "$tripCount",
                            isTablet = isTablet,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = stringResource(R.string.distance_label),
                            value = String.format("%.1f km", totalKm),
                            isTablet = isTablet,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = stringResource(R.string.points_label),
                            value = "$totalPoints",
                            isTablet = isTablet,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = if (isTablet) 72.dp else 64.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = if (isTablet) 14.dp else 10.dp,
                    horizontal = 12.dp
                ),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                fontSize = if (isTablet) 14.sp else 12.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = if (isTablet) 20.sp else 16.sp
            )
        }
    }
}

@Composable
private fun TripCard(
    date: String,
    distanceKm: Double,
    points: Int,
    duration: String,
    isTablet: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 8.dp else 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(if (isTablet) 20.dp else 18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isTablet) 6.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isTablet) 20.dp else 16.dp,
                    vertical = if (isTablet) 16.dp else 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    date,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (isTablet) 18.sp else 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        String.format("%.2f km", distanceKm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Separador
                    Text(
                        " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Ícone e Duração
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        duration,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                PointsPill(points = points, isTablet = isTablet)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.details_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PointsPill(points: Int, isTablet: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isTablet) 12.dp else 10.dp,
                vertical = if (isTablet) 8.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(if (isTablet) 18.dp else 16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "$points ${stringResource(R.string.points_abbrev)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = if (isTablet) 14.sp else 12.sp
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.empty_trips_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.empty_trips_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Preview(showBackground = true, name = "Travel History - Phone")
@Composable
private fun TravelHistoryScreenPreview_Phone() {
    val nav = rememberNavController()
    MaterialTheme {
        TravelHistoryScreen(
            navController = nav,
            windowSizeClass = fakeWindowSizeClass(
                WindowWidthSizeClass.Compact,
                WindowHeightSizeClass.Medium
            )
        )
    }
}

@Preview(showBackground = true, name = "Travel History - Tablet")
@Composable
private fun TravelHistoryScreenPreview_Tablet() {
    val nav = rememberNavController()
    MaterialTheme {
        TravelHistoryScreen(
            navController = nav,
            windowSizeClass = fakeWindowSizeClass(
                WindowWidthSizeClass.Medium,
                WindowHeightSizeClass.Medium
            )
        )
    }
}