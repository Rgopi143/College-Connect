package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.PortalViewModel
import com.example.data.model.CollegeNotification
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusConnectApp(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currUser by viewModel.currentUser.collectAsState()

    val allNotif by viewModel.notifications.collectAsState()
    var shownNotifIds by remember { mutableStateOf<Set<Int>?>(null) }
    var activeHeadsUpNotif by remember { mutableStateOf<CollegeNotification?>(null) }

    LaunchedEffect(allNotif) {
        if (shownNotifIds == null) {
            shownNotifIds = allNotif.map { it.id }.toSet()
        } else {
            val currentIds = shownNotifIds ?: emptySet()
            val newUnread = allNotif.filter { it.id !in currentIds && !it.isRead }
            if (newUnread.isNotEmpty()) {
                val newest = newUnread.maxByOrNull { it.timestamp }
                if (newest != null) {
                    activeHeadsUpNotif = newest
                    shownNotifIds = currentIds + allNotif.map { it.id }.toSet()
                }
            }
        }
    }

    LaunchedEffect(activeHeadsUpNotif) {
        if (activeHeadsUpNotif != null) {
            delay(8000)
            activeHeadsUpNotif = null
        }
    }

    LaunchedEffect(currUser) {
        if (currUser == null) {
            shownNotifIds = null
            activeHeadsUpNotif = null
        }
    }

    // Fully robust state-based navigation stack with correct BackHandler integration
    val screenStack = remember { mutableStateListOf("DASHBOARD") }
    val currentScreen = screenStack.last()

    var showProfileDialog by remember { mutableStateOf(false) }

    fun navigateTo(screen: String) {
        if (screen == "DASHBOARD") {
            screenStack.clear()
            screenStack.add("DASHBOARD")
        } else {
            if (screenStack.last() != screen) {
                screenStack.add(screen)
            }
        }
    }

    // Handle back button on device safely
    BackHandler(enabled = screenStack.size > 1) {
        screenStack.removeAt(screenStack.size - 1)
    }

    if (currUser == null) {
        LoginScreen(viewModel = viewModel, modifier = modifier)
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = getScreenTitle(currentScreen, currUser?.role),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        if (screenStack.size > 1) {
                            IconButton(onClick = { screenStack.removeAt(screenStack.size - 1) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            IconButton(onClick = { showProfileDialog = true }) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "My Profile", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.logout()
                                screenStack.clear()
                                screenStack.add("DASHBOARD")
                                Toast.makeText(context, "Logged out successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Animated navigation transitions
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "navigation"
                ) { screen ->
                    when (screen) {
                        "DASHBOARD" -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigate = { navigateTo(it) }
                        )
                        "OUTPASS" -> OutpassScreen(
                            viewModel = viewModel
                        )
                        "CERTIFICATE" -> CertificateScreen(
                            viewModel = viewModel
                        )
                        "STATIONERY" -> StationeryScreen(
                            viewModel = viewModel
                        )
                        "PRINT" -> PrintScreen(
                            viewModel = viewModel,
                            onNavigate = { navigateTo(it) }
                        )
                        "CANTEEN" -> CanteenScreen(
                            viewModel = viewModel
                        )
                        "TRACK_REQUESTS" -> TrackRequestsScreen(
                            viewModel = viewModel
                        )
                        "ADMIN" -> AdminScreen(
                            viewModel = viewModel
                        )
                        "TPC" -> TPCScreen(
                            viewModel = viewModel
                        )
                        "FIREBASE_HUB" -> FirebaseHubScreen(
                            viewModel = viewModel
                        )
                    }
                }

                // Smooth animated real-time heads-up notification alert banner overlay
                AnimatedVisibility(
                    visible = activeHeadsUpNotif != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    activeHeadsUpNotif?.let { notif ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.markNotificationRead(notif.id)
                                    activeHeadsUpNotif = null
                                    navigateTo("TRACK_REQUESTS")
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (notif.category) {
                                            "Outpass" -> Icons.Default.DirectionsRun
                                            "Certificate" -> Icons.Default.CardMembership
                                            "Canteen" -> Icons.Default.Fastfood
                                            "Store" -> Icons.Default.Storefront
                                            "Print" -> Icons.Default.Print
                                            else -> Icons.Default.NotificationsActive
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notif.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = notif.content,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap to view in Request Timeline",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                IconButton(
                                    onClick = {
                                        viewModel.markNotificationRead(notif.id)
                                        activeHeadsUpNotif = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Student Profile details modal dialog
        if (showProfileDialog) {
            val user = currUser!!
            var isEditing by remember { mutableStateOf(false) }

            var editName by remember(user) { mutableStateOf(user.name) }
            var editDept by remember(user) { mutableStateOf(user.department) }
            var editEmail by remember(user) { mutableStateOf(user.email) }
            var editPhone by remember(user) { mutableStateOf(user.phone) }
            var editParentContact by remember(user) { mutableStateOf(user.parentContact) }

            AlertDialog(
                onDismissRequest = { showProfileDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditing) "Edit Admin Profile" else "Campus User Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (!isEditing) {
                            IconButton(
                                onClick = { isEditing = true },
                                modifier = Modifier.testTag("toggle_profile_edit_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (!isEditing) {
                            Text(
                                text = user.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text(
                            text = "Authentication Level: ${user.role}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider()

                        if (isEditing) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Display Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = editDept,
                                onValueChange = { editDept = it },
                                label = { Text("College Department") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_dept_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                label = { Text("Official Email") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_email_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Phone Number") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_phone_input"),
                                singleLine = true
                            )

                            if (user.role == "STUDENT") {
                                OutlinedTextField(
                                    value = editParentContact,
                                    onValueChange = { editParentContact = it },
                                    label = { Text("Parent Contact") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_parent_input"),
                                    singleLine = true
                                )
                            }
                        } else {
                            DetailRow("Student Roll ID:", user.userId)
                            DetailRow("College Department:", user.department)
                            DetailRow("Official Email:", user.email)
                            DetailRow("Phone Number:", user.phone)
                            if (user.role == "STUDENT") {
                                DetailRow("Parent Contact:", user.parentContact)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "© 2026 RANBIDGE SOLUTIONS PVT LTD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditing) {
                            TextButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.testTag("cancel_profile_button")
                            ) {
                                Text("Discard")
                            }
                            Button(
                                onClick = {
                                    if (editName.isBlank()) {
                                        Toast.makeText(context, "Name cannot be blank!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.updateUserProfile(
                                            name = editName,
                                            email = editEmail,
                                            phone = editPhone,
                                            parentContact = editParentContact,
                                            department = editDept
                                        )
                                        isEditing = false
                                        Toast.makeText(context, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("save_profile_button")
                            ) {
                                Text("Save")
                            }
                        } else {
                            Button(
                                onClick = { showProfileDialog = false },
                                modifier = Modifier.testTag("finished_profile_button")
                            ) {
                                Text("Finished")
                            }
                        }
                    }
                }
            )
        }
    }
}

fun getScreenTitle(screen: String, role: String? = null): String {
    return when (screen) {
        "DASHBOARD" -> "CAMPUS CONNECT"
        "OUTPASS" -> "OUTPASS CLEARANCE"
        "CERTIFICATE" -> "CERTIFICATE SERVICES"
        "STATIONERY" -> "ACADEMIC STORE"
        "PRINT" -> "PRINT CENTER"
        "CANTEEN" -> "CANTEEN TICKETS"
        "TRACK_REQUESTS" -> "REQUEST TIMELINE"
        "ADMIN" -> if (role == "MENTOR" || role == "CLASS_ADVISOR") "STUDENT REQUESTS" else "STAFF CONSOLE"
        "TPC" -> "TRAINING & PLACEMENT (TPC)"
        "FIREBASE_HUB" -> "FIREBASE CLOUD SYNC HUB"
        else -> "PORTAL"
    }
}
