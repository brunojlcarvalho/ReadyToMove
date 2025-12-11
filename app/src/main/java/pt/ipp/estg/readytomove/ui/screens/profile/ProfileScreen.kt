package pt.ipp.estg.readytomove.ui.screens.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import pt.ipp.estg.readytomove.R
import pt.ipp.estg.readytomove.viewmodel.ProfileViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController, windowSizeClass: WindowSizeClass) {
    val viewModel: ProfileViewModel = viewModel()
    val user by viewModel.user.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshUserData()
    }

    if (user == null) {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.profile_title)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_description))
                        }
                    },
                    scrollBehavior = scrollBehavior
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
            windowSizeClass = windowSizeClass,
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
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun ProfileScreenScaffold(
    windowSizeClass: WindowSizeClass,
    userName: String,
    userEmail: String,
    avatarUrl: String?,
    points: Int,
    totalTrips: Int,
    onBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onLogout: () -> Unit
) {
    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass
    val isTablet = widthClass >= WindowWidthSizeClass.Medium
    val isPortraitPhone = widthClass == WindowWidthSizeClass.Compact &&
            heightClass == WindowHeightSizeClass.Expanded
    val headerHeight = when {
        isTablet -> 340.dp
        isPortraitPhone -> 300.dp
        else -> 280.dp
    }
    val avatarSize = when {
        isTablet -> 160.dp
        isPortraitPhone -> 140.dp
        else -> 120.dp
    }
    val nameFontSize = when {
        isTablet -> 48.sp
        isPortraitPhone -> 36.sp
        else -> 28.sp
    }
    val emailFontSize = when {
        isTablet -> 18.sp
        isPortraitPhone -> 16.sp
        else -> 14.sp
    }
    val statCardHeight = when {
        isTablet -> 140.dp
        isPortraitPhone -> 120.dp
        else -> 110.dp
    }
    val pagePadding = when {
        isTablet -> 32.dp
        isPortraitPhone -> 20.dp
        else -> 16.dp
    }
    val sectionSpacing = when {
        isTablet -> 20.dp
        isPortraitPhone -> 18.dp
        else -> 16.dp
    }

    var isEditing by rememberSaveable { mutableStateOf(false) }
    var tempName by rememberSaveable { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val focus = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_description))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ProfileHeader(
                name = userName,
                email = userEmail,
                avatarUrl = avatarUrl,
                isEditing = isEditing,
                tempName = tempName,
                onTempChange = { tempName = it },
                onStartEdit = {
                    tempName = userName
                    isEditing = true
                },
                onCancelEdit = {
                    isEditing = false
                    focus.clearFocus()
                },
                onSaveEdit = {
                    if (tempName.isNotBlank()) onUpdateName(tempName.trim())
                    isEditing = false
                    focus.clearFocus()
                },
                headerHeight = headerHeight,
                avatarSize = avatarSize,
                nameFontSize = nameFontSize,
                emailFontSize = emailFontSize,
                isTablet = isTablet
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = pagePadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                contentPadding = PaddingValues(top = sectionSpacing, bottom = 24.dp)
            ) {
                item {
                    if (isTablet) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProfileStatsSection(
                                points = points,
                                trips = totalTrips,
                                modifier = Modifier.weight(1f),
                                cardHeight = statCardHeight
                            )
                            ProfileInfoCard(
                                title = stringResource(R.string.info_card_title),
                                rows = listOf(
                                    InfoRowData(stringResource(R.string.name_label), userName),
                                    InfoRowData(stringResource(R.string.email_label), userEmail),
                                    InfoRowData(stringResource(R.string.trips_label), totalTrips.toString()),
                                    InfoRowData(stringResource(R.string.points_label), points.toString())
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        ProfileStatsSection(
                            points = points,
                            trips = totalTrips,
                            modifier = Modifier.fillMaxWidth(),
                            cardHeight = statCardHeight
                        )
                        Spacer(Modifier.height(12.dp))
                        ProfileInfoCard(
                            title = stringResource(R.string.info_card_title),
                            rows = listOf(
                                InfoRowData(stringResource(R.string.name_label), userName),
                                InfoRowData(stringResource(R.string.email_label), userEmail),
                                InfoRowData(stringResource(R.string.trips_label), totalTrips.toString()),
                                InfoRowData(stringResource(R.string.points_label), points.toString())
                            )
                        )
                    }
                }
                item {
                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isTablet) 64.dp else 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.logout_button), fontSize = if (isTablet) 18.sp else 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    avatarUrl: String?,
    isEditing: Boolean,
    tempName: String,
    onTempChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    headerHeight: Dp,
    avatarSize: Dp,
    nameFontSize: androidx.compose.ui.unit.TextUnit,
    emailFontSize: androidx.compose.ui.unit.TextUnit,
    isTablet: Boolean
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .padding(horizontal = 0.dp)
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 40.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.08f))
                    .border(
                        width = (if (isTablet) 4.dp else 3.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.firstOrNull()?.uppercase() ?: "?",
                            fontSize = if (isTablet) 42.sp else 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(if (isTablet) 18.dp else 14.dp))

            AnimatedContent(
                targetState = isEditing,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "nameEdit"
            ) { editing ->
                if (editing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = tempName,
                            onValueChange = onTempChange,
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = nameFontSize,
                                fontWeight = FontWeight.Bold
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { onSaveEdit() }),
                            placeholder = { Text(stringResource(R.string.placeholder_new_name)) }
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onSaveEdit) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.save_desc), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        IconButton(onClick = onCancelEdit) {
                            Icon(Icons.Default.ExitToApp, contentDescription = stringResource(R.string.cancel_desc), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            name,
                            fontSize = nameFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onStartEdit) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_name_desc), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            Text(
                email,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = emailFontSize
            )
        }
    }
}

@Composable
private fun ProfileStatsSection(
    points: Int,
    trips: Int,
    modifier: Modifier = Modifier,
    cardHeight: Dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatCard(
            title = stringResource(R.string.points_label),
            value = points.toString(),
            icon = Icons.Default.Star,
            modifier = Modifier.weight(1f),
            cardHeight = cardHeight
        )
        StatCard(
            title = stringResource(R.string.trips_label),
            value = trips.toString(),
            icon = Icons.Default.History,
            modifier = Modifier.weight(1f),
            cardHeight = cardHeight
        )
    }
}

data class InfoRowData(val label: String, val value: String)

@Composable
private fun ProfileInfoCard(
    title: String,
    rows: List<InfoRowData>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        row.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        row.value,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (index != rows.lastIndex) {
                    Divider()
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    cardHeight: Dp
) {
    ElevatedCard(
        modifier = modifier.height(cardHeight),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

@Preview(showBackground = true, name = "Profile - Phone")
@Composable
private fun ProfileScreenPreview_Phone() {
    val nav = rememberNavController()
    MaterialTheme {
        ProfileScreenScaffold(
            windowSizeClass = fakeWindowSizeClass(
                WindowWidthSizeClass.Compact,
                WindowHeightSizeClass.Medium
            ),
            userName = "Ana Silva",
            userEmail = "ana.silva@example.com",
            avatarUrl = null,
            points = 1250,
            totalTrips = 42,
            onBack = {},
            onUpdateName = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, name = "Profile - Tablet")
@Composable
private fun ProfileScreenPreview_Tablet() {
    val nav = rememberNavController()
    MaterialTheme {
        ProfileScreenScaffold(
            windowSizeClass = fakeWindowSizeClass(
                WindowWidthSizeClass.Medium,
                WindowHeightSizeClass.Medium
            ),
            userName = "Bruno Costa",
            userEmail = "bruno.costa@example.com",
            avatarUrl = null,
            points = 3420,
            totalTrips = 128,
            onBack = {},
            onUpdateName = {},
            onLogout = {}
        )
    }
}