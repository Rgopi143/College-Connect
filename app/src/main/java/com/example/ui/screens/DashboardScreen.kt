package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CertificateRequest
import com.example.data.model.CollegeNotification
import com.example.data.model.OutpassRequest
import com.example.data.model.CollegeEvent
import com.example.ui.viewmodel.PortalViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.viewmodel.Department
import com.example.ui.viewmodel.Faculty
import com.example.ui.viewmodel.TpcStaff
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PortalViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val allNotif by viewModel.notifications.collectAsState()
    val studentOutpasses by viewModel.studentOutpasses.collectAsState()
    val studentCertificates by viewModel.studentCertificates.collectAsState()
    val studentStationeries by viewModel.studentStationeryRequests.collectAsState()
    val studentPrints by viewModel.studentPrintRequests.collectAsState()
    val studentCanteenBookings by viewModel.studentCanteenBookings.collectAsState()
    val tpcStaffList by viewModel.tpcStaffList.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val unreadCount = allNotif.count { !it.isRead }

    var showNotifDialog by remember { mutableStateOf(false) }
    var selectedDeptForDetail by remember { mutableStateOf<Department?>(null) }
    var showAddDeptDialog by remember { mutableStateOf(false) }
    var showStaffDialog by remember { mutableStateOf(false) }
    var showAddFacultyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showEventsDialog by remember { mutableStateOf(false) }
    val allEvents by viewModel.allEvents.collectAsState()

    var showSuccessBanner by remember { mutableStateOf(false) }
    var successBannerMsg by remember { mutableStateOf("") }
    var successBannerDetails by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Welcoming & Profile Banner block
        item {
            user?.let { currUser ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            text = currUser.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${currUser.rollNumber} • ${currUser.department}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (currUser.assignedMentorName != null || currUser.assignedAdvisorName != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                currUser.assignedMentorName?.let { mentor ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.testTag("mentor_badge")
                                    ) {
                                        Text(
                                            text = "Mentor: $mentor",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                currUser.assignedAdvisorName?.let { advisor ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.testTag("advisor_badge")
                                    ) {
                                        Text(
                                            text = "Class Teacher: $advisor",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        

                    }

                    // Notification bell with unread badge count
                    Box(modifier = Modifier.clickable { showNotifDialog = true }) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (unreadCount > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                            ) {
                                Text(text = unreadCount.toString(), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Hero illustration
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_nec_logo_new),
                        contentDescription = "NEC Narasaraopeta Engineering College Cover Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Visual Tint
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                    Text(
                        text = "NEC Narasaraopeta Portal",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }
        }

        // Campus Outpass Hub for Students
        item {
            if (user?.role == "STUDENT") {
                StudentOutpassHubCard(
                    studentOutpasses = studentOutpasses,
                    currentUser = user,
                    onSubmitOutpass = { dept, reason, returnTime ->
                        viewModel.submitOutpass(dept, reason, returnTime)
                        successBannerMsg = "Outpass Request Submitted!"
                        successBannerDetails = "Successfully requested campus exit for $dept (Expected return: $returnTime)."
                        showSuccessBanner = true
                    },
                    onNavigate = onNavigate
                )
            }
        }

        // Active Canteen Token Widget
        item {
            if (user?.role == "STUDENT") {
                val activeMealToken = studentCanteenBookings.find { it.status == "BOOKED" }
                if (activeMealToken != null) {
                    ActiveCanteenTokenCard(
                        booking = activeMealToken,
                        onNavigate = onNavigate
                    )
                }
            }
        }

        // Student Requests & History Container Card
        item {
            if (user?.role == "STUDENT") {
                StudentRequestsHistoryCard(
                    outpasses = studentOutpasses,
                    certificates = studentCertificates,
                    stationeries = studentStationeries,
                    prints = studentPrints,
                    canteenBookings = studentCanteenBookings,
                    onNavigate = onNavigate
                )
            }
        }

        // Quick Access Grid Section Header
        if (user?.role == "STUDENT") {
            item {
                Text(
                    text = "Services Directory",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Quick Access Cards Grid (Using Column layout for safe, reliable grids)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickTile(
                            title = "Outpass Clearance",
                            subtitle = "Campus leave permission",
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("outpass_tile"),
                            onClick = { onNavigate("OUTPASS") }
                        )
                        QuickTile(
                            title = "Certificate Hub",
                            subtitle = "Bonafide, NOC & letters",
                            icon = Icons.Default.CardMembership,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("certificate_tile"),
                            onClick = { onNavigate("CERTIFICATE") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickTile(
                            title = "Stationery Store",
                            subtitle = "Order notebooks & supplies",
                            icon = Icons.Default.Storefront,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stationery_tile"),
                            onClick = { onNavigate("STATIONERY") }
                        )
                        QuickTile(
                            title = "Printout Center",
                            subtitle = "Upload and print documents",
                            icon = Icons.Default.Print,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("print_tile"),
                            onClick = { onNavigate("PRINT") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickTile(
                            title = "Canteen Token",
                            subtitle = "Pre-order meals & snacks",
                            icon = Icons.Default.Fastfood,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("canteen_tile"),
                            onClick = { onNavigate("CANTEEN") }
                        )
                        QuickTile(
                            title = "Track Requests",
                            subtitle = "History & updates",
                            icon = Icons.Default.TrackChanges,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tracking_tile"),
                            onClick = { onNavigate("TRACK_REQUESTS") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickTile(
                            title = "Help Desk",
                            subtitle = "Helpline & support contacts",
                            icon = Icons.AutoMirrored.Filled.ContactSupport,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("help_tile_student"),
                            onClick = { showHelpDialog = true }
                        )
                        QuickTile(
                            title = "Campus Events",
                            subtitle = "Seminars, fests & schedules",
                            icon = Icons.Default.Event,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("events_tile_student"),
                            onClick = { showEventsDialog = true }
                        )
                    }
                }
            }
        } else {
            // Administrative & Faculty Services Directory
            item {
                Text(
                    text = "Administrative Services",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (user?.role == "STORE" || user?.role == "ADMIN") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            QuickTile(
                                title = "Store Admin Hub",
                                subtitle = "Manage stationery & printing",
                                icon = Icons.Default.Storefront,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("store_admin_tile"),
                                onClick = { onNavigate("STATIONERY") }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickTile(
                            title = if (user?.role == "MENTOR" || user?.role == "CLASS_ADVISOR") "Student Requests" else "Staff",
                            subtitle = "Approve outpasses & certs",
                            icon = Icons.Default.AdminPanelSettings,
                            color = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_tile"),
                            onClick = { onNavigate("ADMIN") }
                        )
                        QuickTile(
                            title = "Canteen Services",
                            subtitle = "Pre-order meals & orders",
                            icon = Icons.Default.Fastfood,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("canteen_tile"),
                            onClick = { onNavigate("CANTEEN") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (user?.role != "SECURITY") {
                            QuickTile(
                                title = "Placement Cell (TPC)",
                                subtitle = "Staff, departments & job recruitment",
                                icon = Icons.Default.Business,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tpc_tile"),
                                onClick = { onNavigate("TPC") }
                            )
                        }
                        if (user?.role == "PRINCIPAL") {
                            QuickTile(
                                title = "Staff",
                                subtitle = "Depts, roster & allocations",
                                icon = Icons.Default.Groups,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("staff_departments_tile"),
                                onClick = { showStaffDialog = true }
                            )
                        } else if (user?.role != "SECURITY") {
                            QuickTile(
                                title = "Campus Events",
                                subtitle = "Seminars, fests & schedules",
                                icon = Icons.Default.Event,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("events_tile_staff"),
                                onClick = { showEventsDialog = true }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (user?.role == "PRINCIPAL") {
                            QuickTile(
                                title = "Campus Events",
                                subtitle = "Seminars, fests & schedules",
                                icon = Icons.Default.Event,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("events_tile_principal"),
                                onClick = { showEventsDialog = true }
                            )
                            QuickTile(
                                title = "Help Desk",
                                subtitle = "Helpline & support contacts",
                                icon = Icons.AutoMirrored.Filled.ContactSupport,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("help_tile_principal"),
                                onClick = { showHelpDialog = true }
                            )
                        } else {
                            if (user?.role == "SECURITY") {
                                QuickTile(
                                    title = "Campus Events",
                                    subtitle = "Seminars, fests & schedules",
                                    icon = Icons.Default.Event,
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("events_tile_staff_sec"),
                                    onClick = { showEventsDialog = true }
                                )
                            }
                            QuickTile(
                                title = "Help Desk",
                                subtitle = "Helpline & support contacts",
                                icon = Icons.AutoMirrored.Filled.ContactSupport,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("help_tile_staff"),
                                onClick = { showHelpDialog = true }
                            )
                        }
                    }
                }
            }


        }



        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Powered by RANBIDGE SOLUTIONS PVT LTD",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "© 2026 RANBIDGE SOLUTIONS PVT LTD. All Rights Reserved.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    // Modal Notifications Dialog
    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Announcements", fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = {
                            viewModel.markAllNotificationsRead()
                        }
                    ) {
                        Text("Mark Read")
                    }
                }
            },
            text = {
                if (allNotif.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No notifications yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allNotif) { notif ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notif.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text(notif.category, fontSize = 8.sp) },
                                            modifier = Modifier.height(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.content,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showNotifDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    if (selectedDeptForDetail != null) {
        val dept = selectedDeptForDetail!!
        AlertDialog(
            onDismissRequest = { selectedDeptForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(dept.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${dept.faculties.size} Appointed Members", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = dept.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Core Faculties Grid",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        if (user?.role == "PRINCIPAL") {
                            IconButton(
                                onClick = { showAddFacultyDialog = true },
                                modifier = Modifier.testTag("add_faculty_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Add Faculty",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(dept.faculties) { faculty ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = faculty.name.firstOrNull()?.toString()?.uppercase() ?: "F",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = faculty.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${faculty.designation} • ${faculty.specialization}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = faculty.email,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedDeptForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAddDeptDialog) {
        var deptName by remember { mutableStateOf("") }
        var deptDesc by remember { mutableStateOf("") }
        var facName by remember { mutableStateOf("") }
        var facRole by remember { mutableStateOf("Professor & Head") }
        var facSpec by remember { mutableStateOf("") }
        var facEmail by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDeptDialog = false },
            title = {
                Text(
                    text = "Establish New Academic Department",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text(
                            "Fill out the comprehensive department specifications. A corresponding card grid will immediately generate with your setup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    item {
                        Text("Department Credentials", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    }
                    item {
                        OutlinedTextField(
                            value = deptName,
                            onValueChange = { deptName = it },
                            label = { Text("Department Name (e.g. Data Science)") },
                            modifier = Modifier.fillMaxWidth().testTag("dept_name_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = deptDesc,
                            onValueChange = { deptDesc = it },
                            label = { Text("Core Goals / Description") },
                            modifier = Modifier.fillMaxWidth().testTag("dept_desc_input")
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Initial Core Faculty Allocation", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    }
                    item {
                        OutlinedTextField(
                            value = facName,
                            onValueChange = { facName = it },
                            label = { Text("Faculty Full Name") },
                            modifier = Modifier.fillMaxWidth().testTag("fac_name_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = facRole,
                            onValueChange = { facRole = it },
                            label = { Text("Designation / Role") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = facSpec,
                            onValueChange = { facSpec = it },
                            label = { Text("Area of Specialization Research") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = facEmail,
                            onValueChange = { facEmail = it },
                            label = { Text("Official Inst. Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deptName.isBlank()) {
                            Toast.makeText(context, "Please fulfill Department Name!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addDepartment(
                                name = deptName,
                                description = deptDesc.ifBlank { "Department dedicated to research, labs & academics." },
                                facName = facName,
                                facRole = facRole,
                                facSpec = facSpec,
                                facEmail = facEmail
                            )
                            showAddDeptDialog = false
                            Toast.makeText(context, "Department Grid Created Successfully!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.testTag("submit_dept_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Assemble Grid")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDeptDialog = false }) {
                    Text("Discard")
                }
            }
        )
    }

    if (showStaffDialog) {
        AlertDialog(
            onDismissRequest = { showStaffDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(text = "Staff", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }

                    Button(
                        onClick = { showAddDeptDialog = true },
                        modifier = Modifier.testTag("principal_add_dept_dialog_btn"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Dept", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    item {
                        Text(
                            text = "Academic Departments",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val chunkedDepts = departments.chunked(2)
                            chunkedDepts.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { dept ->
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedDeptForDetail = dept }
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (dept.id.contains("CSE") || dept.id.contains("IT")) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                                } else {
                                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                                                }
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(Color.White, CircleShape)
                                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (dept.id.contains("CSE") || dept.id.contains("IT")) {
                                                            Icons.Default.Terminal
                                                        } else if (dept.id.contains("ECE") || dept.id.contains("EEE")) {
                                                            Icons.Default.Memory
                                                        } else {
                                                            Icons.Default.School
                                                        },
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = dept.name,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    lineHeight = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Text(
                                                    text = dept.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = Color.White
                                                ) {
                                                    Text(
                                                        "${dept.faculties.size} Faculties",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showStaffDialog = false },
                    modifier = Modifier.testTag("close_staff_dialog")
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (showAddFacultyDialog && selectedDeptForDetail != null) {
        val targetDept = selectedDeptForDetail!!
        var newFacName by remember { mutableStateOf("") }
        var newFacDesignation by remember { mutableStateOf("") }
        var newFacEmail by remember { mutableStateOf("") }
        var newFacSpecialization by remember { mutableStateOf("") }
        var newFacPhone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddFacultyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Add Faculty to ${targetDept.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Appoint a new academic faculty member to ${targetDept.name}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = newFacName,
                        onValueChange = { newFacName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("new_fac_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newFacDesignation,
                        onValueChange = { newFacDesignation = it },
                        label = { Text("Role / Designation") },
                        placeholder = { Text("e.g. Professor, Assistant Professor") },
                        modifier = Modifier.fillMaxWidth().testTag("new_fac_designation"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newFacSpecialization,
                        onValueChange = { newFacSpecialization = it },
                        label = { Text("Area of Specialization Research") },
                        placeholder = { Text("e.g. Machine Learning, VLSI") },
                        modifier = Modifier.fillMaxWidth().testTag("new_fac_specialization"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newFacEmail,
                        onValueChange = { newFacEmail = it },
                        label = { Text("Official Email ID") },
                        modifier = Modifier.fillMaxWidth().testTag("new_fac_email"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newFacPhone,
                        onValueChange = { newFacPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91 XXXXX XXXXX") },
                        modifier = Modifier.fillMaxWidth().testTag("new_fac_phone"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFacName.isBlank() || newFacDesignation.isBlank()) {
                            Toast.makeText(context, "Full Name and Role / Designation are mandatory!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addFacultyToDepartment(
                                deptId = targetDept.id,
                                name = newFacName,
                                designation = newFacDesignation,
                                email = newFacEmail.ifBlank { "fac.${newFacName.replace(" ", "").lowercase()}@college.edu" },
                                specialization = newFacSpecialization.ifBlank { "General Academic Research" },
                                phone = newFacPhone.ifBlank { "+91 9988776655" }
                            )
                            val updatedDept = viewModel.departments.value.firstOrNull { it.id == targetDept.id }
                            selectedDeptForDetail = updatedDept
                            
                            showAddFacultyDialog = false
                            Toast.makeText(context, "$newFacName added dynamically to ${targetDept.id}!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("submit_add_faculty_btn")
                ) {
                    Text("Appoint Faculty")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFacultyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEventsDialog) {
        var eventTitleVal by remember { mutableStateOf("") }
        var eventDescVal by remember { mutableStateOf("") }
        var eventDateVal by remember { mutableStateOf("") }
        var eventTimeVal by remember { mutableStateOf("") }
        var eventVenueVal by remember { mutableStateOf("") }
        var eventDeptFilter by remember { mutableStateOf("ALL") }
        var showCreateForm by remember { mutableStateOf(false) }

        var isEditingEvent by remember { mutableStateOf<CollegeEvent?>(null) }

        AlertDialog(
            onDismissRequest = { 
                showEventsDialog = false 
                showCreateForm = false
                isEditingEvent = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "College Events Desk",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    if (user?.role != "STUDENT" && !showCreateForm && isEditingEvent == null) {
                        IconButton(
                            onClick = { 
                                eventTitleVal = ""
                                eventDescVal = ""
                                eventDateVal = ""
                                eventTimeVal = ""
                                eventVenueVal = ""
                                eventDeptFilter = "ALL"
                                showCreateForm = true 
                            },
                            modifier = Modifier.testTag("add_event_btn_dialog")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Event", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                ) {
                    if (showCreateForm || isEditingEvent != null) {
                        // Create / Edit Form
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isEditingEvent != null) "Edit Event Details" else "Schedule New Event",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = eventTitleVal,
                                onValueChange = { eventTitleVal = it },
                                label = { Text("Event Title") },
                                placeholder = { Text("e.g. Smart India Hackathon") },
                                modifier = Modifier.fillMaxWidth().testTag("event_title_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = eventDescVal,
                                onValueChange = { eventDescVal = it },
                                label = { Text("Description") },
                                placeholder = { Text("Detailed description of fests, rules...") },
                                modifier = Modifier.fillMaxWidth().testTag("event_desc_input")
                            )

                            OutlinedTextField(
                                value = eventDateVal,
                                onValueChange = { eventDateVal = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                placeholder = { Text("e.g. 2026-07-20") },
                                modifier = Modifier.fillMaxWidth().testTag("event_date_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = eventTimeVal,
                                onValueChange = { eventTimeVal = it },
                                label = { Text("Time") },
                                placeholder = { Text("e.g. 10:00 AM") },
                                modifier = Modifier.fillMaxWidth().testTag("event_time_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = eventVenueVal,
                                onValueChange = { eventVenueVal = it },
                                label = { Text("Venue") },
                                placeholder = { Text("e.g. Seminar Hall-1, Block-B") },
                                modifier = Modifier.fillMaxWidth().testTag("event_venue_input"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = eventDeptFilter,
                                onValueChange = { eventDeptFilter = it },
                                label = { Text("Target Department (e.g. ALL or CS)") },
                                modifier = Modifier.fillMaxWidth().testTag("event_dept_input"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { 
                                        showCreateForm = false
                                        isEditingEvent = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Back")
                                }

                                Button(
                                    onClick = {
                                        if (eventTitleVal.isBlank() || eventDateVal.isBlank() || eventVenueVal.isBlank()) {
                                            Toast.makeText(context, "Title, Date and Venue are required!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val editing = isEditingEvent
                                            if (editing != null) {
                                                val updated = editing.copy(
                                                    title = eventTitleVal.trim(),
                                                    description = eventDescVal.trim(),
                                                    date = eventDateVal.trim(),
                                                    time = eventTimeVal.trim(),
                                                    venue = eventVenueVal.trim(),
                                                    filterDepartment = eventDeptFilter.trim()
                                                )
                                                viewModel.updateEvent(updated)
                                                Toast.makeText(context, "Event updated!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.createEvent(
                                                    title = eventTitleVal,
                                                    description = eventDescVal,
                                                    date = eventDateVal,
                                                    time = eventTimeVal,
                                                    venue = eventVenueVal,
                                                    dpt = eventDeptFilter
                                                )
                                                Toast.makeText(context, "Event scheduled successfully!", Toast.LENGTH_SHORT).show()
                                            }
                                            showCreateForm = false
                                            isEditingEvent = null
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("save_event_submit_btn")
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    } else {
                        // View Events List
                        Text(
                            text = "Discover pre-vetted seminars, schedules, and active campus updates:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        if (allEvents.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("No events scheduled at the moment.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(allEvents) { evt ->
                                    val isUserOrganizer = user?.role == evt.organizerRole || user?.role == "ADMIN" || user?.role == "PRINCIPAL"
                                    
                                    val cardBg = if (evt.isPaused) {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = evt.title,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (evt.isPaused) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurface
                                                )
                                                
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    if (evt.isPaused) {
                                                        Badge(containerColor = Color(0xFFFEE2E2)) {
                                                            Text("POSTPONED", color = Color(0xFF991B1B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else {
                                                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                                            Text("ACTIVE", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = evt.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.DarkGray
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Timelapse, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("${evt.date} @ ${evt.time}", fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(evt.venue, fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "By: ${evt.organizerRole}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "Dept: ${evt.filterDepartment}",
                                                        fontSize = 9.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }

                                            if (isUserOrganizer && user?.role != "STUDENT") {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Pause Toggle
                                                    TextButton(
                                                        onClick = {
                                                            val updated = evt.copy(isPaused = !evt.isPaused)
                                                            viewModel.updateEvent(updated)
                                                            val msg = if (updated.isPaused) "Event postponed" else "Event active"
                                                            Toast.makeText(context, "$msg: ${evt.title}", Toast.LENGTH_SHORT).show()
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp).testTag("pause_event_${evt.id}")
                                                    ) {
                                                        Text(
                                                            text = if (evt.isPaused) "Resume" else "Postpone",
                                                            fontSize = 11.sp,
                                                            color = if (evt.isPaused) Color(0xFF16A34A) else Color(0xFFD97706)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(4.dp))

                                                    // Edit Event
                                                    TextButton(
                                                        onClick = {
                                                            isEditingEvent = evt
                                                            eventTitleVal = evt.title
                                                            eventDescVal = evt.description
                                                            eventDateVal = evt.date
                                                            eventTimeVal = evt.time
                                                            eventVenueVal = evt.venue
                                                            eventDeptFilter = evt.filterDepartment
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp).testTag("edit_event_${evt.id}")
                                                    ) {
                                                        Text("Edit", fontSize = 11.sp)
                                                    }

                                                    Spacer(modifier = Modifier.width(4.dp))

                                                    // Delete Event
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.deleteEvent(evt)
                                                            Toast.makeText(context, "Event deleted!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp).testTag("delete_event_${evt.id}")
                                                    ) {
                                                        Text("Delete", fontSize = 11.sp, color = Color.Red)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!showCreateForm && isEditingEvent == null) {
                    Button(
                        onClick = { showEventsDialog = false },
                        modifier = Modifier.testTag("close_events_dialog")
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ContactSupport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "College Help Desk Support",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Need official assistance? Reach out to our campus administrative support or developer teams directly below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_DIAL,
                                        android.net.Uri.parse("tel:+918247392437")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call Support",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Helpline Phone Number",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "+91 8247392437",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_SENDTO,
                                        android.net.Uri.parse("mailto:ranbidgesolutionspvtltd@gmail.com")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open email client", Toast.LENGTH_SHORT).show()
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email Support",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Official Email ID Support",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "ranbidgesolutionspvtltd@gmail.com",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    modifier = Modifier.testTag("close_help_dialog")
                ) {
                    Text("Close")
                }
            }
        )
    }

    SuccessAlertBanner(
        visible = showSuccessBanner,
        message = successBannerMsg,
        details = successBannerDetails,
        onDismiss = { showSuccessBanner = false },
        modifier = Modifier.align(Alignment.TopCenter)
    )
}
}

@Composable
fun QuickTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = labelColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = labelColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun formatStatus(status: String): String {
    return status.replace("PENDING_ADVISOR", "Pending Class Advisor")
        .replace("PENDING_HOD", "Pending HOD Approval")
        .replace("PENDING_WARDEN", "Pending Principal/Warden")
        .replace("APPROVED", "Approved")
        .replace("REJECTED", "Rejected")
}

@Composable
fun OutpassCountdownTimer(
    timestamp: Long,
    durationMs: Long = 4 * 3600 * 1000L, // 4 hours standard duration limit
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(key1 = timestamp) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }
    
    val expirationTime = timestamp + durationMs
    val remainingTimeMs = expirationTime - currentTime
    
    if (remainingTimeMs <= 0) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Expired",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "OUTPASS EXPIRED",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Your campus leave duration has exceeded the 4-hour limit! Please report/renew immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    } else {
        val hours = (remainingTimeMs / (1000 * 60 * 60)) % 24
        val minutes = (remainingTimeMs / (1000 * 60)) % 60
        val seconds = (remainingTimeMs / 1000) % 60
        val progress = remainingTimeMs.toFloat() / durationMs.toFloat()
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Campus Leave Duration Timer",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("Active Pass", fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Leave started upon HOD validation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = String.format("%02d:%02d:%02d remaining", hours, minutes, seconds),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        strokeWidth = 4.dp
                    )
                }
                
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (progress < 0.25f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
fun ActiveCanteenTokenCard(
    booking: com.example.data.model.CanteenBooking,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigate("CANTEEN") }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fastfood,
                        contentDescription = "Active Token",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active Meal E-Token",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                    Text(
                        text = "Ready to Collect",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show this barcode/token at the counter:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = booking.qrToken,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Items: ${booking.itemsJson}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                // Beautiful matrix grid representing QR barcode
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (j in 0..4) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                for (i in 0..4) {
                                    val color = if ((i + j) % 2 == 0) MaterialTheme.colorScheme.secondary else Color.White
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .background(color, RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Amount Paid: \u20B9${booking.totalCost}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Tap to open receipts • Counter 1",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun StudentRequestsHistoryCard(
    outpasses: List<OutpassRequest>,
    certificates: List<CertificateRequest>,
    stationeries: List<com.example.data.model.StationeryRequest>,
    prints: List<com.example.data.model.PrintRequest>,
    canteenBookings: List<com.example.data.model.CanteenBooking>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPending = outpasses.count { it.status.startsWith("PENDING") } +
            certificates.count { it.status.startsWith("PENDING") } +
            stationeries.count { it.status == "PENDING" } +
            prints.count { it.status == "PENDING" }
            
    val totalCompleted = outpasses.count { it.status == "APPROVED" } +
            certificates.count { it.status == "APPROVED" } +
            stationeries.count { it.status == "COLLECTED" } +
            prints.count { it.status == "COMPLETED" } +
            canteenBookings.count { it.status == "COLLECTED" || it.status == "COMPLETED" }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = "Requests tracking info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "My Requests & History",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(
                    onClick = { onNavigate("TRACK_REQUESTS") },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("View History", style = MaterialTheme.typography.labelMedium)
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Stats metrics summary row with beautiful styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Active/Pending", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("$totalPending Open", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Total Cleared", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Text("$totalCompleted Done", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            // Let's list the 2 most recent requests for immediate history feedback!
            val recentRequests = remember(outpasses, certificates, stationeries, prints, canteenBookings) {
                val list = mutableListOf<RequestSummaryItem>()
                
                outpasses.forEach {
                    list.add(
                        RequestSummaryItem(
                            title = "Outpass: ${it.qrText}",
                            subtitle = "Reason: ${it.reason}",
                            status = it.status,
                            cost = null,
                            dateLabel = it.dateTime,
                            type = "Outpasses",
                            timestamp = it.timestamp
                        )
                    )
                }
                
                certificates.forEach {
                    list.add(
                        RequestSummaryItem(
                            title = "Certificate: ${it.certificateType}",
                            subtitle = "Purpose: ${it.details}",
                            status = it.status,
                            cost = null,
                            dateLabel = "Documents",
                            type = "Certificates",
                            timestamp = it.timestamp
                        )
                    )
                }
                
                stationeries.forEach {
                    list.add(
                        RequestSummaryItem(
                            title = "Store Item: ${it.itemName}",
                            subtitle = "Qty: ${it.quantity} units",
                            status = it.status,
                            cost = "\u20B9${it.totalCost}",
                            dateLabel = "Store Preorder",
                            type = "Store",
                            timestamp = it.timestamp
                        )
                    )
                }

                prints.forEach {
                    list.add(
                        RequestSummaryItem(
                            title = "Print Doc: ${it.fileName}",
                            subtitle = "${it.pagesCount} pgs • ${it.printType}",
                            status = it.status,
                            cost = "\u20B9${it.totalCost}",
                            dateLabel = "Print Center",
                            type = "Prints",
                            timestamp = it.timestamp
                        )
                    )
                }

                canteenBookings.forEach {
                    list.add(
                        RequestSummaryItem(
                            title = "Food Token: ${it.qrToken}",
                            subtitle = "Items: ${it.itemsJson}",
                            status = it.status,
                            cost = "\u20B9${it.totalCost}",
                            dateLabel = "Canteen Token",
                            type = "Canteen",
                            timestamp = it.timestamp
                        )
                    )
                }

                list.sortByDescending { it.timestamp }
                list.take(2)
            }

            if (recentRequests.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "LATEST ACTIVITY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentRequests.forEach { req ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                val icon = when (req.type) {
                                    "Outpasses" -> Icons.AutoMirrored.Filled.DirectionsRun
                                    "Certificates" -> Icons.Default.CardMembership
                                    "Store" -> Icons.Default.Storefront
                                    "Prints" -> Icons.Default.Print
                                    else -> Icons.Default.Fastfood
                                }
                                Box(
                                    modifier = Modifier
                                        .size(31.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = req.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = req.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Badge(
                                    containerColor = when (req.status) {
                                        "APPROVED", "READY", "COLLECTED", "COMPLETED" -> Color(0xFF10B981)
                                        "REJECTED" -> Color.Red
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                ) {
                                    val readableStat = if (req.status.startsWith("PENDING")) "PENDING" else req.status
                                    Text(
                                        text = readableStat,
                                        fontSize = 8.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentOutpassHubCard(
    studentOutpasses: List<OutpassRequest>,
    currentUser: com.example.data.model.User?,
    onSubmitOutpass: (dateTime: String, reason: String, expectedReturnTime: String) -> Unit,
    onNavigate: (String) -> Unit
) {
    var isFormExpanded by remember { mutableStateOf(false) }
    var dateTimeInput by remember { mutableStateOf("Today, 4:00 PM") }
    var expectedReturnInput by remember { mutableStateOf("Today, 8:30 PM") }
    var reasonInput by remember { mutableStateOf("") }
    var parentContactInput by remember { mutableStateOf(currentUser?.parentContact ?: "+91 9885123456") }

    val presetReasons = listOf("Medical Check-up", "Weekend Visit", "Home Visit", "Exam/Interview", "Project Work")

    val activeRequests = remember(studentOutpasses) {
        studentOutpasses.filter { it.status.startsWith("PENDING") || it.status == "APPROVED" || it.status == "EXITED" }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_outpass_hub"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Campus Outpass Hub",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Manage and request exit permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(
                    onClick = { isFormExpanded = !isFormExpanded },
                    modifier = Modifier.testTag("dashboard_outpass_toggle_form")
                ) {
                    Icon(
                        imageVector = if (isFormExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Add,
                        contentDescription = "Toggle Request Form",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Active Requests Section
            Text(
                text = "ACTIVE REQUESTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )

            if (activeRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "No active outpass requests",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Need to leave campus? Submit a request below.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activeRequests.forEach { req ->
                        var isDetailsExpanded by remember { mutableStateOf(false) }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDetailsExpanded = !isDetailsExpanded }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Leave Time: ${req.dateTime}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Reason: ${req.reason}",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Badge(
                                        containerColor = when (req.status) {
                                            "APPROVED" -> Color(0xFF10B981)
                                            "EXITED" -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.tertiary
                                        },
                                        modifier = Modifier.testTag("dashboard_outpass_status_badge_${req.status}")
                                    ) {
                                        Text(
                                            text = when (req.status) {
                                                "PENDING_ADVISOR" -> "Awaiting Advisor"
                                                "PENDING_HOD" -> "Awaiting HOD"
                                                "APPROVED" -> "Approved - Ready"
                                                "EXITED" -> "Exited"
                                                else -> req.status
                                            },
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                OutpassTimeline(status = req.status)

                                if (isDetailsExpanded) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text("Return Expected: ${req.expectedReturnTime}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text("Parent Contact: ${req.parentContact}", fontSize = 11.sp)
                                        Text("Submitted: ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(req.timestamp))}", fontSize = 10.sp, color = Color.Gray)
                                        
                                        if (req.status == "APPROVED" || req.status == "EXITED") {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Button(
                                                onClick = { onNavigate("OUTPASS") },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                                contentPadding = PaddingValues(vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("View QR Pass & Ticket", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Tap to view full details and pass",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Form Section
            AnimatedVisibility(
                visible = isFormExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Text(
                        text = "REQUEST NEW OUTPASS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )

                    // Departure
                    OutlinedTextField(
                        value = dateTimeInput,
                        onValueChange = { dateTimeInput = it },
                        label = { Text("Departure Date & Time") },
                        placeholder = { Text("e.g. Today, 4:00 PM") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Timelapse, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_outpass_departure"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Return
                    OutlinedTextField(
                        value = expectedReturnInput,
                        onValueChange = { expectedReturnInput = it },
                        label = { Text("Expected Return Time") },
                        placeholder = { Text("e.g. Today, 8:30 PM") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Timelapse, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_outpass_return"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Reason
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("Reason for Leaving") },
                        placeholder = { Text("Select preset below or type here...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_outpass_reason"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick Chips for Reason
                    Text("Quick Reasons:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetReasons) { r ->
                            val isSelected = reasonInput == r
                            FilterChip(
                                selected = isSelected,
                                onClick = { reasonInput = r },
                                label = { Text(r, fontSize = 10.sp) },
                                modifier = Modifier.testTag("dashboard_outpass_quick_reason_chip_$r"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Contact
                    OutlinedTextField(
                        value = parentContactInput,
                        onValueChange = { parentContactInput = it },
                        label = { Text("Emergency Parent Contact") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_outpass_parent_contact"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val context = LocalContext.current
                    Button(
                        onClick = {
                            if (dateTimeInput.isBlank() || reasonInput.isBlank() || expectedReturnInput.isBlank()) {
                                Toast.makeText(context, "Please fill in all the required fields", Toast.LENGTH_SHORT).show()
                            } else {
                                onSubmitOutpass(dateTimeInput, reasonInput, expectedReturnInput)
                                Toast.makeText(context, "Outpass requested and saved to local state!", Toast.LENGTH_LONG).show()
                                // Clear/Reset
                                reasonInput = ""
                                isFormExpanded = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dashboard_outpass_submit")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Outpass Request")
                    }
                }
            }
        }
    }
}

@Composable
fun OutpassTimeline(status: String) {
    val stepSubmitted = true
    val stepAdvisor = status != "PENDING_ADVISOR" && status != "PENDING"
    val stepHod = status == "APPROVED" || status == "EXITED"
    val stepExit = status == "EXITED"

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineStep(label = "Submitted", checked = stepSubmitted, current = false)
        TimelineConnector(checked = stepAdvisor)
        TimelineStep(label = "Advisor", checked = stepAdvisor, current = status == "PENDING_ADVISOR")
        TimelineConnector(checked = stepHod)
        TimelineStep(label = "HOD", checked = stepHod, current = status == "PENDING_HOD" || status == "PENDING_WARDEN")
        TimelineConnector(checked = stepExit)
        TimelineStep(label = "Exit Gate", checked = stepExit, current = status == "APPROVED")
    }
}

@Composable
fun RowScope.TimelineStep(label: String, checked: Boolean, current: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        checked -> Color(0xFF10B981)
                        current -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .border(
                    width = 2.dp,
                    color = if (current) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = if (current) "•" else "",
                    color = if (current) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
            color = if (current) MaterialTheme.colorScheme.primary else Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TimelineConnector(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(16.dp)
            .height(3.dp)
            .background(if (checked) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant)
    )
}

