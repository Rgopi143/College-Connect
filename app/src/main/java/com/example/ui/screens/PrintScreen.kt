package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PrintRequest
import com.example.ui.viewmodel.PortalViewModel

@Composable
fun PrintScreen(
    viewModel: PortalViewModel,
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isStoreStaff = currentUser?.role == "STORE" || currentUser?.email?.endsWith("@necstationary.com") == true

    if (isStoreStaff) {
        StationeryPrintingStaffHub(viewModel = viewModel, modifier = modifier)
    } else {
        StudentPrintView(viewModel = viewModel, onNavigate = onNavigate, modifier = modifier)
    }
}

@Composable
fun StudentPrintView(
    viewModel: PortalViewModel,
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val studentPrintRequests by viewModel.studentPrintRequests.collectAsState()

    var showForm by remember { mutableStateOf(false) }

    // Recently submitted request representation for preview
    var recentlySubmittedRequest by remember { mutableStateOf<PrintRequest?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewCurrentPage by remember { mutableStateOf(1) }
    var showPreviewMargins by remember { mutableStateOf(true) }

    // Form states
    var fileNameInput by remember { mutableStateOf("") }
    var pagesCountInput by remember { mutableStateOf("15") }
    var printTypeSelected by remember { mutableStateOf("Black & White") } // or Color
    var copyTypeSelected by remember { mutableStateOf("Normal Print") } // or Xerox, Project Report
    var bindingTypeSelected by remember { mutableStateOf("None") } // or Spiral Binding

    // Dynamic cost computing
    val pages = pagesCountInput.toIntOrNull() ?: 0
    val costPerPage = if (printTypeSelected == "Color") 10.0 else 2.0
    val bindingCost = if (bindingTypeSelected == "Spiral Binding") 40.0 else 0.0
    val totalCost = (pages * costPerPage) + bindingCost

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Printout & Resource Center",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Upload college assignments and project files for printout",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val isFirebaseActive by viewModel.isFirebaseConnected.collectAsState()
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isFirebaseActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                        .clickable { onNavigate("FIREBASE_HUB") }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Firebase Connection Status",
                        tint = if (isFirebaseActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isFirebaseActive) "Firebase DB Connected" else "Configure Firebase Cloud",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isFirebaseActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isFirebaseActive) Color(0xFF4CAF50) else Color(0xFFFF9800))
                    )
                }
            }

            if (!showForm && selectedRequest() == null && recentlySubmittedRequest == null) {
                Button(
                    onClick = { showForm = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Upload PDF", fontSize = 12.sp)
                }
            } else {
                IconButton(onClick = { 
                    showForm = false 
                    recentlySubmittedRequest = null
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                }
            }
        }

        HorizontalDivider()

        if (showForm) {
            // Document upload & printing configurations form
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Print Configurations",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Simulated Picker trigger
                    Button(
                        onClick = {
                            fileNameInput = "System_Architecture_Report_Draft_${(10..99).random()}.pdf"
                            Toast.makeText(context, "Draft PDF loaded from local storage!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (fileNameInput.isEmpty()) "Select Document from Files" else "Change File")
                    }

                    if (fileNameInput.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = fileNameInput,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pagesCountInput,
                        onValueChange = { pagesCountInput = it },
                        label = { Text("Total Pages in Document") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Select Color Mode
                    Text("Color Mode:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CardOption(
                            text = "Black & White (₹2.0/pg)",
                            selected = printTypeSelected == "Black & White",
                            modifier = Modifier.weight(1f),
                            onClick = { printTypeSelected = "Black & White" }
                        )
                        CardOption(
                            text = "Full Color (₹10.0/pg)",
                            selected = printTypeSelected == "Color",
                            modifier = Modifier.weight(1f),
                            onClick = { printTypeSelected = "Color" }
                        )
                    }

                    // Select Copy & Binding Type
                    Text("Document Processing Specs:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CardOption(
                            text = "No Binding",
                            selected = bindingTypeSelected == "None",
                            modifier = Modifier.weight(1f),
                            onClick = { bindingTypeSelected = "None" }
                        )
                        CardOption(
                            text = "Spiral Binding (+₹40.0)",
                            selected = bindingTypeSelected == "Spiral Binding",
                            modifier = Modifier.weight(1f),
                            onClick = { bindingTypeSelected = "Spiral Binding" }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dynamic Price Estimation:", fontWeight = FontWeight.Bold)
                        Text(
                            text = "\u20B9$totalCost",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            if (fileNameInput.isEmpty()) {
                                Toast.makeText(context, "Please upload or select a document file first!", Toast.LENGTH_SHORT).show()
                            } else if (pages <= 0) {
                                Toast.makeText(context, "Invalid page count", Toast.LENGTH_SHORT).show()
                            } else {
                                val mockReq = PrintRequest(
                                    id = (1000..9999).random(),
                                    studentId = viewModel.currentUser.value?.userId ?: "",
                                    studentName = viewModel.currentUser.value?.name ?: "",
                                    fileName = fileNameInput,
                                    pagesCount = pages,
                                    printType = printTypeSelected,
                                    copyType = copyTypeSelected,
                                    bindingType = bindingTypeSelected,
                                    totalCost = totalCost,
                                    status = "QUEUED"
                                )
                                viewModel.submitPrintRequest(
                                    fileName = fileNameInput,
                                    pages = pages,
                                    printType = printTypeSelected,
                                    copyType = copyTypeSelected,
                                    bindingType = bindingTypeSelected
                                )
                                Toast.makeText(context, "Print Job successfully queued for processing!", Toast.LENGTH_LONG).show()
                                recentlySubmittedRequest = mockReq
                                showForm = false
                                fileNameInput = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Order & Send to Printer")
                    }
                }
            }
        } else if (recentlySubmittedRequest != null) {
            val req = recentlySubmittedRequest!!
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_success_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Print Order Sent to Queue!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your document is securely buffered and ready for printing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Receipt & Configuration Status",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider()
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Document Name:", fontSize = 11.sp, color = Color.Gray)
                                Text(req.fileName, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Pages:", fontSize = 11.sp, color = Color.Gray)
                                Text("${req.pagesCount} pages", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Mode Selected:", fontSize = 11.sp, color = Color.Gray)
                                Text(req.printType, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Binding Type:", fontSize = 11.sp, color = Color.Gray)
                                Text(req.bindingType, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Price:", fontSize = 11.sp, color = Color.Gray)
                                Text("\u20B9${req.totalCost}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showPreviewDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("preview_document_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview Printout", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { recentlySubmittedRequest = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("See History List", fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            // Queue lists
            Text(
                text = "My Print Queue History",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            if (studentPrintRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PrintDisabled,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No active print requests found.",
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
                    items(studentPrintRequests) { req ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = req.fileName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1
                                        )
                                    }
                                    Badge(
                                        containerColor = when (req.status) {
                                            "QUEUED" -> MaterialTheme.colorScheme.secondary
                                            "PRINTING" -> Color(0xFF3B82F6)
                                            "READY" -> Color(0xFF10B981)
                                            "COMPLETED" -> Color.Gray
                                            else -> Color.DarkGray
                                        }
                                    ) {
                                        Text(req.status, color = Color.White, fontSize = 9.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Config: ${req.pagesCount} pgs • ${req.printType}", fontSize = 11.sp, color = Color.Gray)
                                        Text("Processing: ${req.bindingType}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text(
                                        text = "\u20B9${req.totalCost}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                if (req.status == "READY") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFFE0F2FE),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Celebration,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Printed successfully! Ready at Resource Center Counter 2.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF0284C7),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            recentlySubmittedRequest = req
                                            showPreviewDialog = true
                                        },
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Preview Document", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showPreviewDialog && recentlySubmittedRequest != null) {
            val req = recentlySubmittedRequest!!
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showPreviewDialog = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = req.fileName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        modifier = Modifier.width(180.dp)
                                    )
                                    Text(
                                        text = "Print View - Buffered",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            IconButton(onClick = { showPreviewDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close preview")
                            }
                        }

                        HorizontalDivider()

                        // Simulated interactive canvas paper representation
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Paper Sheet
                            Card(
                                shape = RoundedCornerShape(4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .aspectRatio(1f / 1.414f) // standard A4 aspect ratio
                                    .padding(4.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    // Binding rendering
                                    if (req.bindingType == "Spiral Binding") {
                                        Column(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .fillMaxHeight()
                                                .background(Color(0xFFE2E8F0)),
                                            verticalArrangement = Arrangement.SpaceEvenly,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            repeat(12) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.DarkGray)
                                                )
                                            }
                                        }
                                    }

                                    // Paper text content
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(if (showPreviewMargins) 16.dp else 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "File: ${req.fileName}",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (req.printType == "Color") MaterialTheme.colorScheme.primary else Color.Black,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "Page $previewCurrentPage of ${req.pagesCount}",
                                                fontSize = 8.sp,
                                                color = Color.LightGray
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Placeholder lines mimicking assignment/project text
                                        Text(
                                            text = if (previewCurrentPage == 1) {
                                                "PROJECT REPORT COVER PAGE\n\nSubmitted in partial fulfillment of university design requirements..."
                                            } else {
                                                "CHAPTER $previewCurrentPage: EXPERIMENTAL RESULTS\n\nThe architectural model features multi-level caching systems..."
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (req.printType == "Color") MaterialTheme.colorScheme.secondary else Color.DarkGray
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            repeat(6) { i ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(if (i == 5) 0.6f else 1f)
                                                        .height(6.dp)
                                                        .background(
                                                            if (req.printType == "Color") {
                                                                listOf(Color(0xFF60A5FA), Color(0xFF34D399), Color(0xFFFBBF24))[i % 3].copy(alpha = 0.5f)
                                                            } else {
                                                                Color.LightGray.copy(alpha = 0.6f)
                                                            }
                                                        )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Watermark
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "CAMPUS CONNECT PREVIEW",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.LightGray.copy(alpha = 0.3f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom navigation and options panel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalIconButton(
                                    onClick = { if (previewCurrentPage > 1) previewCurrentPage-- },
                                    enabled = previewCurrentPage > 1,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous page", modifier = Modifier.size(16.dp))
                                }
                                FilledTonalIconButton(
                                    onClick = { if (previewCurrentPage < req.pagesCount) previewCurrentPage++ },
                                    enabled = previewCurrentPage < req.pagesCount,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next page", modifier = Modifier.size(16.dp))
                                }
                            }

                            Text(
                                text = "Page $previewCurrentPage of ${req.pagesCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Margins", fontSize = 10.sp, color = Color.Gray)
                                Switch(
                                    checked = showPreviewMargins,
                                    onCheckedChange = { showPreviewMargins = it },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showPreviewDialog = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Done Previewing")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardOption(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() }
            .background(Color.Transparent),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun selectedRequest(): PrintRequest? {
    return null
}
