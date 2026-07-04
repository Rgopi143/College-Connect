package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.model.OutpassRequest
import com.example.ui.viewmodel.PortalViewModel

@Composable
fun OutpassScreen(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val studentOutpasses by viewModel.studentOutpasses.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    var showForm by remember { mutableStateOf(false) }

    var showSuccessBanner by remember { mutableStateOf(false) }
    var successBannerMsg by remember { mutableStateOf("") }
    var successBannerDetails by remember { mutableStateOf("") }

    // Form inputs
    var dateTimeInput by remember { mutableStateOf("Today, 4:00 PM") }
    var expectedReturnInput by remember { mutableStateOf("Today, 8:30 PM") }
    var reasonInput by remember { mutableStateOf("Family emergency/Weekend visit") }
    var parentContactInput by remember { mutableStateOf(user?.parentContact ?: "+91 9885123456") }

    var selectedRequest by remember { mutableStateOf<OutpassRequest?>(null) }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(
                    if (showForm || selectedRequest != null) {
                        Modifier.verticalScroll(scrollState)
                    } else {
                        Modifier
                    }
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Module Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Outpass Clearance",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Request campus exit permission",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!showForm && selectedRequest == null) {
                Button(
                    onClick = { showForm = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Request", fontSize = 12.sp)
                }
            } else {
                IconButton(onClick = {
                    showForm = false
                    selectedRequest = null
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                }
            }
        }

        HorizontalDivider()

        if (showForm) {
            OutpassRequestForm(
                studentName = user?.name ?: "N/A",
                studentDept = user?.department ?: "N/A",
                studentRoll = user?.rollNumber ?: "N/A",
                parentContact = user?.parentContact ?: "+91 9885123456",
                onSubmit = { departure, reason, returnTime ->
                    if (departure.isBlank() || reason.isBlank()) {
                        Toast.makeText(context, "Please fill out all details", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.submitOutpass(
                            dateTime = departure,
                            reason = reason,
                            expectedReturnTime = returnTime
                        )
                        Toast.makeText(context, "Outpass request successfully submitted & Synced to Firestore live!", Toast.LENGTH_LONG).show()
                        successBannerMsg = "Outpass Request Submitted!"
                        successBannerDetails = "Requested departure: $departure. Expect return: $returnTime."
                        showSuccessBanner = true
                        showForm = false
                    }
                }
            )
        } else if (selectedRequest != null) {
            // Selected outpass details with QR-style code & download PDF Simulation
            val req = selectedRequest!!
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Outpass Details & Ticket",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pass Code", fontSize = 10.sp, color = Color.Gray)
                            Text(req.qrText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Worfklow Status", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                text = formatStatus(req.status),
                                color = if (req.status == "APPROVED") Color(0xFF10B981) else if (req.status == "REJECTED") Color.Red else MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    HorizontalDivider()

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailRow("Date & Time Out:", req.dateTime)
                        DetailRow("Expected Return:", req.expectedReturnTime)
                        DetailRow("Primary Reason:", req.reason)
                        DetailRow("Parent Contact:", req.parentContact)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (req.status == "APPROVED") {
                        OutpassCountdownTimer(
                            timestamp = req.timestamp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Drawing a beautiful barcode/QR visual representive block
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "OFFICIAL DIGITAL GATE PASS",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Let's create a visual Simulated Barcode with Canvas
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(Color.White)
                            ) {
                                val widthMultiplier = size.width / 40
                                for (i in 0..40) {
                                    val barWidth = if (i % 3 == 0) 6f else if (i % 5 == 0) 12f else 3f
                                    val startX = i * widthMultiplier
                                    if (i % 2 == 0) {
                                        drawLine(
                                            color = Color.Black,
                                            start = Offset(startX, 0f),
                                            end = Offset(startX, size.height),
                                            strokeWidth = barWidth
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "*{req.qrText}*",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Text("Class Advisor: OK", fontSize = 9.sp, color = Color.DarkGray)
                                Text("HOD: OK", fontSize = 9.sp, color = Color.DarkGray)
                            }
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Downloading Outpass PDF to /Downloads/CampusConnect-${req.qrText}.pdf", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Approved PDF")
                        }
                    } else {
                        // Workflow progress timeline visualization
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Workflow Timeline Progress", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            
                            TimelineStep("Step 1: Class Advisor Clearance", 
                                approved = req.status != "PENDING_ADVISOR" && req.status != "REJECTED",
                                pending = req.status == "PENDING_ADVISOR"
                            )
                            TimelineStep("Step 2: Department HOD Clearance", 
                                approved = req.status == "APPROVED",
                                pending = req.status == "PENDING_HOD"
                            )
                        }
                    }
                }
            }
        } else {
            // Outpasses lists
            Text(
                text = "My Outpasses History",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            if (studentOutpasses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No outpasses applied yet.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(studentOutpasses) { outpass ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRequest = outpass },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (outpass.status == "APPROVED") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = outpass.qrText,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = formatStatus(outpass.status),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when (outpass.status) {
                                            "APPROVED" -> Color(0xFF10B981)
                                            "REJECTED" -> Color.Red
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Date Out: ${outpass.dateTime}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Reason: ${outpass.reason}",
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        if (!showForm && selectedRequest == null) {
            ExtendedFloatingActionButton(
                onClick = { showForm = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Submit Outpass", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("submit_outpass_fab")
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
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontWeight = FontWeight.Medium, fontSize = 12.sp, textAlign = TextAlign.End)
    }
}

@Composable
fun TimelineStep(name: String, approved: Boolean, pending: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (approved) Icons.Default.CheckCircle else if (pending) Icons.Default.Circle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (approved) Color(0xFF10B981) else if (pending) MaterialTheme.colorScheme.secondary else Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = if (pending) FontWeight.Bold else FontWeight.Normal,
            color = if (approved) MaterialTheme.colorScheme.onSurface else if (pending) MaterialTheme.colorScheme.secondary else Color.Gray
        )
    }
}

@Composable
fun OutpassRequestForm(
    studentName: String,
    studentDept: String,
    studentRoll: String,
    parentContact: String,
    onSubmit: (dateTime: String, reason: String, expectedReturnTime: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var dateTimeInput by remember { mutableStateOf("Today, 4:00 PM") }
    var expectedReturnInput by remember { mutableStateOf("Today, 8:30 PM") }
    var reasonInput by remember { mutableStateOf("") }
    var parentContactInput by remember { mutableStateOf(parentContact) }

    val presetReasons = listOf(
        "Medical Check-up",
        "Weekend Parental Visit",
        "Festival Celebrations",
        "Emergency Work at Home",
        "Other Personal/Project Work"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Outpass Requisition Form",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Read-only user bio info card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Student Name", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Dept & Roll Number", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("$studentDept ($studentRoll)", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Departure Date & Time Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Departure Date & Time",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = dateTimeInput,
                    onValueChange = { dateTimeInput = it },
                    placeholder = { Text("e.g. 18-06-2026, 04:30 PM") },
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Today, 4:00 PM", "Tomorrow, 8:00 AM", "Friday, 2:00 PM").forEach { prepopulate ->
                        Surface(
                            onClick = { dateTimeInput = prepopulate },
                            shape = RoundedCornerShape(8.dp),
                            color = if (dateTimeInput == prepopulate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = prepopulate,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (dateTimeInput == prepopulate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Return Date & Time Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Expected Return Date & Time",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = expectedReturnInput,
                    onValueChange = { expectedReturnInput = it },
                    placeholder = { Text("e.g. 21-06-2026, 08:30 PM") },
                    leadingIcon = {
                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Today, 10:00 PM", "Tomorrow, 8:30 PM", "Sunday, 6:00 PM").forEach { prepopulate ->
                        Surface(
                            onClick = { expectedReturnInput = prepopulate },
                            shape = RoundedCornerShape(8.dp),
                            color = if (expectedReturnInput == prepopulate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = prepopulate,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (expectedReturnInput == prepopulate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Reason field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Reason for Outpass",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    placeholder = { Text("Enter a detailed reason for security gate validation...") },
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Text(text = "Quick Select Reason:", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetReasons) { preset ->
                        Surface(
                            onClick = { reasonInput = preset },
                            shape = RoundedCornerShape(20.dp),
                            color = if (reasonInput == preset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = if (reasonInput == preset) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (reasonInput == preset) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Parent Contact Field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Parent/Guardian Contact",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = parentContactInput,
                    onValueChange = { parentContactInput = it },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Submissions Indicator Showing Firestore Live Sync
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Connected: Auto-syncing to Firestore Cloud Hub on submit.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Button(
                onClick = {
                    onSubmit(dateTimeInput, reasonInput, expectedReturnInput)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Submit & Sync to active Firestore")
            }
        }
    }
}
