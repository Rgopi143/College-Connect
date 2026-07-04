package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.*

@Composable
fun TPCScreen(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val isStudent = user?.role == "STUDENT"

    val drives by viewModel.placementDrives.collectAsState()
    val applications by viewModel.placementApplications.collectAsState()
    val studentCgpaMap by viewModel.studentCgpa.collectAsState()

    // TPC directory details
    val tpcStaffList by viewModel.tpcStaffList.collectAsState()
    val departments by viewModel.departments.collectAsState()

    // 0 = Staff & Depts directory, 1 = Active Job Drives, 2 = Student Applications, 3 = Profile & Analytics
    var activeTab by remember { mutableStateOf(0) }

    // Dynamic state control
    var showAddDeptDialog by remember { mutableStateOf(false) }
    var selectedDeptForDetail by remember { mutableStateOf<Department?>(null) }
    var showCreateDriveDialog by remember { mutableStateOf(false) }
    var selectedDriveForDetail by remember { mutableStateOf<PlacementDrive?>(null) }
    var selectedAppForAction by remember { mutableStateOf<PlacementApplication?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-fidelity Upper Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Training & Placement Cell (TPC)",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Comprehensive Faculty, Staff Directories & Dynamic Recruitment Channels",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // TPC Navigation Tabs Row
        val tabsList = listOf(
            "Staff & Depts",
            "Job Drives",
            "Applications",
            if (isStudent) "My Profile" else "Metrics"
        )

        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabsList.forEachIndexed { idx, title ->
                Tab(
                    selected = activeTab == idx,
                    onClick = { activeTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (activeTab == idx) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        // Active workspace loader
        when (activeTab) {
            0 -> {
                // TPC Staff Directory & Department Grids
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Section 1: TPC Staff Directory
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TPC Core Coordinators",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "${tpcStaffList.size} Staff",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            tpcStaffList.forEach { staff ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = staff.name.split(" ").lastOrNull()?.take(1)?.uppercase() ?: "T",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = staff.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = staff.designation,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = staff.responsibility,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }


                }
            }
            1 -> {
                // Job Placement Drives Directory List
                Box(modifier = Modifier.weight(1f)) {
                    if (drives.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No active job recruitment drives posted yet", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(drives) { drive ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedDriveForDetail = drive }
                                        .border(
                                            width = 1.dp,
                                            color = if (drive.status == "Active") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = drive.companyName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Badge(
                                                containerColor = if (drive.status == "Active") Color(0xFF10B981) else Color.Gray,
                                                contentColor = Color.White
                                            ) {
                                                Text(drive.status, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = drive.roleName,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                                Text("Package: ${drive.packageCTC}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFF59E0B))
                                                Text("CGPA: >= ${drive.eligibilityCGPA}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Deadline: ${drive.deadline}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Red.copy(alpha = 0.7f),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Button(
                                                onClick = { selectedDriveForDetail = drive },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text("Details & Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!isStudent) {
                        FloatingActionButton(
                            onClick = { showCreateDriveDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Post Job Drive")
                        }
                    }
                }
            }
            2 -> {
                // Applications Hub (student app listing and recruitment workflows)
                val activeApps = if (isStudent) {
                    applications.filter { it.studentId == user?.userId }
                } else {
                    applications
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (activeApps.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No applications found in tracking log", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(activeApps) { app ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = borderModifierForStatus(app.status)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(app.companyName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(app.roleName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            }
                                            Badge(
                                                containerColor = badgeColorForStatus(app.status),
                                                contentColor = Color.White
                                            ) {
                                                Text(app.status.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (!isStudent) {
                                            Text(
                                                text = "Applicant: ${app.studentName} (${app.department})",
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "GPA: ${app.cgpa} | Resume: ${app.resumeUrl}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        } else {
                                            Text(
                                                text = "Resume Filed: ${app.resumeUrl}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    .weight(1f)
                                            ) {
                                                Text(
                                                    text = "Feedback: ${app.feedback}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            if (!isStudent) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = { selectedAppForAction = app },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(30.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = Color.White
                                                    )
                                                ) {
                                                    Text("Status Action", fontSize = 10.sp)
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
            3 -> {
                // Profile & Resume (Students) / Funnel Analytics (TPC Staff)
                if (isStudent) {
                    val uid = user?.userId ?: ""
                    val currentCgpa = studentCgpaMap[uid] ?: 8.0
                    var cgpaInput by remember { mutableStateOf(currentCgpa.toString()) }
                    var resumeInput by remember { mutableStateOf("${user?.name?.replace(" ", "_")}_Resume.pdf") }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("TPC Registration Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Your declared CGPA score and active Resume Link are automatically verified against academic thresholds during drive application filtration.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = cgpaInput,
                                onValueChange = { cgpaInput = it },
                                label = { Text("Cumulative Core CGPA") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = resumeInput,
                                onValueChange = { resumeInput = it },
                                label = { Text("Resume Portfolio PDF Name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val d = cgpaInput.toDoubleOrNull()
                                    if (d == null || d < 0.0 || d > 10.0) {
                                        Toast.makeText(context, "Invalid score metrics. Range is 0.0 to 10.0", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.updateStudentCgpa(d)
                                        Toast.makeText(context, "TPC Student Credentials updated!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save TPC Profile")
                            }
                        }
                    }
                } else {
                    // Staff recruitment analytics details
                    val totalApps = applications.size
                    val shortlisted = applications.count { it.status == "Shortlisted" }
                    val selected = applications.count { it.status == "Selected" }
                    val rejected = applications.count { it.status == "Rejected" }
                    val placementRatio = if (totalApps > 0) ((selected.toFloat() / totalApps.toFloat()) * 100).toInt() else 0

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text("Campus Recruitment Analytics Metrics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Acceptance Rate", style = MaterialTheme.typography.labelSmall)
                                        Text("$placementRatio%", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium)
                                        Text("Offers/Applicants", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Placements Secured", style = MaterialTheme.typography.labelSmall)
                                        Text(selected.toString(), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium)
                                        Text("Offer Letters", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Application Funnel Distribution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                                    StatProgressRow("Selected Candidates", selected, totalApps, Color(0xFF10B981))
                                    StatProgressRow("Shortlisted Candidates", shortlisted, totalApps, Color(0xFF3B82F6))
                                    StatProgressRow("Under Review", (totalApps - selected - shortlisted - rejected), totalApps, Color(0xFFF59E0B))
                                    StatProgressRow("Not Retained", rejected, totalApps, Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DYNAMIC ADD DEPARTMENT REQUIREMENTS FORM DIALOG
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

    // DEPARTMENT FACULTY LIST MODAL
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

                    Text(
                        text = "Core Faculties Grid",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

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

    // JOB DRIVE DETAILS / APPLY DIALOG
    if (selectedDriveForDetail != null) {
        val drive = selectedDriveForDetail!!
        val studentId = user?.userId ?: ""
        val currentCgpa = studentCgpaMap[studentId] ?: 8.0
        val isQualified = currentCgpa >= drive.eligibilityCGPA
        val isApplied = applications.any { it.driveId == drive.id && it.studentId == studentId }

        AlertDialog(
            onDismissRequest = { selectedDriveForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(drive.companyName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(drive.roleName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Recruitment Profile",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(drive.description, style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            InfoTextRow("Salary Package:", drive.packageCTC)
                            InfoTextRow("Cutoff Score:", "Min ${drive.eligibilityCGPA} CGPA")
                            InfoTextRow("Allowed Streams:", drive.eligibleBranches)
                            InfoTextRow("Application Limit:", drive.deadline)
                        }
                    }

                    if (isStudent && drive.status == "Active") {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isApplied) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE0F2FE), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Applied Successfully", fontWeight = FontWeight.Bold, color = Color(0xFF0369A1), fontSize = 12.sp)
                            }
                        } else {
                            if (isQualified) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Status Qualified (Your GPA: $currentCgpa)", fontWeight = FontWeight.Bold, color = Color(0xFF047857), fontSize = 12.sp)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Cutoff Unfulfilled (Your GPA: $currentCgpa | Threshold: ${drive.eligibilityCGPA})", fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isStudent && drive.status == "Active" && !isApplied && isQualified) {
                    Button(
                        onClick = {
                            viewModel.applyToPlacementDrive(drive.id, "${user?.name?.replace(" ", "")}_Resume.pdf")
                            selectedDriveForDetail = null
                            Toast.makeText(context, "Transmitted application details!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Apply Now")
                    }
                } else {
                    Button(onClick = { selectedDriveForDetail = null }) {
                        Text("Back")
                    }
                }
            },
            dismissButton = {
                if (isStudent && drive.status == "Active" && !isApplied) {
                    TextButton(onClick = { selectedDriveForDetail = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // STAFF TPC ACTION STATUS DIALOG
    if (selectedAppForAction != null) {
        val app = selectedAppForAction!!
        var feedbackValue by remember { mutableStateOf(app.feedback) }

        AlertDialog(
            onDismissRequest = { selectedAppForAction = null },
            title = { Text("Fulfill Placement Action: ${app.studentName}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Target Recruitment Role: ${app.companyName} - ${app.roleName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = feedbackValue,
                        onValueChange = { feedbackValue = it },
                        label = { Text("Recruiter Feedback Comment") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        onClick = {
                            viewModel.updateApplicationStatus(app.id, "Shortlisted", feedbackValue)
                            selectedAppForAction = null
                            Toast.makeText(context, "Application Shortlisted!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Shortlist")
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        onClick = {
                            viewModel.updateApplicationStatus(app.id, "Selected", feedbackValue)
                            selectedAppForAction = null
                            Toast.makeText(context, "Application Appointed/Selected!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Appoint/Select")
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        onClick = {
                            viewModel.updateApplicationStatus(app.id, "Rejected", feedbackValue)
                            selectedAppForAction = null
                            Toast.makeText(context, "Application Dropped/Rejected", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Reject")
                    }
                }
            }
        )
    }

    // CREATE DRIVE DIALOG
    if (showCreateDriveDialog) {
        var companyName by remember { mutableStateOf("") }
        var roleName by remember { mutableStateOf("") }
        var packageCTC by remember { mutableStateOf("") }
        var minCGPA by remember { mutableStateOf("7.0") }
        var branches by remember { mutableStateOf("CSE, ECE, EEE, IT") }
        var description by remember { mutableStateOf("") }
        var deadline by remember { mutableStateOf("June 30, 2026") }

        AlertDialog(
            onDismissRequest = { showCreateDriveDialog = false },
            title = { Text("Post Placement Drive Opportunity", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = roleName,
                            onValueChange = { roleName = it },
                            label = { Text("Designation Role") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = packageCTC,
                            onValueChange = { packageCTC = it },
                            label = { Text("CTC (e.g. 12.5 LPA)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = minCGPA,
                            onValueChange = { minCGPA = it },
                            label = { Text("Eligible CGPA Threshold") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = branches,
                            onValueChange = { branches = it },
                            label = { Text("Eligible Branches") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = deadline,
                            onValueChange = { deadline = it },
                            label = { Text("Deadline Date") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Job Description & Skill Requirements") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dCgpa = minCGPA.toDoubleOrNull() ?: 7.0
                        if (companyName.isBlank() || roleName.isBlank() || packageCTC.isBlank()) {
                            Toast.makeText(context, "Please fulfill primary credentials info", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.postPlacementDrive(
                                company = companyName,
                                role = roleName,
                                ctc = packageCTC,
                                cgpa = dCgpa,
                                branches = branches,
                                desc = description,
                                deadlineStr = deadline
                            )
                            showCreateDriveDialog = false
                            Toast.makeText(context, "New placement drive posted successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Broadcast Opportunity")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDriveDialog = false }) {
                    Text("Discard")
                }
            }
        )
    }
}

@Composable
fun InfoTextRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatProgressRow(label: String, count: Int, total: Int, progressColor: Color) {
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text("$count / $total", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            color = progressColor,
            trackColor = progressColor.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
        )
    }
}

fun badgeColorForStatus(status: String): Color {
    return when (status.uppercase()) {
        "SELECTED" -> Color(0xFF10B981)
        "SHORTLISTED" -> Color(0xFF3B82F6)
        "APPLIED" -> Color(0xFFF59E0B)
        "REJECTED" -> Color(0xFFEF4444)
        else -> Color.Gray
    }
}

fun borderModifierForStatus(status: String): androidx.compose.foundation.BorderStroke {
    val color = when (status.uppercase()) {
        "SELECTED" -> Color(0xFF10B981)
        "SHORTLISTED" -> Color(0xFF3B82F6)
        "APPLIED" -> Color(0xFFF59E0B)
        "REJECTED" -> Color(0xFFEF4444)
        else -> Color.Gray
    }
    return androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
}

