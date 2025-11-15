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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import pt.ipp.estg.readytomove.viewmodel.LeaderboardViewModel
import pt.ipp.estg.readytomove.data.model.LeaderboardEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(navController: NavHostController, windowSizeClass: WindowSizeClass) {
    val viewModel: LeaderboardViewModel = viewModel()
    val leaderboard by viewModel.leaderboard.collectAsState()

    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass

    val isTablet = widthClass >= WindowWidthSizeClass.Medium
    val isPortraitPhone = widthClass == WindowWidthSizeClass.Compact &&
            heightClass == WindowHeightSizeClass.Expanded

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
                        "Leaderboard",
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize * titleScale
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
    val top3 = entries.take(3)
    val rest = entries.drop(3)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LeaderboardHeader(
                headerHeight = headerHeight,
                horizontalPadding = horizontalPadding,
                titleScale = titleScale
            )
        }
        if (top3.size == 3) {
            item {
                PodiumSection(
                    entries = top3,
                    podiumCardHeight = podiumCardHeight,
                    podiumAvatarSize = podiumAvatarSize
                )
            }
        }
        itemsIndexed(rest) { index, entry ->
            LeaderboardRow(
                entry = entry,
                position = entry.position ?: (index + 4),
                avatarSize = avatarSize,
                height = rowCardHeight,
                horizontalPadding = horizontalPadding,
                isTablet = isTablet
            )
        }
    }
}

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
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Top Utilizadores",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Quem está a liderar esta semana",
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

        PodiumCard(entry = second, height = podiumCardHeight, avatarSize = podiumAvatarSize, accent = Color(0xFFB0BEC5))
        PodiumCard(entry = first, height = podiumCardHeight, avatarSize = podiumAvatarSize, accent = Color(0xFFFFD700))
        PodiumCard(entry = third, height = podiumCardHeight, avatarSize = podiumAvatarSize, accent = Color(0xFFCD7F32))
    }
}


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
            if (entry.avatarUrl != null) {
                AsyncImage(
                    model = entry.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                )
            } else {
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
            Spacer(Modifier.height(8.dp))
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${entry.points} pts",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


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
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (entry.avatarUrl != null) {
                    AsyncImage(
                        model = entry.avatarUrl,
                        contentDescription = entry.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = entry.name.firstOrNull()?.uppercase() ?: "?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
                    "${entry.points} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Medal(position: Int) {
    val (emoji, tint) = when (position) {
        1 -> "🥇" to Color(0xFFFFD700)
        2 -> "🥈" to Color(0xFFC0C0C0)
        3 -> "🥉" to Color(0xFFCD7F32)
        else -> "⭐" to MaterialTheme.colorScheme.primary
    }
    Text(
        text = emoji,
        style = MaterialTheme.typography.titleLarge
    )
}

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
