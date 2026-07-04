package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.PortalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackRequestsScreen(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val myOutpasses by viewModel.studentOutpasses.collectAsState()
    val myCertificates by viewModel.studentCertificates.collectAsState()
    val myStationeries by viewModel.studentStationeryRequests.collectAsState()
    val myPrints by viewModel.studentPrintRequests.collectAsState()
    val myCanteenBookings by viewModel.studentCanteenBookings.collectAsState()

    var currentRequestFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Outpasses", "Certificates", "Store", "Prints", "Canteen")

    // Let's create an aggregated timeline item representation
    val timelineItems = remember(myOutpasses, myCertificates, myStationeries, myPrints, myCanteenBookings) {
        val list = mutableListOf<RequestSummaryItem>()
        
        myOutpasses.forEach {
            list.add(
                RequestSummaryItem(
                    title = "Outpass Clearance: ${it.qrText}",
                    subtitle = "Reason: ${it.reason}",
                    status = it.status,
                    cost = null,
                    dateLabel = it.dateTime,
                    type = "Outpasses",
                    timestamp = it.timestamp
                )
            )
        }
        
        myCertificates.forEach {
            list.add(
                RequestSummaryItem(
                    title = "Certificate applied: ${it.certificateType}",
                    subtitle = "Purpose: ${it.details}",
                    status = it.status,
                    cost = null,
                    dateLabel = "A/Y 2025-26",
                    type = "Certificates",
                    timestamp = it.timestamp
                )
            )
        }
        
        myStationeries.forEach {
            list.add(
                RequestSummaryItem(
                    title = "Stationery pre-order: ${it.itemName}",
                    subtitle = "Quantity: ${it.quantity} units",
                    status = it.status,
                    cost = "\u20B9${it.totalCost}",
                    dateLabel = "Store pick-up",
                    type = "Store",
                    timestamp = it.timestamp
                )
            )
        }

        myPrints.forEach {
            list.add(
                RequestSummaryItem(
                    title = "Print Draft: ${it.fileName}",
                    subtitle = "${it.pagesCount} pgs • ${it.printType} • ${it.bindingType}",
                    status = it.status,
                    cost = "\u20B9${it.totalCost}",
                    dateLabel = "Printer Queue",
                    type = "Prints",
                    timestamp = it.timestamp
                )
            )
        }

        myCanteenBookings.forEach {
            list.add(
                RequestSummaryItem(
                    title = "Canteen Token Booking: ${it.qrToken}",
                    subtitle = "Meals: ${it.itemsJson}",
                    status = it.status,
                    cost = "\u20B9${it.totalCost}",
                    dateLabel = "Serving counter",
                    type = "Canteen",
                    timestamp = it.timestamp
                )
            )
        }

        // Sort by timestamp desc
        list.sortByDescending { it.timestamp }
        list
    }

    val filteredList = if (currentRequestFilter == "All") timelineItems 
                       else timelineItems.filter { it.type == currentRequestFilter }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Track Campus Requests",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Consolidated tracking list of your papers and transactions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        // Filter chips list
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { flt ->
                FilterChip(
                    selected = currentRequestFilter == flt,
                    onClick = { currentRequestFilter = flt },
                    label = { Text(flt, fontSize = 11.sp) }
                )
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterListOff,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No recorded requests under '$currentRequestFilter'",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredList) { item ->
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
                                    val icon = when (item.type) {
                                        "Outpasses" -> Icons.AutoMirrored.Filled.DirectionsRun
                                        "Certificates" -> Icons.Default.CardMembership
                                        "Store" -> Icons.Default.Storefront
                                        "Prints" -> Icons.Default.Print
                                        else -> Icons.Default.Fastfood
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Badge(
                                    containerColor = when (item.status) {
                                        "APPROVED", "READY", "COLLECTED", "COMPLETED" -> Color(0xFF10B981)
                                        "REJECTED" -> Color.Red
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                ) {
                                    val readableStat = if (item.status.startsWith("PENDING")) "PENDING" else item.status
                                    Text(
                                        text = readableStat,
                                        fontSize = 8.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Section: ${item.type} • ${item.dateLabel}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                item.cost?.let {
                                    Text(
                                        text = it,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
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

data class RequestSummaryItem(
    val title: String,
    val subtitle: String,
    val status: String,
    val cost: String?,
    val dateLabel: String,
    val type: String,
    val timestamp: Long
)
