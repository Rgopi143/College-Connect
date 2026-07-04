package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StationeryItem
import com.example.data.model.StationeryRequest
import com.example.ui.viewmodel.PortalViewModel

@Composable
fun StationeryScreen(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isStoreStaff = currentUser?.role == "STORE" || currentUser?.email?.endsWith("@necstationary.com") == true

    if (isStoreStaff) {
        StationeryPrintingStaffHub(viewModel = viewModel, modifier = modifier)
    } else {
        StudentStationeryView(viewModel = viewModel, modifier = modifier)
    }
}

@Composable
fun StudentStationeryView(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalogItems by viewModel.stationeryItems.collectAsState()
    val myRequests by viewModel.studentStationeryRequests.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var selectedItemForOrder by remember { mutableStateOf<StationeryItem?>(null) }
    var orderQuantity by remember { mutableIntStateOf(1) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Academic Stationery Store",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Pre-order textbooks, records and writing materials",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Item Catalog", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Pre-Orders (${myRequests.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
            }
        }

        if (selectedTab == 0) {
            if (catalogItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No inventory items found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1.0f)
                ) {
                    items(catalogItems) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Category: ${item.category}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("\u20B9${item.price}", fontWeight = FontWeight.SemiBold) },
                                            modifier = Modifier.height(26.dp)
                                        )
                                        SuggestionChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    text = if (item.stock > 0) "${item.stock} In Stock" else "Out of Stock",
                                                    color = if (item.stock > 5) Color(0xFF10B981) else Color.Red
                                                )
                                            },
                                            modifier = Modifier.height(26.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        selectedItemForOrder = item
                                        orderQuantity = 1
                                    },
                                    enabled = item.stock > 0,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Order", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Pre-orders status lists with collection notices
            if (myRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No pre-orders recorded yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(myRequests) { req ->
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
                                    Text(
                                        text = req.itemName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Badge(
                                        containerColor = when (req.status) {
                                            "PENDING" -> MaterialTheme.colorScheme.secondary
                                            "READY_FOR_COLLECTION" -> Color(0xFF10B981)
                                            "COLLECTED" -> Color.Gray
                                            else -> Color.DarkGray
                                        }
                                    ) {
                                        Text(
                                            text = if (req.status == "READY_FOR_COLLECTION") "READY FOR PICKUP" else req.status,
                                            fontSize = 9.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Qty: ${req.quantity} units", fontSize = 12.sp)
                                    Text("Total Paid: \u20B9${req.totalCost}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                if (req.status == "READY_FOR_COLLECTION") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFFE8F5E9),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Celebration,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Ready at counter! Show this app order token to collect",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2E7D32),
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

    // Checkout modal dialog
    if (selectedItemForOrder != null) {
        val item = selectedItemForOrder!!
        AlertDialog(
            onDismissRequest = { selectedItemForOrder = null },
            title = { Text("Confirm Pre-Order", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Item: ${item.name}", fontWeight = FontWeight.Bold)
                    Text("Unit Price: \u20B9${item.price}")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Select Quantity:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (orderQuantity > 1) orderQuantity-- },
                                enabled = orderQuantity > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            Text(
                                text = orderQuantity.toString(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { if (orderQuantity < item.stock) orderQuantity++ },
                                enabled = orderQuantity < item.stock
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estimated Total Price:", fontWeight = FontWeight.Bold)
                        Text(
                            text = "\u20B9${item.price * orderQuantity}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.purchaseStationeryItem(
                            item = item,
                            quantity = orderQuantity,
                            onSuccess = {
                                Toast.makeText(context, "Order placed successfully! Track status in Pre-orders tab.", Toast.LENGTH_LONG).show()
                                selectedItemForOrder = null
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Confirm Booking & Pay")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForOrder = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
