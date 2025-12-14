package pt.ipp.estg.readytomove.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.viewmodel.LeaderboardViewModel
import pt.ipp.estg.readytomove.data.model.LeaderboardEntry

/**
 * The main Composable for the Leaderboard Screen.
 *
 * This screen displays a ranked list of users based on their points.
 * It features a specialized "Podium" view for the top 3 users and a standard list
 * for the remaining participants. The layout adapts dynamically to different
 * screen sizes (Tablet vs Phone).
 *
 * @param navController Controller to handle navigation actions (e.g., going back).
 * @param windowSizeClass The size class of the device window, used for adaptive layout adjustments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavHostController, windowSizeClass: WindowSizeClass) {
    val viewModel: LeaderboardViewModel = viewModel()
    // Collects the realtime leaderboard state from the ViewModel
    val leaderboard by viewModel.leaderboard.collectAsState()

    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass

    // Device classification logic
    val isTablet = widthClass >= WindowWidthSizeClass.Medium
    val isPortraitPhone = widthClass == WindowWidthSizeClass.Compact &&
            heightClass == WindowHeightSizeClass.Expanded

    // --- Adaptive Dimensions ---
    // Calculates sizes for header, cards, and avatars based on device type
    val headerHeight = when {
        isTablet -> 220.dp
        isPortraitPhone -> 170.dp
        else -> 150.dp
    }
    val podiumCardHeight = when {
        isTablet -> 200.dp
        isPortraitPhone -> 180.dp
        else -> 150.dp
    }
    val podiumAvatarSize = when {
        isTablet -> 96.dp
        isPortraitPhone -> 72.dp
        else -> 60.dp
    }
    val avatarSize = when {
        isTablet -> 72.dp
        isPortraitPhone -> 60.dp
        else -> 54.dp
    }
    val rowCardHeight = when {
        isTablet -> 110.dp
        isPortraitPhone -> 96.dp
        else -> 88.dp
    }
    val horizontalPadding = if (isTablet) 32.dp else 16.dp
    val titleScale = if (isTablet) 1.25f else 1f

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.leaderboard_title),
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize * titleScale
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_description)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LeaderboardContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            entries = leaderboard,
            headerHeight = headerHeight,
            podiumCardHeight = podiumCardHeight,
            podiumAvatarSize = podiumAvatarSize,
            avatarSize = avatarSize,
            rowCardHeight = rowCardHeight,
            horizontalPadding = horizontalPadding,
            isTablet = isTablet,
            titleScale = titleScale
        )
    }
}

/**
 * Renders the actual content of the Leaderboard, splitting the list into Top 3 and Rest.
 */
@Composable
private fun LeaderboardContent(
    modifier: Modifier,
    entries: List<LeaderboardEntry>,
    headerHeight: Dp,
    podiumCardHeight: Dp,
    podiumAvatarSize: Dp,
    avatarSize: Dp,
    rowCardHeight: Dp,
    horizontalPadding: Dp,
    isTablet: Boolean,
    titleScale: Float
) {
    // Separate the Top 3 for special display
    val top3 = entries.take(3)
    val rest = entries.drop(3)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header with Stats Summary
        item {
            LeaderboardHeader(
                headerHeight = headerHeight,
                horizontalPadding = horizontalPadding,
                titleScale = titleScale
            )
        }

        // 2. Podium Section (Only if we have at least 3 users)
        if (top3.size == 3) {
            item {
                PodiumSection(
                    entries = top3,
                    podiumCardHeight = podiumCardHeight,
                    podiumAvatarSize = podiumAvatarSize
                )
            }
        }

        // 3. List of remaining users
        itemsIndexed(rest) { index, entry ->
            LeaderboardRow(
                entry = entry,
                // Calculate position based on list index + offset for top 3
                position = entry.position ?: (index + 4),
                avatarSize = avatarSize,
                height = rowCardHeight,
                horizontalPadding = horizontalPadding,
                isTablet = isTablet
            )
        }
    }
}

/**
 * Displays the decorative header with gradient background and title.
 */
@Composable
private fun LeaderboardHeader(headerHeight: Dp, horizontalPadding: Dp, titleScale: Float) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = horizontalPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        stringResource(R.string.top_users_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        stringResource(R.string.top_users_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

/**
 * Renders the top 3 users in a Podium layout (2nd - 1st - 3rd).
 */
@Composable
private fun PodiumSection(entries: List<LeaderboardEntry>, podiumCardHeight: Dp, podiumAvatarSize: Dp) {
    val ordered = entries.sortedBy { it.position }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val second = ordered.getOrNull(1)
        val first = ordered.getOrNull(0)
        val third = ordered.getOrNull(2)

        // The order here defines the visual order: Left (2nd), Center (1st), Right (3rd)
        PodiumCard(entry = second, height = podiumCardHeight, avatarSize = podiumAvatarSize, accent = Color(0xFFB0BEC5)) // Silver
        PodiumCard(entry = first, height = podiumCardHeight, avatarSize = podiumAvatarSize, accent = Color(0xFFFFD700))  // Gold
        PodiumCard(entry = third, height = podiumCardHeight, avatarSize = podiumAvatarSize, accent = Color(0xFFCD7F32))  // Bronze
    }
}

/**
 * Represents a single card in the Podium (Top 3).
 */
@Composable
private fun RowScope.PodiumCard(
    entry: LeaderboardEntry?,
    height: Dp,
    avatarSize: Dp,
    accent: Color
) {
    val baseModifier = Modifier
        .weight(1f)
        .height(height)

    if (entry == null) {
        Box(modifier = baseModifier)
        return
    }

    Surface(
        modifier = baseModifier,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        color = accent.copy(alpha = 0.25f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Medal(position = entry.position)
            Spacer(Modifier.height(6.dp))

            // Helper Composable for fallback Avatar (First letter of name)
            val PlaceholderLetter = @Composable {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        entry.name.first().uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (entry.avatarUrl != null) {
                SubcomposeAsyncImage(
                    model = entry.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape),
                    error = { PlaceholderLetter() }
                )
            } else {
                PlaceholderLetter()
            }

            Spacer(Modifier.height(8.dp))
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${entry.points} ${stringResource(R.string.points_abbrev)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Represents a standard row for users ranked 4th and below.
 */
@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    position: Int,
    avatarSize: Dp,
    height: Dp,
    horizontalPadding: Dp,
    isTablet: Boolean
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(if (isTablet) 24.dp else 18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding / 2, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RankBadge(position = position)

            // Helper Composable for fallback Avatar
            val PlaceholderLetter = @Composable {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.name.firstOrNull()?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (entry.avatarUrl != null) {
                SubcomposeAsyncImage(
                    model = entry.avatarUrl,
                    contentDescription = entry.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape),
                    error = { PlaceholderLetter() }
                )
            } else {
                PlaceholderLetter()
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${entry.points} ${stringResource(R.string.points_abbrev)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Helper to display the Medal Emoji based on rank.
 */
@Composable
private fun Medal(position: Int) {
    val emoji = when (position) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "⭐"
    }
    Text(
        text = emoji,
        style = MaterialTheme.typography.titleLarge
    )
}

/**
 * Helper to display the Rank Number (e.g., #4) with a colored background.
 */
@Composable
private fun RankBadge(position: Int) {
    val bg = when (position) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "#$position",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- PREVIEWS ---

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

@Preview(showBackground = true, name = "Leaderboard - Phone")
@Composable
private fun LeaderboardScreenPreview_Phone() {
    MaterialTheme {
        val nav = rememberNavController()
        LeaderboardScreen(
            navController = nav,
            windowSizeClass = fakeWindowSizeClass(
                WindowWidthSizeClass.Compact,
                WindowHeightSizeClass.Expanded
            )
        )
    }
}

@Preview(showBackground = true, name = "Leaderboard - Tablet")
@Composable
private fun LeaderboardScreenPreview_Tablet() {
    MaterialTheme {
        val nav = rememberNavController()
        LeaderboardScreen(
            navController = nav,
            windowSizeClass = fakeWindowSizeClass(
                WindowWidthSizeClass.Medium,
                WindowHeightSizeClass.Medium
            )
        )
    }
}