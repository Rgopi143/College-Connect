package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PrintRequest
import com.example.data.model.StationeryItem
import com.example.data.model.StationeryRequest
import com.example.ui.viewmodel.PortalViewModel

@Composable
fun StationeryPrintingStaffHub(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allStationeryRequests by viewModel.allStationeryRequests.collectAsState()
    val stationeryItems by viewModel.stationeryItems.collectAsState()
    val allPrintRequests by viewModel.allPrintRequests.collectAsState()

    var activeTabState by remember { mutableIntStateOf(0) } // 0: Stationery Orders, 1: Inventory Management, 2: Print Jobs

    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var stationeryFilterStatus by remember { mutableStateOf("ALL") }
    var printFilterStatus by remember { mutableStateOf("ALL") }

    // Dialog state for adding/updating stock
    var showAddStockDialog by remember { mutableStateOf(false) }
    var newStockId by remember { mutableStateOf("") }
    var newStockName by remember { mutableStateOf("") }
    var newStockCategory by remember { mutableStateOf("Writing") }
    var newStockAmount by remember { mutableStateOf("") }
    var newStockPrice by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Staff Header Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Store & Print Admin Hub",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Fulfill academic orders, manage stock & track queues",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        // Tabs Row
        TabRow(
            selectedTabIndex = activeTabState,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = activeTabState == 0, onClick = { activeTabState = 0 }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stationery Orders", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeTabState == 1, onClick = { activeTabState = 1 }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inventory Catalog", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeTabState == 2, onClick = { activeTabState = 2 }) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print Jobs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Shared Search Panel
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, item or ID...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().testTag("store_hub_search_bar"),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            shape = RoundedCornerShape(12.dp)
        )

        when (activeTabState) {
            0 -> {
                // STATIONERY ORDERS TAB
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "PENDING", "READY_FOR_COLLECTION", "COLLECTED").forEach { status ->
                        val displayLabel = when (status) {
                            "ALL" -> "All Orders"
                            "PENDING" -> "Pending"
                            "READY_FOR_COLLECTION" -> "Ready to Pick"
                            else -> "Collected"
                        }
                        FilterChip(
                            selected = stationeryFilterStatus == status,
                            onClick = { stationeryFilterStatus = status },
                            label = { Text(displayLabel, fontSize = 10.sp) },
                            modifier = Modifier.testTag("stationery_filter_$status")
                        )
                    }
                }

                val filteredStationeryRequests = allStationeryRequests.filter { req ->
                    val matchesStatus = stationeryFilterStatus == "ALL" || req.status == stationeryFilterStatus
                    val matchesSearch = searchQuery.isBlank() ||
                            req.itemName.contains(searchQuery, ignoreCase = true) ||
                            req.studentName.contains(searchQuery, ignoreCase = true) ||
                            req.studentId.contains(searchQuery, ignoreCase = true)
                    matchesStatus && matchesSearch
                }.sortedByDescending { it.timestamp }

                if (filteredStationeryRequests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No matching stationery orders found.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredStationeryRequests) { req ->
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("stationery_request_card_${req.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = req.itemName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Student: ${req.studentName} (${req.studentId})",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Badge(
                                            containerColor = when (req.status) {
                                                "PENDING" -> MaterialTheme.colorScheme.secondary
                                                "READY_FOR_COLLECTION" -> Color(0xFFF59E0B)
                                                "COLLECTED" -> Color.Gray
                                                else -> Color.DarkGray
                                            }
                                        ) {
                                            Text(
                                                text = when (req.status) {
                                                    "PENDING" -> "Awaiting Prep"
                                                    "READY_FOR_COLLECTION" -> "Ready to Collect"
                                                    else -> "Handed Over"
                                                },
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Quantity:", fontSize = 10.sp, color = Color.Gray)
                                            Text("${req.quantity} units", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(
                                            text = "\u20B9${req.totalCost}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    if (req.status != "COLLECTED") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (req.status == "PENDING") {
                                                Button(
                                                    onClick = {
                                                        viewModel.changeStationeryRequestStatus(req, "READY_FOR_COLLECTION")
                                                        Toast.makeText(context, "Marked order ready! Student notified.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.weight(1f).testTag("ready_stationery_btn_${req.id}"),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                                                ) {
                                                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Mark Ready", fontSize = 11.sp, color = Color.White)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.changeStationeryRequestStatus(req, "COLLECTED")
                                                    Toast.makeText(context, "Order completed & handed over!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f).testTag("complete_stationery_btn_${req.id}"),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Handover", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // INVENTORY MANAGEMENT TAB
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Store Inventory Catalog (${stationeryItems.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = { showAddStockDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("hub_add_item_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", fontSize = 12.sp)
                    }
                }

                val filteredCatalog = stationeryItems.filter { item ->
                    searchQuery.isBlank() ||
                            item.name.contains(searchQuery, ignoreCase = true) ||
                            item.category.contains(searchQuery, ignoreCase = true)
                }

                if (filteredCatalog.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No inventory items match search.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredCatalog) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth().testTag("inventory_item_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Category: ${item.category}", fontSize = 11.sp, color = Color.Gray)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = "\u20B9${item.price}",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontSize = 12.sp
                                            )
                                            Badge(
                                                containerColor = if (item.stock > 5) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                            ) {
                                                Text(
                                                    text = "${item.stock} in stock",
                                                    color = if (item.stock > 5) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Quick stock adjustments
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (item.stock > 0) {
                                                    viewModel.addStationeryStock(
                                                        itemId = item.id,
                                                        name = item.name,
                                                        additionalStock = -1,
                                                        category = item.category,
                                                        price = item.price
                                                    )
                                                }
                                            },
                                            modifier = Modifier.testTag("decrement_stock_${item.id}")
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Reduce Stock", tint = MaterialTheme.colorScheme.primary)
                                        }

                                        Text(
                                            text = item.stock.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(24.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                viewModel.addStationeryStock(
                                                    itemId = item.id,
                                                    name = item.name,
                                                    additionalStock = 1,
                                                    category = item.category,
                                                    price = item.price
                                                )
                                            },
                                            modifier = Modifier.testTag("increment_stock_${item.id}")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Stock", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // PRINT JOBS QUEUE TAB
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "QUEUED", "PRINTING", "READY", "COMPLETED").forEach { status ->
                        FilterChip(
                            selected = printFilterStatus == status,
                            onClick = { printFilterStatus = status },
                            label = { Text(status, fontSize = 10.sp) },
                            modifier = Modifier.testTag("print_filter_$status")
                        )
                    }
                }

                val filteredPrintJobs = allPrintRequests.filter { req ->
                    val matchesStatus = printFilterStatus == "ALL" || req.status == printFilterStatus
                    val matchesSearch = searchQuery.isBlank() ||
                            req.fileName.contains(searchQuery, ignoreCase = true) ||
                            req.studentName.contains(searchQuery, ignoreCase = true) ||
                            req.studentId.contains(searchQuery, ignoreCase = true)
                    matchesStatus && matchesSearch
                }.sortedByDescending { it.timestamp }

                if (filteredPrintJobs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No matching print requests in queue.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredPrintJobs) { req ->
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("print_request_card_${req.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = req.fileName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "From: ${req.studentName} (${req.studentId})",
                                                fontSize = 11.sp,
                                                color = Color.Gray
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
                                            Text(
                                                text = req.status,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Print Options:", fontSize = 10.sp, color = Color.Gray)
                                            Text("${req.pagesCount} pgs • ${req.printType} • ${req.copyType}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            if (req.bindingType != "None") {
                                                Text("Binding: ${req.bindingType}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Text(
                                            text = "\u20B9${req.totalCost}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    if (req.status != "COMPLETED") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            when (req.status) {
                                                "QUEUED" -> {
                                                    Button(
                                                        onClick = {
                                                            viewModel.actionOnPrintRequest(req, "PRINTING")
                                                            Toast.makeText(context, "Printing document...", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.weight(1f).testTag("start_print_btn_${req.id}"),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Start Print", fontSize = 11.sp)
                                                    }
                                                }
                                                "PRINTING" -> {
                                                    Button(
                                                        onClick = {
                                                            viewModel.actionOnPrintRequest(req, "READY")
                                                            Toast.makeText(context, "Print completed & ready for pickup!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.weight(1f).testTag("ready_print_btn_${req.id}"),
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                                    ) {
                                                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Mark Ready", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                                "READY" -> {
                                                    Button(
                                                        onClick = {
                                                            viewModel.actionOnPrintRequest(req, "COMPLETED")
                                                            Toast.makeText(context, "Handed over documents!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.weight(1f).testTag("complete_print_btn_${req.id}"),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Handover", fontSize = 11.sp)
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
        }
    }

    // Modal dialog to add stock / create a new item
    if (showAddStockDialog) {
        AlertDialog(
            onDismissRequest = { showAddStockDialog = false },
            title = { Text("Add Item to Stationery", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newStockId,
                        onValueChange = { newStockId = it },
                        label = { Text("Unique Item ID (e.g. blue_pen)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_item_id_field")
                    )

                    OutlinedTextField(
                        value = newStockName,
                        onValueChange = { newStockName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth().testTag("add_item_name_field")
                    )

                    OutlinedTextField(
                        value = newStockAmount,
                        onValueChange = { newStockAmount = it },
                        label = { Text("Initial Stock Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_item_stock_field")
                    )

                    OutlinedTextField(
                        value = newStockPrice,
                        onValueChange = { newStockPrice = it },
                        label = { Text("Unit Price (\u20B9)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_item_price_field")
                    )

                    Text("Category:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Writing", "Notebooks", "Laboratory", "Folders").forEach { cat ->
                            FilterChip(
                                selected = newStockCategory == cat,
                                onClick = { newStockCategory = cat },
                                label = { Text(cat, fontSize = 9.sp) },
                                modifier = Modifier.testTag("add_category_chip_$cat")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val stockVal = newStockAmount.toIntOrNull()
                        val priceVal = newStockPrice.toDoubleOrNull()
                        if (newStockId.isNotBlank() && newStockName.isNotBlank() && stockVal != null && priceVal != null) {
                            viewModel.addStationeryStock(
                                itemId = newStockId.trim().lowercase(),
                                name = newStockName.trim(),
                                additionalStock = stockVal,
                                category = newStockCategory,
                                price = priceVal
                            )
                            Toast.makeText(context, "${newStockName.trim()} added to store catalog!", Toast.LENGTH_SHORT).show()
                            showAddStockDialog = false
                            newStockId = ""
                            newStockName = ""
                            newStockAmount = ""
                            newStockPrice = ""
                        } else {
                            Toast.makeText(context, "Please provide valid inputs.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("submit_add_item_btn")
                ) {
                    Text("Add & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
