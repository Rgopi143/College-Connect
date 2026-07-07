package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import com.example.data.model.*
import com.example.ui.viewmodel.PortalViewModel

@Composable
fun AdminScreen(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val staffUser by viewModel.currentUser.collectAsState()

    val outpasses by viewModel.allOutpasses.collectAsState()
    val certificates by viewModel.allCertificates.collectAsState()
    val stationeryRequests by viewModel.allStationeryRequests.collectAsState()
    val printRequests by viewModel.allPrintRequests.collectAsState()
    val canteenBookings by viewModel.allCanteenBookings.collectAsState()
    val canteenMenuList by viewModel.canteenItems.collectAsState()
    val stationerySupplyList by viewModel.stationeryItems.collectAsState()

    val initialTab = remember(staffUser) {
        when (staffUser?.role) {
            "CANTEEN" -> "Canteen"
            "STORE" -> "Stationery"
            "SECURITY" -> "Outpasses"
            "ADMIN" -> "Allocations"
            "HOD" -> "Allocations"
            "PRINCIPAL" -> "Allocations"
            "MENTOR", "CLASS_ADVISOR" -> "Outpasses"
            "PA" -> "Certificates"
            else -> "Outpasses"
        }
    }
    var activeAdminTab by remember(staffUser) { mutableStateOf(initialTab) }
    val adminTabsList = remember(staffUser) {
        when (staffUser?.role) {
            "CANTEEN" -> listOf("Canteen", "Analytics")
            "STORE" -> listOf("Stationery", "Analytics")
            "SECURITY" -> listOf("Outpasses", "History Logs", "Message Chat")
            "HOD" -> listOf("Allocations", "Outpasses", "Certificates", "History", "Message Chat", "Analytics")
            "MENTOR", "CLASS_ADVISOR" -> listOf("Outpasses", "Certificates", "History", "Analytics")
            "PRINCIPAL" -> listOf("Allocations", "Certificates", "History", "Manage Employees", "Message Chat", "Analytics")
            "PA" -> listOf("Certificates", "History")
            "ADMIN" -> listOf("Allocations", "Manage Employees", "Outpasses", "Certificates", "History", "Stationery", "Print Center", "Canteen", "Message Chat", "Analytics")
            else -> listOf("Outpasses")
        }
    }

    // Outpass rejection comment box
    var outpassRejectReason by remember { mutableStateOf("") }
    var selectedOutpassForAction by remember { mutableStateOf<OutpassRequest?>(null) }
    var showRejectDialog by remember { mutableStateOf(false) }

    var selectedOutpassForDetails by remember { mutableStateOf<OutpassRequest?>(null) }
    val allUsers by viewModel.allUsers.collectAsState()

    // Stationery add stock states
    var statIdInput by remember { mutableStateOf("pens") }
    var statQtyInput by remember { mutableStateOf("25") }
    var statPriceInput by remember { mutableStateOf("15.0") }
    var stationerySearchQuery by remember { mutableStateOf("") }

    // Print Center Multi-Select & Batch states
    var printSearchQuery by remember { mutableStateOf("") }
    var printStatusFilter by remember { mutableStateOf("All") }
    val selectedPrintIds = remember { mutableStateListOf<Int>() }

    // Manage Employees States
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showEditEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToEdit by remember { mutableStateOf<User?>(null) }
        var employeeSearchQuery by remember { mutableStateOf("") }
    var employeeRoleFilter by remember { mutableStateOf("All") }

    // CSV Batch Student Import states
    var csvFileName by remember { mutableStateOf<String?>(null) }
    var csvSuccessMsg by remember { mutableStateOf<String?>(null) }
    var csvErrorMsg by remember { mutableStateOf<String?>(null) }
    var csvParsedUsers by remember { mutableStateOf<List<User>>(emptyList()) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val nameResult = getFileNameHelper(context, uri)
            csvFileName = nameResult
            csvSuccessMsg = null
            csvErrorMsg = null
            csvParsedUsers = emptyList()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = inputStream.bufferedReader()
                    val lines = reader.readLines()
                    if (lines.isEmpty()) {
                        csvErrorMsg = "The selected CSV file is empty."
                        return@rememberLauncherForActivityResult
                    }

                    val list = mutableListOf<User>()
                    val headerLine = lines.firstOrNull()?.split(",")?.map { it.trim().lowercase() } ?: emptyList()
                    val hasHeaders = headerLine.any { it.contains("roll") || it.contains("id") || it.contains("name") || it.contains("email") }
                    
                    val startIdx = if (hasHeaders) 1 else 0
                    
                    var rollIdx = 0
                    var nameIdx = 1
                    var emailIdx = 2
                    var phoneIdx = 3
                    var parentIdx = 4
                    var passIdx = 5

                    if (hasHeaders) {
                        rollIdx = headerLine.indexOfFirst { it.contains("roll") || it.contains("id") }.let { if (it != -1) it else 0 }
                        nameIdx = headerLine.indexOfFirst { it.contains("name") }.let { if (it != -1) it else 1 }
                        emailIdx = headerLine.indexOfFirst { it.contains("email") }.let { if (it != -1) it else 2 }
                        phoneIdx = headerLine.indexOfFirst { it.contains("phone") || it.contains("mobile") }.let { if (it != -1) it else 3 }
                        parentIdx = headerLine.indexOfFirst { it.contains("parent") || it.contains("father") || it.contains("mother") }.let { if (it != -1) it else 4 }
                        passIdx = headerLine.indexOfFirst { it.contains("pass") }.let { if (it != -1) it else 5 }
                    }

                    for (i in startIdx until lines.size) {
                        val line = lines[i].trim()
                        if (line.isEmpty()) continue
                        val parts = parseCsvLineHelper(line)
                        if (parts.isNotEmpty()) {
                            val roll = parts.getOrNull(rollIdx)?.trim() ?: ""
                            if (roll.isNotEmpty()) {
                                val name = parts.getOrNull(nameIdx)?.trim()?.takeIf { it.isNotEmpty() } ?: "Student $roll"
                                val email = parts.getOrNull(emailIdx)?.trim()?.takeIf { it.isNotEmpty() } ?: "${roll.lowercase()}@nrtec.in"
                                val phone = parts.getOrNull(phoneIdx)?.trim()?.takeIf { it.isNotEmpty() } ?: "+91 9000000000"
                                val parent = parts.getOrNull(parentIdx)?.trim()?.takeIf { it.isNotEmpty() } ?: "+91 9111111111"
                                val password = parts.getOrNull(passIdx)?.trim()?.takeIf { it.isNotEmpty() } ?: "pass"
                                
                                val inferredDept = if (staffUser?.role == "HOD") {
                                    staffUser?.department ?: "Computer Science"
                                } else {
                                    getDeptFromRollNumber(roll) ?: "Computer Science"
                                }

                                list.add(
                                    User(
                                        userId = roll,
                                        name = name,
                                        rollNumber = roll,
                                        department = inferredDept,
                                        email = email,
                                        phone = phone,
                                        parentContact = parent,
                                        role = "STUDENT",
                                        password = password,
                                        isLoggedIn = false,
                                        isPaused = false
                                    )
                                )
                            }
                        }
                    }

                    if (list.isEmpty()) {
                        csvErrorMsg = "No valid student records found in the CSV. Make sure the first column has Roll Numbers."
                    } else {
                        csvParsedUsers = list
                        csvSuccessMsg = "Successfully parsed ${list.size} students from $nameResult! Click 'Import to Firestore' below to save."
                    }
                }
            } catch (e: Exception) {
                csvErrorMsg = "Error parsing CSV: ${e.localizedMessage}"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = if (staffUser?.role == "MENTOR" || staffUser?.role == "CLASS_ADVISOR") "Student Requests Panel" else "Staff Administration Panel",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Authorized Session: ${staffUser?.name} • Role: ${staffUser?.role}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        // Tab Row Slider
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(adminTabsList) { tab ->
                ElevatedFilterChip(
                    selected = activeAdminTab == tab,
                    onClick = { activeAdminTab = tab },
                    label = { Text(tab, fontSize = 11.sp) }
                )
            }
        }

        Box(modifier = Modifier.weight(1.0f)) {
            when (activeAdminTab) {
                "Allocations" -> {
                    val currentStaff = staffUser
                    var studentToAllocateMentor by remember { mutableStateOf<User?>(null) }
                    var studentToAllocateAdvisor by remember { mutableStateOf<User?>(null) }
                    var deptFilter by remember { mutableStateOf(if (currentStaff?.role == "HOD") currentStaff.department else "All") }
                    var searchQuery by remember { mutableStateOf("") }
                    var isBulkCardExpanded by remember { mutableStateOf(false) }
                    var isCsvCardExpanded by remember { mutableStateOf(false) }
                    var bulkInputText by remember { mutableStateOf("") }
                    var bulkSuccessMsg by remember { mutableStateOf<String?>(null) }
                    var bulkErrorMsg by remember { mutableStateOf<String?>(null) }

                    val depts = remember(allUsers) {
                        listOf("All") + allUsers.filter { it.role == "STUDENT" }.map { it.department }.distinct()
                    }

                    val filteredStudents = allUsers.filter { student ->
                        val isStudent = student.role == "STUDENT"
                        val matchesDept = if (currentStaff?.role == "HOD") {
                            student.department.equals(currentStaff.department, ignoreCase = true)
                        } else {
                            deptFilter == "All" || student.department.equals(deptFilter, ignoreCase = true)
                        }
                        val matchesSearch = student.name.contains(searchQuery, ignoreCase = true) ||
                                            student.rollNumber.contains(searchQuery, ignoreCase = true)
                        isStudent && matchesDept && matchesSearch
                    }

                    val allMentors = allUsers.filter { it.role == "MENTOR" }
                    val allAdvisors = allUsers.filter { it.role == "CLASS_ADVISOR" }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Faculty & Mentor Allocations",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (currentStaff?.role == "HOD") {
                                    "Allocate Academic Mentors and Class Advisors for ${currentStaff.department} students"
                                } else {
                                    "Allocate Academic Mentors and Class Advisors for students across departments"
                                },
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        // Bulk Student Registration Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .testTag("bulk_add_students_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isBulkCardExpanded = !isBulkCardExpanded }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GroupAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = "Bulk Student Registration",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (currentStaff?.role == "HOD") {
                                                    "Add multiple students directly to ${currentStaff.department}"
                                                } else {
                                                    "Add multiple students across departments"
                                                },
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (isBulkCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand/Collapse"
                                    )
                                }

                                if (isBulkCardExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Enter students one per line.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Format Options:\n1. RollNumber, Full Name (e.g. 23CS101, Alice Vance)\n2. Just RollNumber (e.g. 23CS105)",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    OutlinedTextField(
                                        value = bulkInputText,
                                        onValueChange = {
                                            bulkInputText = it
                                            bulkSuccessMsg = null
                                            bulkErrorMsg = null
                                        },
                                        placeholder = {
                                            Text(
                                                "23CS101, Alice Vance\n23CS102, Bob Dylan\n23CS103",
                                                fontSize = 12.sp
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .testTag("bulk_input_field"),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    bulkSuccessMsg?.let { msg ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = Color(0xFFD1FAE5),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = msg,
                                                color = Color(0xFF065F46),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    bulkErrorMsg?.let { msg ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = msg,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            val lines = bulkInputText.lines()
                                            var successCount = 0
                                            var errorCount = 0
                                            lines.forEach { line ->
                                                val trimmed = line.trim()
                                                if (trimmed.isNotEmpty()) {
                                                    val parts = trimmed.split(",", limit = 2)
                                                    val roll = parts[0].trim()
                                                    if (roll.isNotEmpty()) {
                                                        val name = if (parts.size > 1 && parts[1].trim().isNotEmpty()) {
                                                            parts[1].trim()
                                                        } else {
                                                            "Student $roll"
                                                        }
                                                        
                                                        val dept = if (currentStaff?.role == "HOD") {
                                                            currentStaff.department
                                                        } else {
                                                            getDeptFromRollNumber(roll) ?: (if (deptFilter != "All") deptFilter else "Computer Science")
                                                        }
                                                        
                                                        viewModel.registerNewStudent(
                                                            userId = roll,
                                                            name = name,
                                                            roll = roll,
                                                            dept = dept
                                                        )
                                                        successCount++
                                                    } else {
                                                        errorCount++
                                                    }
                                                }
                                            }
                                            if (successCount > 0) {
                                                bulkSuccessMsg = "Successfully registered $successCount students!"
                                                bulkInputText = ""
                                                if (errorCount > 0) {
                                                    bulkSuccessMsg += " ($errorCount invalid lines skipped)"
                                                }
                                                bulkErrorMsg = null
                                            } else {
                                                bulkErrorMsg = "No valid student entries found to register."
                                                bulkSuccessMsg = null
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("save_bulk_students_btn"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Register Students in Bulk", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // CSV Batch Student Import Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .testTag("csv_batch_import_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isCsvCardExpanded = !isCsvCardExpanded }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = "CSV Batch Student Import",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (currentStaff?.role == "HOD") {
                                                    "Upload CSV to batch import to ${currentStaff.department} Firestore"
                                                } else {
                                                    "Upload CSV to batch import students across departments"
                                                },
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (isCsvCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand/Collapse"
                                    )
                                }

                                if (isCsvCardExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Import student lists directly from a CSV file.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "CSV Columns (Header optional):\n1. Roll Number (Required)\n2. Full Name (Optional)\n3. Email (Optional)\n4. Mobile (Optional)\n5. Parent Contact (Optional)\n6. Password (Optional)",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { csvLauncher.launch("*/*") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("select_csv_button"),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Select CSV File", fontSize = 11.sp)
                                        }

                                        if (csvFileName != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    csvFileName = null
                                                    csvSuccessMsg = null
                                                    csvErrorMsg = null
                                                    csvParsedUsers = emptyList()
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear file", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    csvFileName?.let { name ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Selected: $name",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    csvSuccessMsg?.let { msg ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = Color(0xFFD1FAE5),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = msg,
                                                color = Color(0xFF065F46),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    csvErrorMsg?.let { msg ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = msg,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    if (csvParsedUsers.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Preview (${csvParsedUsers.size} records found):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        // Scrollable horizontal list preview of first few parsed records
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 120.dp)
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .padding(6.dp)
                                        ) {
                                            LazyColumn(
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(csvParsedUsers) { usr ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("${usr.rollNumber} - ${usr.name}", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                                        Text(usr.department, fontSize = 9.sp, color = Color.Gray)
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                var count = 0
                                                csvParsedUsers.forEach { usr ->
                                                    viewModel.registerNewUser(
                                                        userId = usr.userId,
                                                        name = usr.name,
                                                        roll = usr.rollNumber,
                                                        dept = usr.department,
                                                        email = usr.email,
                                                        role = usr.role,
                                                        password = usr.password
                                                    )
                                                    count++
                                                }
                                                csvSuccessMsg = "Successfully imported $count student records to Firestore Database live!"
                                                csvParsedUsers = emptyList()
                                                csvFileName = null
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("import_csv_button"),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Import Students to Firestore", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search student name/roll...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(52.dp)
                            )

                            if (currentStaff?.role != "HOD") {
                                var showDeptDropdown by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { showDeptDropdown = true },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text("Dept: $deptFilter")
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = showDeptDropdown,
                                        onDismissRequest = { showDeptDropdown = false }
                                    ) {
                                        depts.forEach { dept ->
                                            DropdownMenuItem(
                                                text = { Text(dept) },
                                                onClick = {
                                                    deptFilter = dept
                                                    showDeptDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredStudents.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No students found in department \"$deptFilter\" matching search.",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredStudents) { student ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = student.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Roll: ${student.rollNumber} • ${student.department}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                                    Text(
                                                        text = "Student",
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Academic Mentor",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = student.assignedMentorName ?: "Not Allocated",
                                                        fontSize = 13.sp,
                                                        fontWeight = if (student.assignedMentorName != null) FontWeight.Medium else FontWeight.Normal,
                                                        color = if (student.assignedMentorName != null) MaterialTheme.colorScheme.onSurface else Color.Gray
                                                    )
                                                }
                                                Button(
                                                    onClick = { studentToAllocateMentor = student },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (student.assignedMentorName != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = if (student.assignedMentorName != null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                                    ),
                                                    modifier = Modifier.height(36.dp).testTag("allocate_mentor_${student.userId}")
                                                ) {
                                                    Text(
                                                        text = if (student.assignedMentorName != null) "Change" else "Allocate",
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Class Advisor (Class Teacher)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                    Text(
                                                        text = student.assignedAdvisorName ?: "Not Allocated",
                                                        fontSize = 13.sp,
                                                        fontWeight = if (student.assignedAdvisorName != null) FontWeight.Medium else FontWeight.Normal,
                                                        color = if (student.assignedAdvisorName != null) MaterialTheme.colorScheme.onSurface else Color.Gray
                                                    )
                                                }
                                                Button(
                                                    onClick = { studentToAllocateAdvisor = student },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (student.assignedAdvisorName != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = if (student.assignedAdvisorName != null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                                    ),
                                                    modifier = Modifier.height(36.dp).testTag("allocate_advisor_${student.userId}")
                                                ) {
                                                    Text(
                                                        text = if (student.assignedAdvisorName != null) "Change" else "Allocate",
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    studentToAllocateMentor?.let { targetStudent ->
                        val deptMentors = allMentors.filter {
                            targetStudent.department.equals(it.department, ignoreCase = true)
                        }
                        val mentorsToShow = if (deptMentors.isNotEmpty()) deptMentors else allMentors

                        AlertDialog(
                            onDismissRequest = { studentToAllocateMentor = null },
                            title = { Text("Allocate Academic Mentor") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Select a Mentor for ${targetStudent.name} (${targetStudent.department}):", fontSize = 13.sp)
                                    if (mentorsToShow.isEmpty()) {
                                        Text("No Mentor accounts found in this department. Create or register a MENTOR first.", color = Color.Red, fontSize = 12.sp)
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.heightIn(max = 250.dp)
                                        ) {
                                            items(mentorsToShow) { mentor ->
                                                val isCurrent = targetStudent.assignedMentorId == mentor.userId
                                                Card(
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            val updated = targetStudent.copy(
                                                                assignedMentorId = mentor.userId,
                                                                assignedMentorName = mentor.name
                                                            )
                                                            viewModel.updateUserAdmin(updated)
                                                            studentToAllocateMentor = null
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(mentor.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                            Text("Dept: ${mentor.department}", fontSize = 11.sp, color = Color.Gray)
                                                        }
                                                        if (isCurrent) {
                                                            Icon(Icons.Default.Check, contentDescription = "Currently Allocated", tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { studentToAllocateMentor = null }) {
                                    Text("Cancel")
                                }
                            },
                            dismissButton = {
                                if (targetStudent.assignedMentorName != null) {
                                    TextButton(
                                        onClick = {
                                            val updated = targetStudent.copy(
                                                assignedMentorId = null,
                                                assignedMentorName = null
                                            )
                                            viewModel.updateUserAdmin(updated)
                                            studentToAllocateMentor = null
                                        }
                                    ) {
                                        Text("De-allocate", color = Color.Red)
                                    }
                                }
                            }
                        )
                    }

                    studentToAllocateAdvisor?.let { targetStudent ->
                        val deptAdvisors = allAdvisors.filter {
                            targetStudent.department.equals(it.department, ignoreCase = true)
                        }
                        val advisorsToShow = if (deptAdvisors.isNotEmpty()) deptAdvisors else allAdvisors

                        AlertDialog(
                            onDismissRequest = { studentToAllocateAdvisor = null },
                            title = { Text("Allocate Class Advisor") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Select a Class Advisor for ${targetStudent.name} (${targetStudent.department}):", fontSize = 13.sp)
                                    if (advisorsToShow.isEmpty()) {
                                        Text("No Class Advisor accounts found in this department. Create or register a CLASS_ADVISOR first.", color = Color.Red, fontSize = 12.sp)
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.heightIn(max = 250.dp)
                                        ) {
                                            items(advisorsToShow) { advisor ->
                                                val isCurrent = targetStudent.assignedAdvisorId == advisor.userId
                                                Card(
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            val updated = targetStudent.copy(
                                                                assignedAdvisorId = advisor.userId,
                                                                assignedAdvisorName = advisor.name
                                                            )
                                                            viewModel.updateUserAdmin(updated)
                                                            studentToAllocateAdvisor = null
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(advisor.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                            Text("Dept: ${advisor.department}", fontSize = 11.sp, color = Color.Gray)
                                                        }
                                                        if (isCurrent) {
                                                            Icon(Icons.Default.Check, contentDescription = "Currently Allocated", tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { studentToAllocateAdvisor = null }) {
                                    Text("Cancel")
                                }
                            },
                            dismissButton = {
                                if (targetStudent.assignedAdvisorName != null) {
                                    TextButton(
                                        onClick = {
                                            val updated = targetStudent.copy(
                                                assignedAdvisorId = null,
                                                assignedAdvisorName = null
                                            )
                                            viewModel.updateUserAdmin(updated)
                                            studentToAllocateAdvisor = null
                                        }
                                    ) {
                                        Text("De-allocate", color = Color.Red)
                                    }
                                }
                            }
                        )
                    }
                }

                "Manage Employees" -> {
                    val context = LocalContext.current
                    val employeeListByRole = allUsers.filter { user ->
                        val matchesRole = if (employeeRoleFilter == "All") {
                            user.role != "STUDENT"
                        } else {
                            user.role.equals(employeeRoleFilter, ignoreCase = true)
                        }
                        val matchesQuery = user.name.contains(employeeSearchQuery, ignoreCase = true) ||
                                           user.userId.contains(employeeSearchQuery, ignoreCase = true) ||
                                           user.department.contains(employeeSearchQuery, ignoreCase = true)
                        matchesRole && matchesQuery
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Employee Directory",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Active, paused, and registered staff members (${employeeListByRole.size} total)",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Button(
                                onClick = { showAddEmployeeDialog = true },
                                modifier = Modifier.testTag("add_employee_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Employee", fontSize = 12.sp)
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = employeeSearchQuery,
                                    onValueChange = { employeeSearchQuery = it },
                                    modifier = Modifier.fillMaxWidth().testTag("employee_search_input"),
                                    placeholder = { Text("Search by name, ID or department...", fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        if (employeeSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { employeeSearchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                )

                                val roles = listOf("All", "PRINCIPAL", "HOD", "WARDEN", "MENTOR", "CLASS_ADVISOR", "CANTEEN", "STORE", "SECURITY", "ADMIN", "PA")
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(roles) { role ->
                                        val isSelected = employeeRoleFilter == role
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { employeeRoleFilter = role },
                                            label = { Text(role, fontSize = 11.sp) },
                                            modifier = Modifier.testTag("role_filter_chip_$role")
                                        )
                                    }
                                }
                            }
                        }

                        if (employeeListByRole.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No employees match your search or filter.", fontSize = 13.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(employeeListByRole) { emp ->
                                    val borderStroke = if (emp.isPaused) {
                                        BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    }
                                    val cardBg = if (emp.isPaused) {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("employee_card_${emp.userId}"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = borderStroke,
                                        colors = CardDefaults.cardColors(containerColor = cardBg)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .background(
                                                                if (emp.isPaused) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.primaryContainer,
                                                                shape = RoundedCornerShape(18.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = emp.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase(),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = if (emp.isPaused) Color(0xFF7F1D1D) else MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = emp.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                        Text(
                                                            text = "ID: ${emp.userId} • Dept: ${emp.department}",
                                                            fontSize = 11.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    if (emp.isPaused) {
                                                        Badge(containerColor = Color(0xFFFEE2E2)) {
                                                            Text("PAUSED", color = Color(0xFFB91C1C), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                                        Text(emp.role, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(emp.email, fontSize = 11.sp, color = Color.DarkGray, maxLines = 1)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(emp.phone, fontSize = 11.sp, color = Color.DarkGray, maxLines = 1)
                                                }
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val updated = emp.copy(isPaused = !emp.isPaused)
                                                        viewModel.updateUserAdmin(updated)
                                                        val msg = if (updated.isPaused) "Employee account paused" else "Employee account active"
                                                        Toast.makeText(context, "$msg: ${updated.name}", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = if (emp.isPaused) Color(0xFF16A34A) else Color(0xFFD97706)
                                                    ),
                                                    modifier = Modifier.height(32.dp).testTag("pause_toggle_${emp.userId}"),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, if (emp.isPaused) Color(0xFF16A34A).copy(alpha = 0.5f) else Color(0xFFD97706).copy(alpha = 0.5f))
                                                ) {
                                                    Icon(
                                                        imageVector = if (emp.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (emp.isPaused) "Resume" else "Pause", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    FilledTonalIconButton(
                                                        onClick = {
                                                            employeeToEdit = emp
                                                            showEditEmployeeDialog = true
                                                        },
                                                        modifier = Modifier.size(32.dp).testTag("edit_employee_${emp.userId}")
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit details", modifier = Modifier.size(14.dp))
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            viewModel.deleteUser(emp)
                                                            Toast.makeText(context, "Employee deleted: ${emp.name}", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            contentColor = Color(0xFFDC2626),
                                                            containerColor = Color(0xFFFEE2E2)
                                                        ),
                                                        modifier = Modifier.size(32.dp).testTag("delete_employee_${emp.userId}")
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete employee", modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showAddEmployeeDialog) {
                        var idVal by remember { mutableStateOf("") }
                        var nameVal by remember { mutableStateOf("") }
                        var codeVal by remember { mutableStateOf("") }
                        var deptVal by remember { mutableStateOf("") }
                        var emailVal by remember { mutableStateOf("") }
                        var phoneVal by remember { mutableStateOf("") }
                        var roleVal by remember { mutableStateOf("HOD") }
                        var inputError by remember { mutableStateOf<String?>(null) }

                        val roleOptions = listOf("PRINCIPAL", "HOD", "WARDEN", "MENTOR", "CLASS_ADVISOR", "CANTEEN", "STORE", "SECURITY", "ADMIN", "PA")

                        AlertDialog(
                            onDismissRequest = { showAddEmployeeDialog = false },
                            confirmButton = {
                                TextButton(
                                    modifier = Modifier.testTag("submit_add_employee_confirm"),
                                    onClick = {
                                        if (idVal.isBlank() || nameVal.isBlank() || deptVal.isBlank()) {
                                            inputError = "ID, Name and Department are required."
                                        } else {
                                            val newEmp = User(
                                                userId = idVal.trim(),
                                                name = nameVal.trim(),
                                                rollNumber = if (codeVal.isBlank()) "STAFF_${idVal.trim().uppercase()}" else codeVal.trim(),
                                                department = deptVal.trim(),
                                                email = if (emailVal.isBlank()) "${idVal.trim().lowercase()}@college.edu" else emailVal.trim(),
                                                phone = if (phoneVal.isBlank()) "+91 9000000000" else phoneVal.trim(),
                                                parentContact = "N/A",
                                                role = roleVal,
                                                isLoggedIn = false,
                                                isPaused = false
                                            )
                                            viewModel.saveUser(newEmp)
                                            Toast.makeText(context, "Employee added successfully: ${newEmp.name}", Toast.LENGTH_SHORT).show()
                                            showAddEmployeeDialog = false
                                        }
                                    }
                                ) {
                                    Text("Add")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddEmployeeDialog = false }) {
                                    Text("Cancel")
                                }
                            },
                            title = { Text("Add New Employee", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (inputError != null) {
                                        Text(inputError!!, color = Color.Red, fontSize = 12.sp)
                                    }

                                    OutlinedTextField(
                                        value = idVal,
                                        onValueChange = { idVal = it },
                                        label = { Text("User ID (Unique Username)") },
                                        modifier = Modifier.fillMaxWidth().testTag("add_emp_id"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = nameVal,
                                        onValueChange = { nameVal = it },
                                        label = { Text("Full Name") },
                                        modifier = Modifier.fillMaxWidth().testTag("add_emp_name"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = codeVal,
                                        onValueChange = { codeVal = it },
                                        label = { Text("Employee Code / Staff Roll (Optional)") },
                                        modifier = Modifier.fillMaxWidth().testTag("add_emp_code"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = deptVal,
                                        onValueChange = { deptVal = it },
                                        label = { Text("Department (e.g. Computer Science)") },
                                        modifier = Modifier.fillMaxWidth().testTag("add_emp_dept"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = emailVal,
                                        onValueChange = { emailVal = it },
                                        label = { Text("Email (Optional)") },
                                        modifier = Modifier.fillMaxWidth().testTag("add_emp_email"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )

                                    OutlinedTextField(
                                        value = phoneVal,
                                        onValueChange = { phoneVal = it },
                                        label = { Text("Phone Number (Optional)") },
                                        modifier = Modifier.fillMaxWidth().testTag("add_emp_phone"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                    )

                                    Text("Select Designation / Role:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        roleOptions.chunked(3).forEach { rowRoles ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                rowRoles.forEach { roleOpt ->
                                                    ElevatedFilterChip(
                                                        selected = roleVal == roleOpt,
                                                        onClick = { roleVal = roleOpt },
                                                        label = { Text(roleOpt, fontSize = 9.sp) },
                                                        modifier = Modifier.weight(1f).testTag("dialog_role_$roleOpt")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    if (showEditEmployeeDialog && employeeToEdit != null) {
                        val emp = employeeToEdit!!
                        var nameVal by remember { mutableStateOf(emp.name) }
                        var codeVal by remember { mutableStateOf(emp.rollNumber) }
                        var deptVal by remember { mutableStateOf(emp.department) }
                        var emailVal by remember { mutableStateOf(emp.email) }
                        var phoneVal by remember { mutableStateOf(emp.phone) }
                        var roleVal by remember { mutableStateOf(emp.role) }
                        var inputError by remember { mutableStateOf<String?>(null) }

                        val roleOptions = listOf("PRINCIPAL", "HOD", "WARDEN", "MENTOR", "CLASS_ADVISOR", "CANTEEN", "STORE", "SECURITY", "ADMIN", "PA")

                        AlertDialog(
                            onDismissRequest = { 
                                showEditEmployeeDialog = false
                                employeeToEdit = null
                            },
                            confirmButton = {
                                TextButton(
                                    modifier = Modifier.testTag("submit_edit_employee_confirm"),
                                    onClick = {
                                        if (nameVal.isBlank() || deptVal.isBlank()) {
                                            inputError = "Name and Department are required."
                                        } else {
                                            val updatedEmp = emp.copy(
                                                name = nameVal.trim(),
                                                rollNumber = codeVal.trim(),
                                                department = deptVal.trim(),
                                                email = emailVal.trim(),
                                                phone = phoneVal.trim(),
                                                role = roleVal
                                            )
                                            viewModel.updateUserAdmin(updatedEmp)
                                            Toast.makeText(context, "Employee updated successfully: ${updatedEmp.name}", Toast.LENGTH_SHORT).show()
                                            showEditEmployeeDialog = false
                                            employeeToEdit = null
                                        }
                                    }
                                ) {
                                    Text("Save Changes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { 
                                    showEditEmployeeDialog = false
                                    employeeToEdit = null
                                }) {
                                    Text("Cancel")
                                }
                            },
                            title = { Text("Edit Employee details", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (inputError != null) {
                                        Text(inputError!!, color = Color.Red, fontSize = 12.sp)
                                    }

                                    OutlinedTextField(
                                        value = emp.userId,
                                        onValueChange = {},
                                        label = { Text("User ID (Cannot be changed)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = false
                                    )

                                    OutlinedTextField(
                                        value = nameVal,
                                        onValueChange = { nameVal = it },
                                        label = { Text("Full Name") },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_emp_name"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = codeVal,
                                        onValueChange = { codeVal = it },
                                        label = { Text("Employee Code / Staff Roll") },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_emp_code"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = deptVal,
                                        onValueChange = { deptVal = it },
                                        label = { Text("Department") },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_emp_dept"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = emailVal,
                                        onValueChange = { emailVal = it },
                                        label = { Text("Email") },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_emp_email"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )

                                    OutlinedTextField(
                                        value = phoneVal,
                                        onValueChange = { phoneVal = it },
                                        label = { Text("Phone Number") },
                                        modifier = Modifier.fillMaxWidth().testTag("edit_emp_phone"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                    )

                                    Text("Designation / Role:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        roleOptions.chunked(3).forEach { rowRoles ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                rowRoles.forEach { roleOpt ->
                                                    ElevatedFilterChip(
                                                        selected = roleVal == roleOpt,
                                                        onClick = { roleVal = roleOpt },
                                                        label = { Text(roleOpt, fontSize = 9.sp) },
                                                        modifier = Modifier.weight(1f).testTag("dialog_edit_role_$roleOpt")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                "Outpasses" -> {
                    val actionableOutpasses = outpasses.filter {
                        ((staffUser?.role == "MENTOR" || staffUser?.role == "CLASS_ADVISOR") && (it.status == "PENDING_MENTOR" || it.status == "PENDING_ADVISOR")) ||
                        (staffUser?.role == "HOD" && it.status == "PENDING_HOD") ||
                        (staffUser?.role == "SECURITY" && it.status == "PENDING_SECURITY") ||
                        (staffUser?.role == "ADMIN")
                    }

                    var outpassSearchQuery by remember { mutableStateOf("") }
                    val filteredOutpasses = actionableOutpasses.filter {
                        outpassSearchQuery.isBlank() ||
                        it.studentName.contains(outpassSearchQuery, ignoreCase = true) ||
                        it.rollNumber.contains(outpassSearchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = outpassSearchQuery,
                            onValueChange = { outpassSearchQuery = it },
                            placeholder = { Text("Search student name/roll...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (outpassSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { outpassSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("outpass_search_field")
                        )

                        if (filteredOutpasses.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (outpassSearchQuery.isEmpty()) 
                                        "No pending outpass clearance requests found for your role."
                                    else 
                                        "No outpass requests match \"$outpassSearchQuery\"",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredOutpasses) { req ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedOutpassForDetails = req }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(req.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("Roll: ${req.rollNumber} • Dept: ${req.department}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                                    Text(formatStatus(req.status), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Reason: ${req.reason}", style = MaterialTheme.typography.bodySmall)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Tap container to inspect parent details, attendance & outpass history",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Text("Leaving: ${req.dateTime} • Return: ${req.expectedReturnTime}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (staffUser?.role == "SECURITY") {
                                                            viewModel.verifyExitSecurity(req)
                                                        } else {
                                                            viewModel.actionOnOutpass(req, approve = true)
                                                        }
                                                        Toast.makeText(context, if (staffUser?.role == "SECURITY") "Gate status verified! Exit recorded successfully." else "Outpass cleared successfully!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (staffUser?.role == "SECURITY") MaterialTheme.colorScheme.primary else Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(if (staffUser?.role == "SECURITY") Icons.Default.CheckCircle else Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (staffUser?.role == "SECURITY") "Verify Exit" else "Approve", fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = {
                                                        selectedOutpassForAction = req
                                                        outpassRejectReason = ""
                                                        showRejectDialog = true
                                                    },
                                                    modifier = if (staffUser?.role == "SECURITY") Modifier.size(0.dp) else Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (staffUser?.role == "SECURITY") Color.Transparent else Color.Red),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(if (staffUser?.role == "SECURITY") 0.dp else 16.dp))
                                                    Spacer(modifier = Modifier.width(if (staffUser?.role == "SECURITY") 0.dp else 4.dp))
                                                    Text(if (staffUser?.role == "SECURITY") "" else "Reject", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Certificates" -> {
                    val pendingCerts = certificates.filter {
                        ((staffUser?.role == "CLASS_ADVISOR" || staffUser?.role == "MENTOR") && it.status == "PENDING_MENTOR") ||
                        (staffUser?.role == "HOD" && it.status == "PENDING_HOD") ||
                        (staffUser?.role == "PRINCIPAL" && it.status == "PENDING_PRINCIPAL") ||
                        (staffUser?.role == "PA" && it.status == "PENDING_PA_PRINT") ||
                        (staffUser?.role == "ADMIN" && it.status.startsWith("PENDING"))
                    }

                    var certSearchQuery by remember { mutableStateOf("") }
                    val filteredCerts = pendingCerts.filter {
                        certSearchQuery.isBlank() ||
                        it.studentName.contains(certSearchQuery, ignoreCase = true) ||
                        it.rollNumber.contains(certSearchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = certSearchQuery,
                            onValueChange = { certSearchQuery = it },
                            placeholder = { Text("Search student name/roll...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (certSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { certSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("cert_search_field")
                        )

                        if (filteredCerts.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (certSearchQuery.isEmpty()) 
                                        "No pending certificate requests found for your stage."
                                    else 
                                        "No certificate requests match \"$certSearchQuery\"",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredCerts) { cert ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(cert.studentName, fontWeight = FontWeight.Bold)
                                                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                                    val stageLabel = when (cert.status) {
                                                        "PENDING_MENTOR" -> "Awaiting Mentor"
                                                        "PENDING_HOD" -> "Awaiting HOD"
                                                        "PENDING_PRINCIPAL" -> "Awaiting Principal"
                                                        "PENDING_PA_PRINT" -> "Awaiting PA Printing"
                                                        else -> cert.status
                                                    }
                                                    Text(stageLabel, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                            Text("Roll: ${cert.rollNumber} • Dept: ${cert.department}", fontSize = 11.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Requested: ${cert.certificateType}", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                            Text("Purpose Info: ${cert.details}", fontSize = 11.sp, color = Color.DarkGray)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val actionText = when (staffUser?.role) {
                                                    "CLASS_ADVISOR", "MENTOR" -> "Forward to HOD"
                                                    "HOD" -> "Forward to Principal"
                                                    "PRINCIPAL" -> "Forward to PA to Print"
                                                    "PA" -> "Print Certificate"
                                                    else -> "Approve"
                                                }
                                                Button(
                                                    onClick = {
                                                        viewModel.actionOnCertificate(cert, approve = true)
                                                        val msg = when (staffUser?.role) {
                                                            "CLASS_ADVISOR", "MENTOR" -> "Forwarded to HOD successfully!"
                                                            "HOD" -> "Forwarded to Principal successfully!"
                                                            "PRINCIPAL" -> "Forwarded to PA for printing!"
                                                            "PA" -> "Certificate printed successfully!"
                                                            else -> "Certificate request approved!"
                                                        }
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(actionText, fontSize = 11.sp)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        viewModel.actionOnCertificate(cert, approve = false)
                                                        Toast.makeText(context, "Certificate application denied", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                                ) {
                                                    Text("Reject", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "History" -> {
                    val historyOutpasses = if (staffUser?.role == "PRINCIPAL") emptyList() else outpasses.filter { it.status != "PENDING_ADVISOR" && it.status != "PENDING" }
                    val historyCerts = certificates.filter { it.status != "PENDING_MENTOR" && it.status != "PENDING" }
                    
                    val combinedHistory = (historyOutpasses.map { "Outpass" to it } + historyCerts.map { "Certificate" to it })
                        .sortedByDescending { 
                            when (val item = it.second) {
                                is OutpassRequest -> item.timestamp
                                is CertificateRequest -> item.timestamp
                                else -> 0L
                            }
                        }

                    var historySearchQuery by remember { mutableStateOf("") }
                    val filteredHistory = combinedHistory.filter { (_, req) ->
                        val name = when (req) {
                            is OutpassRequest -> req.studentName
                            is CertificateRequest -> req.studentName
                            else -> ""
                        }
                        val roll = when (req) {
                            is OutpassRequest -> req.rollNumber
                            is CertificateRequest -> req.rollNumber
                            else -> ""
                        }
                        historySearchQuery.isBlank() ||
                        name.contains(historySearchQuery, ignoreCase = true) ||
                        roll.contains(historySearchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = historySearchQuery,
                            onValueChange = { historySearchQuery = it },
                            placeholder = { Text("Search student name/roll...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (historySearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { historySearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("history_search_field")
                        )

                        if (filteredHistory.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (historySearchQuery.isEmpty()) 
                                        "No history of student requests found."
                                    else 
                                        "No history records match \"$historySearchQuery\"",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                items(filteredHistory) { (type, req) ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = when (req) {
                                                            is OutpassRequest -> req.studentName
                                                            is CertificateRequest -> req.studentName
                                                            else -> ""
                                                        },
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = when (req) {
                                                            is OutpassRequest -> "Roll: ${req.rollNumber} • Dept: ${req.department}"
                                                            is CertificateRequest -> "Roll: ${req.rollNumber} • Dept: ${req.department}"
                                                            else -> ""
                                                        },
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                                Badge(
                                                    containerColor = when (type) {
                                                        "Outpass" -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> MaterialTheme.colorScheme.tertiaryContainer
                                                    }
                                                ) {
                                                    Text(
                                                        text = type,
                                                        fontSize = 8.sp,
                                                        color = when (type) {
                                                            "Outpass" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                                                        },
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            when (req) {
                                                is OutpassRequest -> {
                                                    Text("Reason: ${req.reason}", style = MaterialTheme.typography.bodySmall)
                                                    Text("Leaving: ${req.dateTime} • Return: ${req.expectedReturnTime}", fontSize = 11.sp)
                                                }
                                                is CertificateRequest -> {
                                                    Text("Certificate Type: ${req.certificateType}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                    Text("Purpose: ${req.details}", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val statusLabel = when (req) {
                                                    is OutpassRequest -> formatStatus(req.status)
                                                    is CertificateRequest -> when (req.status) {
                                                        "PENDING_MENTOR" -> "Awaiting Mentor"
                                                        "PENDING_HOD" -> "Awaiting HOD"
                                                        "PENDING_PRINCIPAL" -> "Awaiting Principal"
                                                        "PENDING_PA_PRINT" -> "Awaiting PA Printing"
                                                        else -> req.status
                                                    }
                                                    else -> ""
                                                }
                                                
                                                val statusColor = when {
                                                    statusLabel.contains("APPROVED") || statusLabel.contains("Approved") -> Color(0xFF10B981)
                                                    statusLabel.contains("REJECTED") || statusLabel.contains("Rejected") -> Color.Red
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                                
                                                Text(
                                                    text = "Status: $statusLabel",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp,
                                                    color = statusColor
                                                )
                                                
                                                val timestamp = when (req) {
                                                    is OutpassRequest -> req.timestamp
                                                    is CertificateRequest -> req.timestamp
                                                    else -> 0L
                                                }
                                                
                                                val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
                                                Text(
                                                    text = dateStr,
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "History Logs" -> {
                    SecurityHistoryLogsSheet(
                        outpasses = outpasses,
                        viewModel = viewModel,
                        context = context
                    )
                }

                "Message Chat" -> {
                    MessageChatPanel(
                        viewModel = viewModel,
                        staffUser = staffUser
                    )
                }

                "Stationery" -> {
                    // Prepopulate inventory stock & receive status
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Refill Supply Stock", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = statQtyInput,
                                            onValueChange = { statQtyInput = it },
                                            label = { Text("Stock Qty") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                val qty = statQtyInput.toIntOrNull() ?: 0
                                                if (qty > 0) {
                                                    viewModel.addStationeryStock(statIdInput, "Item Refill", qty, "Writing", statPriceInput.toDoubleOrNull() ?: 15.0)
                                                    statQtyInput = ""
                                                    Toast.makeText(context, "Stock updated successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.align(Alignment.CenterVertically),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Refill")
                                        }
                                    }
                                    // Dropdown of items
                                    Text("Select Stock Item ID:", fontSize = 10.sp, color = Color.Gray)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(stationerySupplyList) { item ->
                                            ElevatedFilterChip(
                                                selected = statIdInput == item.id,
                                                onClick = { statIdInput = item.id },
                                                label = { Text("${item.name} (${item.stock} left)") }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Pending Stationery Pickups", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        item {
                            OutlinedTextField(
                                value = stationerySearchQuery,
                                onValueChange = { stationerySearchQuery = it },
                                placeholder = { Text("Search pending pickups by student or item...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (stationerySearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { stationerySearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("stationery_pickup_search")
                            )
                        }

                        val pendingPickups = stationeryRequests.filter { it.status == "PENDING" || it.status == "READY_FOR_COLLECTION" }
                        val filteredPickups = pendingPickups.filter {
                            stationerySearchQuery.isBlank() ||
                            it.studentName.contains(stationerySearchQuery, ignoreCase = true) ||
                            it.itemName.contains(stationerySearchQuery, ignoreCase = true)
                        }

                        if (filteredPickups.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (stationerySearchQuery.isEmpty()) 
                                            "No active pre-order pickups on catalog."
                                        else 
                                            "No pre-order pickups match \"$stationerySearchQuery\"",
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredPickups) { req ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(req.studentName, fontWeight = FontWeight.Bold)
                                        Text("Item: ${req.itemName} • Qty: ${req.quantity}", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (req.status == "PENDING") {
                                                Button(
                                                    onClick = { viewModel.changeStationeryRequestStatus(req, "READY_FOR_COLLECTION") },
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("Mark Ready", fontSize = 10.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = { viewModel.changeStationeryRequestStatus(req, "COLLECTED") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("Mark Collected", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Print Center" -> {
                    // Document processing status actions
                    val filteredRequests = remember(printRequests, printSearchQuery, printStatusFilter) {
                        printRequests.filter { req ->
                            val matchesSearch = req.studentName.contains(printSearchQuery, ignoreCase = true) ||
                                                req.fileName.contains(printSearchQuery, ignoreCase = true)
                            val matchesStatus = if (printStatusFilter == "All") true else req.status == printStatusFilter
                            matchesSearch && matchesStatus
                        }
                    }
                    
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title / Description
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Queue Bulk Management",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            
                            // Multi select visual info badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "${selectedPrintIds.size} Selected",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        // Filters & Search section
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Search bar
                                OutlinedTextField(
                                    value = printSearchQuery,
                                    onValueChange = { printSearchQuery = it },
                                    placeholder = { Text("Search by Student Name or File Name...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        if (printSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { printSearchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                                
                                // Status selector Chips row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Filter Status:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    
                                    val printStatuses = listOf("All", "QUEUED", "PRINTING", "READY", "COMPLETED")
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(printStatuses) { status ->
                                            val isSelected = printStatusFilter == status
                                            val chipBgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                            val chipTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            val chipBorderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = chipBgColor,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, chipBorderColor),
                                                onClick = {
                                                    printStatusFilter = status
                                                    selectedPrintIds.clear() // Clear selection on tab change to avoid bulk applying wrong states
                                                },
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 10.dp)
                                                ) {
                                                    Text(status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = chipTextColor)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bulk Actions Bar (only visible when at least 1 item is selected)
                        AnimatedVisibility(
                            visible = selectedPrintIds.isNotEmpty(),
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut()
                        ) {
                            val selectedRequests = printRequests.filter { it.id in selectedPrintIds }
                            val totalSelectedPages = selectedRequests.sumOf { it.pagesCount }
                            val totalSelectedCost = selectedRequests.sumOf { it.totalCost }
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
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
                                        Column {
                                            Text(
                                                "Bulk Actions: ${selectedPrintIds.size} Selected",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                "Total Pages: $totalSelectedPages • Total: \u20b9$totalSelectedCost",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                        
                                        TextButton(
                                            onClick = { selectedPrintIds.clear() },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Clear Selection", fontSize = 11.sp)
                                        }
                                    }
                                    
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.actionOnMultiplePrintRequests(selectedRequests, "PRINTING")
                                                Toast.makeText(context, "Processing ${selectedRequests.size} print jobs: SENT TO PRINTING!", Toast.LENGTH_SHORT).show()
                                                selectedPrintIds.clear()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Set Printing", fontSize = 10.sp)
                                        }
                                        
                                        Button(
                                            onClick = {
                                                viewModel.actionOnMultiplePrintRequests(selectedRequests, "READY")
                                                Toast.makeText(context, "${selectedRequests.size} print jobs marked as READY!", Toast.LENGTH_SHORT).show()
                                                selectedPrintIds.clear()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF10B981),
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Set Ready", fontSize = 10.sp)
                                        }
                                        
                                        Button(
                                            onClick = {
                                                viewModel.actionOnMultiplePrintRequests(selectedRequests, "COMPLETED")
                                                Toast.makeText(context, "Served ${selectedRequests.size} printouts: COMPLETED!", Toast.LENGTH_SHORT).show()
                                                selectedPrintIds.clear()
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.DarkGray,
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Collect", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Select All Toggle of current filtered list
                        if (filteredRequests.isNotEmpty()) {
                            val allFilteredSelected = filteredRequests.all { it.id in selectedPrintIds }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.clickable {
                                        if (allFilteredSelected) {
                                            filteredRequests.forEach { selectedPrintIds.remove(it.id) }
                                        } else {
                                            filteredRequests.forEach {
                                                if (it.id !in selectedPrintIds) selectedPrintIds.add(it.id)
                                            }
                                        }
                                    }
                                ) {
                                    Checkbox(
                                        checked = allFilteredSelected,
                                        onCheckedChange = { checked ->
                                            if (checked == true) {
                                                filteredRequests.forEach {
                                                    if (it.id !in selectedPrintIds) selectedPrintIds.add(it.id)
                                                }
                                            } else {
                                                filteredRequests.forEach { selectedPrintIds.remove(it.id) }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text("Select All Visible (${filteredRequests.size})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                
                                Text(
                                    "Total cost of visible: \u20b9${filteredRequests.sumOf { it.totalCost }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Print requests list
                        if (filteredRequests.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No matching print requests in queue.", color = Color.Gray, fontSize = 13.sp)
                                    if (printSearchQuery.isNotEmpty() || printStatusFilter != "All") {
                                        TextButton(onClick = {
                                            printSearchQuery = ""
                                            printStatusFilter = "All"
                                        }) {
                                            Text("Reset Filters")
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredRequests, key = { it.id }) { req ->
                                    val isSelected = req.id in selectedPrintIds
                                    
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                             else MaterialTheme.colorScheme.surface
                                        ),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                                 else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isSelected) {
                                                    selectedPrintIds.remove(req.id)
                                                } else {
                                                    selectedPrintIds.add(req.id)
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Selection Checkbox
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked == true) {
                                                        if (req.id !in selectedPrintIds) selectedPrintIds.add(req.id)
                                                    } else {
                                                        selectedPrintIds.remove(req.id)
                                                    }
                                                },
                                                modifier = Modifier.testTag("print_select_checkbox_${req.id}")
                                            )
                                            
                                            Spacer(modifier = Modifier.width(10.dp))
                                            
                                            // Details
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(req.studentName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                                        Text(req.fileName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                                    }
                                                    
                                                    // Badge indicator
                                                    val badgeColor = when (req.status) {
                                                        "QUEUED" -> MaterialTheme.colorScheme.errorContainer
                                                        "PRINTING" -> MaterialTheme.colorScheme.primaryContainer
                                                        "READY" -> Color(0xFFD1FAE5) // light green
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                    val badgeTextColor = when (req.status) {
                                                        "QUEUED" -> MaterialTheme.colorScheme.onErrorContainer
                                                        "PRINTING" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        "READY" -> Color(0xFF065F46) // dark green
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                    
                                                    Surface(
                                                        color = badgeColor,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(
                                                            text = req.status,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = badgeTextColor,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(6.dp))
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "${req.pagesCount} pages • ${req.printType} • ${req.bindingType}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        "\u20B9${req.totalCost}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                // Individual control options for convenience
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (req.status == "QUEUED") {
                                                        ElevatedButton(
                                                            onClick = { viewModel.actionOnPrintRequest(req, "PRINTING") },
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("Start Printing", fontSize = 10.sp)
                                                        }
                                                    } else if (req.status == "PRINTING") {
                                                        ElevatedButton(
                                                            onClick = { viewModel.actionOnPrintRequest(req, "READY") },
                                                            colors = ButtonDefaults.elevatedButtonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("Mark Ready", fontSize = 10.sp)
                                                        }
                                                    } else if (req.status == "READY") {
                                                        ElevatedButton(
                                                            onClick = { viewModel.actionOnPrintRequest(req, "COMPLETED") },
                                                            colors = ButtonDefaults.elevatedButtonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("Mark Collected", fontSize = 10.sp)
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
                }

                "Canteen" -> {
                    // Update availability of Canteen products or serve food token
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Text("Pending Fast Food Collect Tokens", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        val activeMealBookings = canteenBookings.filter { it.status == "BOOKED" }
                        if (activeMealBookings.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("No food token collections pending.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(activeMealBookings) { booking ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(booking.studentName, fontWeight = FontWeight.Bold)
                                            Text("Token Code: ${booking.qrToken}", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(booking.itemsJson, fontSize = 12.sp, color = Color.DarkGray)
                                        }
                                        Button(
                                            onClick = { viewModel.actionOnCanteenBooking(booking, "COMPLETED") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF139C6B)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Serve Food", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Manage Menu Listing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        items(canteenMenuList) { item ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${item.category} • \u20B9${item.price}", fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (item.isAvailable) "Available" else "Sold Out", fontSize = 10.sp, modifier = Modifier.padding(end = 8.dp))
                                        Switch(
                                            checked = item.isAvailable,
                                            onCheckedChange = { isChecked ->
                                                viewModel.saveCanteenItem(item.copy(isAvailable = isChecked))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "Analytics" -> {
                    // Revenue calculator & request trackers display metrics
                    val totalPrintRev = printRequests.sumOf { it.totalCost }
                    val totalCanteenRev = canteenBookings.sumOf { it.totalCost }
                    val totalOutpassCount = if (staffUser?.role == "PRINCIPAL") 0 else outpasses.size
                    val totalApprovedOutpass = if (staffUser?.role == "PRINCIPAL") 0 else outpasses.count { it.status == "APPROVED" }
                    val certApprovedCount = certificates.count { it.status == "APPROVED" }
                    val totalPreordersCount = stationeryRequests.size

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (staffUser?.role != "PRINCIPAL") {
                            item {
                                Text("Campus Services Reports", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    StatCard(
                                        title = "Printer Revenue",
                                        value = "\u20B9$totalPrintRev",
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        title = "Canteen Desk Sale",
                                        value = "\u20B9$totalCanteenRev",
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Analytics Chart",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Application Volume Statistics",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Real-time query distribution & clearance rates",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Let's compute the slice counts dynamically
                                    val pendingCount = (if (staffUser?.role == "PRINCIPAL") 0 else outpasses.count { it.status.startsWith("PENDING_") }) + 
                                            certificates.count { it.status.startsWith("PENDING") } + 
                                            stationeryRequests.count { it.status == "PENDING" } + 
                                            printRequests.count { it.status == "QUEUED" || it.status == "PRINTING" }

                                    val acceptedCount = (if (staffUser?.role == "PRINCIPAL") 0 else outpasses.count { it.status == "APPROVED" }) + 
                                            certificates.count { it.status == "APPROVED" || it.status == "ISSUED" } + 
                                            stationeryRequests.count { it.status == "READY_FOR_COLLECTION" || it.status == "COLLECTED" } + 
                                            printRequests.count { it.status == "READY" || it.status == "COMPLETED" }

                                    val deniedCount = (if (staffUser?.role == "PRINCIPAL") 0 else outpasses.count { it.status == "REJECTED" }) + 
                                            certificates.count { it.status == "REJECTED" } + 
                                            stationeryRequests.count { it.status == "REJECTED" } + 
                                            printRequests.count { it.status == "REJECTED" }

                                    val othersCount = canteenBookings.size + canteenMenuList.size + stationerySupplyList.size

                                    // We create our PieSlices
                                    val slices = listOf(
                                        PieSlice("Pending Requests", pendingCount, Color(0xFFF59E0B)),
                                        PieSlice("Accepted Requests", acceptedCount, Color(0xFF10B981)),
                                        PieSlice("Denied Requests", deniedCount, Color(0xFFEF4444)),
                                        PieSlice("Others (Store/Menu)", othersCount, Color(0xFF6366F1))
                                    )

                                    val totalCount = slices.sumOf { it.count }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left side: Donut Chart Canvas
                                        Box(
                                            modifier = Modifier
                                                .size(130.dp)
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (totalCount == 0) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    drawArc(
                                                        color = Color(0xFFE5E7EB),
                                                        startAngle = 0f,
                                                        sweepAngle = 360f,
                                                        useCenter = false,
                                                        style = Stroke(width = 45f)
                                                    )
                                                }
                                                Text(
                                                    text = "No Data",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    var currentAngle = -90f
                                                    slices.forEach { slice ->
                                                        if (slice.count > 0) {
                                                            val sweepAngle = (slice.count.toFloat() / totalCount.toFloat()) * 360f
                                                            drawArc(
                                                                color = slice.color,
                                                                startAngle = currentAngle,
                                                                sweepAngle = sweepAngle,
                                                                useCenter = false,
                                                                style = Stroke(width = 45f)
                                                            )
                                                            currentAngle += sweepAngle
                                                        }
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = totalCount.toString(),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "TOTAL",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        // Right side: Custom Legends with stats
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            slices.forEach { slice ->
                                                val percentage = if (totalCount > 0) {
                                                    (slice.count.toFloat() / totalCount.toFloat() * 100).toInt()
                                                } else {
                                                    0
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .background(slice.color, RoundedCornerShape(100.dp))
                                                    )
                                                    Column {
                                                        Text(
                                                            text = slice.name,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "${slice.count} volume ($percentage%)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.Gray,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Interactive informational badge at bottom of the card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            val overallClearance = if (totalCount > 0) {
                                                ((acceptedCount.toFloat() / (totalCount - othersCount).coerceAtLeast(1).toFloat()) * 100).toInt().coerceIn(0, 100)
                                            } else {
                                                100
                                            }
                                            Text(
                                                text = "Overall request clearance rate: $overallClearance%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
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
    }

    // Modal Reject Dialog
    if (showRejectDialog && selectedOutpassForAction != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Outpass Request", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Outpass applied by ${selectedOutpassForAction?.studentName}")
                    OutlinedTextField(
                        value = outpassRejectReason,
                        onValueChange = { outpassRejectReason = it },
                        label = { Text("Rejection Comments") },
                        placeholder = { Text("e.g. Parental verification missing / Exams active") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedOutpassForAction?.let { out ->
                            val reason = if (outpassRejectReason.isBlank()) "Rejected by system" else outpassRejectReason
                            viewModel.actionOnOutpass(out, approve = false, comment = reason)
                            Toast.makeText(context, "Outpass rejected.", Toast.LENGTH_SHORT).show()
                        }
                        showRejectDialog = false
                        selectedOutpassForAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Reject Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Outpass Deep Inquiry Details Dialog for Class Advisor & Admin
    if (selectedOutpassForDetails != null) {
        val req = selectedOutpassForDetails!!
        val studentUser = allUsers.find { it.userId.equals(req.studentId, ignoreCase = true) || it.rollNumber.equals(req.rollNumber, ignoreCase = true) }
        
        // Find all outpasses for this student to display "outpass track"
        val studentPassedOutpasses = outpasses.filter { it.rollNumber.equals(req.rollNumber, ignoreCase = true) }
        
        // Derive customized Parent Name based on student last name/family background for realism
        val parentName = remember(req.studentName) {
            val studentNames = req.studentName.split(" ")
            val lastName = if (studentNames.size > 1) studentNames.last() else "Reddy"
            if (lastName.equals("Reddy", ignoreCase = true)) "Mr. Ramakrishna Reddy"
            else "Mr. S. ${lastName}"
        }
        
        // Derive Attendance percentage and metrics uniquely from roll number for realism
        val attendancePercent = remember(req.rollNumber) {
            val charSum = req.rollNumber.sumOf { it.code }
            val rng = (charSum % 15) + 80 // Ensures a realistic score between 80% and 94%
            rng
        }
        
        AlertDialog(
            onDismissRequest = { selectedOutpassForDetails = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = "Student Details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Student Outpass Deep Inquiry",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Part 1: Student Class Information Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "STUDENT CLASS INFORMATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Name: ${req.studentName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Roll Code: ${req.rollNumber}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            Text(text = "Department: ${req.department}", fontSize = 12.sp)
                            Text(text = "Class Section: Year III - Section B (Batch A)", fontSize = 11.sp, color = Color.Gray)
                            Text(text = "Official Mail: ${studentUser?.email ?: "${req.rollNumber.lowercase()}@college.edu"}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    // Part 2: Parent Name & Guardian Contact Phone
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "VERIFIED PARENTAL & CONTACT DETAILS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Father/Guardian: $parentName", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "Contact Phone: ${req.parentContact}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(100.dp))
                                        .clickable {
                                            Toast.makeText(context, "Initiating parental voice verification call to ${req.parentContact}...", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Parent",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Part 3: Attendance Track
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = "STUDENT ATTENDANCE TRACK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val isEligible = attendancePercent >= 75
                                    Icon(
                                        imageVector = if (isEligible) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isEligible) Color(0xFF10B981) else Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "$attendancePercent% Cumulative Attendance",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isEligible) Color(0xFF10B981) else Color.Red
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (attendancePercent >= 75) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (attendancePercent >= 75) "ELIGIBLE" else "SHORTAGE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (attendancePercent >= 75) Color(0xFF10B981) else Color.Red,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = attendancePercent / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = if (attendancePercent >= 75) Color(0xFF10B981) else Color.Red,
                                trackColor = Color.LightGray.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Lectures: ${ (attendancePercent * 150) / 100 } / 150 total sessions. Minimum exit gate threshold is 75%.",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Part 4: Outpass Track (Audit History)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "OUTPASS REQUEST HISTORY & TRACK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            
                            val historicCount = studentPassedOutpasses.size
                            Text(
                                text = "Total Outpasses Applied: $historicCount (${studentPassedOutpasses.count { it.status == "APPROVED" }} Approved)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            if (studentPassedOutpasses.isEmpty()) {
                                Text(text = "No prior outpass history records found.", fontSize = 11.sp, color = Color.Gray)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    studentPassedOutpasses.take(4).forEach { past ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = past.qrText, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                                Text(text = "Reason: ${past.reason}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                                Text(text = "Date: ${past.dateTime}", fontSize = 9.sp, color = Color.Gray)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (past.status) {
                                                    "APPROVED" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                    "REJECTED" -> Color.Red.copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                }
                                            ) {
                                                Text(
                                                    text = formatStatus(past.status),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (past.status) {
                                                        "APPROVED" -> Color(0xFF10B981)
                                                        "REJECTED" -> Color.Red
                                                        else -> MaterialTheme.colorScheme.secondary
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }
                    
                    // Display details field of the current active outpass
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Yellow.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "Active Outpass Request Specifics:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Leaving Time: ${req.dateTime}", fontSize = 11.sp)
                        Text(text = "Expected Return: ${req.expectedReturnTime}", fontSize = 11.sp)
                        Text(text = "Reason Given: ${req.reason}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.actionOnOutpass(req, approve = true)
                            Toast.makeText(context, "Outpass approved!", Toast.LENGTH_SHORT).show()
                            selectedOutpassForDetails = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            selectedOutpassForAction = req
                            outpassRejectReason = ""
                            showRejectDialog = true
                            selectedOutpassForDetails = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOutpassForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun StatMetricsRow(label: String, valStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(valStr, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

data class PieSlice(
    val name: String,
    val count: Int,
    val color: Color
)

@Composable
fun SecurityHistoryLogsSheet(
    outpasses: List<OutpassRequest>,
    viewModel: PortalViewModel,
    context: android.content.Context
) {
    val exitedOutpasses = outpasses.filter { it.status == "EXITED" }
    val selectedIds = remember { mutableStateListOf<Int>() }
    var selectedExportOfficial by remember { mutableStateOf("HOD") }
    var showSendSuccessDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp)
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
                    Column {
                        Text(
                            text = "Verified Exit Logs",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${exitedOutpasses.size} recorded student exits at gate",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                selectedIds.clear()
                                selectedIds.addAll(exitedOutpasses.map { it.id })
                            }
                        ) {
                            Text("Select All", fontSize = 10.sp)
                        }
                        TextButton(
                            onClick = { selectedIds.clear() }
                        ) {
                            Text("Clear", fontSize = 10.sp)
                        }
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recipient Official: ",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(0.4f)
                    )
                    
                    Row(
                        modifier = Modifier.weight(0.6f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("HOD", "PRINCIPAL").forEach { role ->
                            FilterChip(
                                selected = selectedExportOfficial == role,
                                onClick = { selectedExportOfficial = role },
                                label = { Text(role, fontSize = 10.sp) }
                            )
                        }
                    }
                }
                
                Button(
                    onClick = {
                        if (selectedIds.isEmpty()) {
                            Toast.makeText(context, "Please select at least one exit record to send", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val selectedRequests = exitedOutpasses.filter { it.id in selectedIds }
                        val dateFormat = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                        
                        val attachmentDataStr = selectedRequests.joinToString("\n") { req ->
                            val exitTime = dateFormat.format(java.util.Date(req.timestamp))
                            "${req.studentName}::${req.rollNumber}::${req.department}::${exitTime}"
                        }
                        
                        viewModel.sendChatMessage(
                            recipientRole = selectedExportOfficial,
                            messageText = "Attached: Gate Exit Pass Log Sheet [${selectedRequests.size} records]",
                            isSheet = true,
                            sheetData = attachmentDataStr
                        )
                        showSendSuccessDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = exitedOutpasses.isNotEmpty()
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Extract & Send Selected Sheet via Chat", fontSize = 11.sp)
                }
            }
        }
        
        if (showSendSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSendSuccessDialog = false },
                title = { Text("Sheet Dispatched!") },
                text = { Text("Secured Gate Exit Log Sheet containing ${selectedIds.size} records has been successfully generated and sent to the $selectedExportOfficial via Message Chat.") },
                confirmButton = {
                    Button(onClick = { showSendSuccessDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        if (exitedOutpasses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No verified exits recorded yet.\nClear pending approved outpasses at gate to populate exits.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            Text(
                text = "Sheet Records Table",
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(36.dp))
                        Text("Student Name / Roll", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1.2f))
                        Text("Dept", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(0.5f))
                        Text("Exit Verified Time", fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.weight(1f))
                    }
                }
                
                items(exitedOutpasses) { req ->
                    val isSelected = req.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent)
                            .clickable {
                                if (isSelected) selectedIds.remove(req.id) else selectedIds.add(req.id)
                            }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked == true) selectedIds.add(req.id) else selectedIds.remove(req.id)
                            },
                            modifier = Modifier.width(36.dp)
                        )
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(req.studentName, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text(req.rollNumber, fontSize = 9.sp, color = Color.Gray)
                        }
                        Text(req.department, fontSize = 10.sp, modifier = Modifier.weight(0.5f))
                        
                        val timeStr = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(req.timestamp))
                        Text(timeStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
fun MessageChatPanel(
    viewModel: PortalViewModel,
    staffUser: User?
) {
    val currentRole = staffUser?.role ?: "SECURITY"
    val chatMessages by viewModel.chatMessages.collectAsState()
    
    // Message input State
    var textMessageInput by remember { mutableStateOf("") }
    
    // Choose whom to send to
    val possibleRecipients = when (currentRole) {
        "SECURITY" -> listOf("HOD", "PRINCIPAL")
        "HOD" -> listOf("SECURITY", "PRINCIPAL")
        "PRINCIPAL" -> listOf("SECURITY", "HOD")
        else -> listOf("SECURITY", "HOD", "PRINCIPAL")
    }
    var selectedRecipient by remember { mutableStateOf(possibleRecipients.firstOrNull() ?: "HOD") }

    // Filter relevant messages only
    val filteredMessages = chatMessages.filter {
        (it.senderRole == currentRole && it.recipientRole == selectedRecipient) ||
        (it.senderRole == selectedRecipient && it.recipientRole == currentRole)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chat Partner Filter Selector Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Conversation: ", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    possibleRecipients.forEach { rec ->
                        FilterChip(
                            selected = selectedRecipient == rec,
                            onClick = { selectedRecipient = rec },
                            label = { Text(rec, fontSize = 9.sp) }
                        )
                    }
                }
            }
        }

        // Messages Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            if (filteredMessages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No messages with $selectedRecipient yet. Send a query or share a gate sheet above!",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMessages) { msg ->
                        val isMe = msg.senderRole == currentRole
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            Text(
                                text = "${msg.senderName} (${msg.senderRole})",
                                fontSize = 8.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            
                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 10.dp,
                                    topEnd = 10.dp,
                                    bottomStart = if (isMe) 10.dp else 0.dp,
                                    bottomEnd = if (isMe) 0.dp else 10.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = msg.messageText,
                                        fontSize = 11.sp,
                                        color = if (isMe) Color.White else Color.Black
                                    )
                                    
                                    if (msg.isSheetAttachment) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Card(
                                            shape = RoundedCornerShape(6.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        ) {
                                            Column {
                                                Row(
                                                    modifier = Modifier
                                                        .background(Color(0xFF10B981))
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "EXTRACTED EXITS TABLE SHEET",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 8.sp
                                                    )
                                                }
                                                
                                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                                
                                                val rows = msg.attachmentData?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
                                                rows.forEachIndexed { idx, row ->
                                                    val cols = row.split("::")
                                                    if (cols.size >= 4) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(if (idx % 2 == 0) Color.Transparent else Color.White.copy(alpha = 0.3f))
                                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Column(modifier = Modifier.weight(1.2f)) {
                                                                Text(cols[0], fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                                Text("Roll: ${cols[1]}", fontSize = 8.sp, color = Color.Gray)
                                                            }
                                                            Text(cols[2], fontSize = 8.sp, modifier = Modifier.weight(0.5f))
                                                            Text(cols[3], fontSize = 8.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                                        }
                                                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp))
                                    Text(
                                        text = timeStr,
                                        fontSize = 7.sp,
                                        color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textMessageInput,
                onValueChange = { textMessageInput = it },
                placeholder = { Text("Type official chat message...", fontSize = 11.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
            
            Button(
                onClick = {
                    if (textMessageInput.trim().isNotBlank()) {
                        viewModel.sendChatMessage(
                            recipientRole = selectedRecipient,
                            messageText = textMessageInput.trim()
                        )
                        textMessageInput = ""
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Send", fontSize = 11.sp)
            }
        }
    }
}

private fun getDeptFromRollNumber(roll: String): String? {
    val clean = roll.uppercase().trim()
    return when {
        clean.contains("CSE") || clean.contains("CS") -> "Computer Science"
        clean.contains("ECE") || clean.contains("EC") -> "Electronics & Communication"
        clean.contains("EEE") || clean.contains("EE") -> "Electrical & Electronics"
        clean.contains("CIVIL") || clean.contains("CIV") || clean.contains("CE") -> "Civil Engineering"
        clean.contains("MECH") || clean.contains("MEC") || clean.contains("ME") -> "Mechanical Engineering"
        clean.contains("IT") -> "Information Technology"
        else -> null
    }
}

private fun getFileNameHelper(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "selected_file.csv"
}

private fun parseCsvLineHelper(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (c == '\"') {
            inQuotes = !inQuotes
        } else if (c == ',' && !inQuotes) {
            result.add(current.toString())
            current.setLength(0)
        } else {
            current.append(c)
        }
        i++
    }
    result.add(current.toString())
    return result.map { it.trim().removeSurrounding("\"") }
}
