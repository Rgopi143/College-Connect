package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CanteenBooking
import com.example.data.model.CanteenItem
import com.example.ui.viewmodel.PortalViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.example.R

@Composable
fun CanteenScreen(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isCanteenStaff = currentUser?.role == "CANTEEN" || currentUser?.email?.endsWith("@neccanteen.com") == true

    var forceCustomerView by remember { mutableStateOf(false) }

    if (isCanteenStaff && !forceCustomerView) {
        Column(modifier = modifier.fillMaxSize()) {
            CanteenStaffHub(
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
                onSwitchToCustomer = { forceCustomerView = true }
            )
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            if (isCanteenStaff) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Staff Booking Session",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        TextButton(
                            onClick = { forceCustomerView = false }
                        ) {
                            Text("Back to Staff Hub", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            StudentCanteenView(viewModel = viewModel, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StudentCanteenView(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val menuItems by viewModel.canteenItems.collectAsState()
    val cart by viewModel.canteenCart.collectAsState()
    val myBookings by viewModel.studentCanteenBookings.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val categories = listOf("All", "Breakfast", "Lunch", "Snacks", "Dinner", "Beverages")

    var selectedBookingForBill by remember { mutableStateOf<CanteenBooking?>(null) }
    var showCheckoutSheet by remember { mutableStateOf(false) }

    val totalCartCost = cart.entries.sumOf { (id, qty) ->
        val item = menuItems.find { it.id == id }
        (item?.price ?: 0.0) * qty
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
                text = "Canteen Token Booking",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Skip the queue! Pre-book meals and generate QR tokens",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Pre-book Meal", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Food Tokens (${myBookings.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
            }
        }

        if (selectedTab == 0) {
            // Category scroll row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat, fontSize = 11.sp) }
                    )
                }
            }

            // Filter items by category
            val filteredMenu = if (selectedCategoryFilter == "All") menuItems else menuItems.filter { it.category == selectedCategoryFilter }

            if (filteredMenu.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No canteen items available currently.", color = Color.Gray)
                }
            } else {
                Box(modifier = Modifier.weight(1.0f)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredMenu) { item ->
                            val qtyInCart = cart[item.id] ?: 0
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
                                    val imageRes = when (item.category) {
                                        "Breakfast" -> R.drawable.img_breakfast_1781814942224
                                        "Lunch", "Dinner" -> R.drawable.img_meals_1781814956972
                                        "Snacks" -> R.drawable.img_snacks_1781814970298
                                        "Beverages" -> R.drawable.img_beverages_1781814985277
                                        else -> R.drawable.img_snacks_1781814970298
                                    }
                                    
                                    Image(
                                        painter = painterResource(id = imageRes),
                                        contentDescription = item.name,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = item.category,
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\u20B9${item.price}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 14.sp
                                        )
                                    }

                                    if (qtyInCart == 0) {
                                        Button(
                                            onClick = { viewModel.addToCanteenCart(item.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                            contentPadding = PaddingValues(horizontal = 14.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Add", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.removeFromCanteenCart(item.id) },
                                                modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            Text(qtyInCart.toString(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            IconButton(
                                                onClick = { viewModel.addToCanteenCart(item.id) },
                                                modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Cart bar
                    if (cart.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { showCheckoutSheet = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${cart.values.sum()} items in Cart",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "View Cart • \u20B9$totalCartCost",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Bookings tab lists with dynamic QR tokens details clickable
            if (myBookings.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No food token bookings found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(myBookings) { booking ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBookingForBill = booking },
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
                                        text = booking.qrToken,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Badge(
                                        containerColor = when (booking.status) {
                                            "BOOKED" -> MaterialTheme.colorScheme.secondary
                                            "READY_FOR_COLLECTION" -> Color(0xFFF59E0B)
                                            else -> Color.Gray
                                        }
                                    ) {
                                        Text(
                                            text = when (booking.status) {
                                                "BOOKED" -> "ACTIVE TOKEN"
                                                "READY_FOR_COLLECTION" -> "READY TO PICK"
                                                else -> "COLLECTED"
                                            },
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Items: ${booking.itemsJson}",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pre-booked via Digital Pay", fontSize = 11.sp, color = Color.Gray)
                                    Text("Paid: \u20B9${booking.totalCost}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Active Food token details modal
    if (selectedBookingForBill != null) {
        val bk = selectedBookingForBill!!
        AlertDialog(
            onDismissRequest = { selectedBookingForBill = null },
            title = { Text("E-Meal Token", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "CANTEEN QR RECEIPT",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    // Draw a visual matrix representation for scanning token
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing simulated QR pixel groupings using small icon or simple box grid
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (j in 0..4) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (i in 0..4) {
                                        val color = if ((i + j) % 2 == 0) MaterialTheme.colorScheme.primary else Color.White
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(color, RoundedCornerShape(2.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = bk.qrToken,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    HorizontalDivider()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DetailRow("User Profile:", bk.studentName)
                        DetailRow("Ordered Meals:", bk.itemsJson)
                        DetailRow("Amount Settled:", "\u20B9${bk.totalCost}")
                        DetailRow("Counter Status:", when (bk.status) {
                            "BOOKED" -> "AWAITING PREP"
                            "READY_FOR_COLLECTION" -> "MEAL READY!"
                            else -> "REDEEMED & HANDED OVER"
                        })
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedBookingForBill = null }) {
                    Text("Finished")
                }
            }
        )
    }

    // Cart Checkout Sheet modal trigger
    if (showCheckoutSheet) {
        AlertDialog(
            onDismissRequest = { showCheckoutSheet = false },
            title = { Text("Confirm Pre-paid Canteen Booking", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your cart summary:", fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.heightIn(max = 150.dp)
                    ) {
                        cart.forEach { (id, qty) ->
                            val item = menuItems.find { it.id == id }
                            if (item != null) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${item.name} x$qty")
                                        Text("\u20B9${item.price * qty}", fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount to Pay:", fontWeight = FontWeight.Bold)
                        Text(
                            text = "\u20B9$totalCartCost",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Deducted smoothly from Unified Campus Wallet.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bookCanteenCart(
                            onSuccess = { booking ->
                                selectedBookingForBill = booking
                                showCheckoutSheet = false
                                Toast.makeText(context, "Canteen Food Token pre-booked! Order code: ${booking.qrToken}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text("Secure Pay & Book Token")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CanteenStaffHub(
    viewModel: PortalViewModel,
    modifier: Modifier = Modifier,
    onSwitchToCustomer: () -> Unit = {}
) {
    val context = LocalContext.current
    val allBookings by viewModel.allCanteenBookings.collectAsState()
    val menuItems by viewModel.canteenItems.collectAsState()

    var staffTabState by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var orderStatusFilter by remember { mutableStateOf("ALL") }

    // Add item states
    var showAddItemDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemPrice by remember { mutableStateOf("") }
    var newItemCategory by remember { mutableStateOf("Breakfast") }

    // Simulated QR Code Scanner Modal
    var showScannerModal by remember { mutableStateOf(false) }
    var scannedBookingResult by remember { mutableStateOf<CanteenBooking?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Canteen Staff Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Canteen Counter Hub",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Redeem tokens, manage dishes, & view live sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.Fastfood,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        // Button to switch to customer view
        Button(
            onClick = onSwitchToCustomer,
            modifier = Modifier.fillMaxWidth().testTag("staff_book_food_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Switch to Customer View (Book Food)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        TabRow(selectedTabIndex = staffTabState) {
            Tab(selected = staffTabState == 0, onClick = { staffTabState = 0 }) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tokens", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = staffTabState == 1, onClick = { staffTabState = 1 }) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Menu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = staffTabState == 2, onClick = { staffTabState = 2 }) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Insights", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        when (staffTabState) {
            0 -> {
                // REDEMPTIONS TAB
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Token Validation & Scan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search by Code / Name", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).testTag("token_search_field"),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Button(
                                onClick = { 
                                    showScannerModal = true 
                                    scannedBookingResult = null
                                },
                                modifier = Modifier.testTag("simulate_scan_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan QR", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "BOOKED", "READY_FOR_COLLECTION", "COMPLETED").forEach { status ->
                        val display = when (status) {
                            "ALL" -> "All"
                            "BOOKED" -> "Awaiting Prep"
                            "READY_FOR_COLLECTION" -> "Ready"
                            else -> "Redeemed"
                        }
                        FilterChip(
                            selected = orderStatusFilter == status,
                            onClick = { orderStatusFilter = status },
                            label = { Text(display, fontSize = 10.sp) }
                        )
                    }
                }

                val activeList = allBookings.filter { bk ->
                    val matchesStatus = orderStatusFilter == "ALL" || bk.status == orderStatusFilter
                    val matchesSearch = searchQuery.isBlank() || 
                            bk.qrToken.contains(searchQuery, ignoreCase = true) ||
                            bk.studentName.contains(searchQuery, ignoreCase = true) ||
                            bk.studentId.contains(searchQuery, ignoreCase = true)
                    matchesStatus && matchesSearch
                }.sortedByDescending { it.timestamp }

                if (activeList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No matching canteen tokens found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(activeList) { booking ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = booking.qrToken,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "${booking.studentName} (${booking.studentId})",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Badge(
                                            containerColor = when (booking.status) {
                                                "BOOKED" -> MaterialTheme.colorScheme.secondary
                                                "READY_FOR_COLLECTION" -> Color(0xFFF59E0B)
                                                else -> Color.Gray
                                            }
                                        ) {
                                            Text(
                                                text = when (booking.status) {
                                                    "BOOKED" -> "Awaiting Prep"
                                                    "READY_FOR_COLLECTION" -> "Ready to Pick"
                                                    else -> "Collected"
                                                },
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Items Ordered:", fontSize = 10.sp, color = Color.Gray)
                                            Text(booking.itemsJson, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Text(
                                            text = "\u20B9${booking.totalCost}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    if (booking.status != "COMPLETED") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (booking.status == "BOOKED") {
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.actionOnCanteenBooking(booking, "READY_FOR_COLLECTION")
                                                        Toast.makeText(context, "Marked ready! Student notified.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Mark Ready", fontSize = 11.sp)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.actionOnCanteenBooking(booking, "COMPLETED")
                                                    Toast.makeText(context, "Token Redeemed! Handed over meals.", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f).testTag("redeem_btn_${booking.qrToken}"),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Handover / Redeem", fontSize = 11.sp)
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
                // MENU ADMIN TAB
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Live Menu (${menuItems.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = { showAddItemDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_item_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Dish", fontSize = 11.sp)
                    }
                }

                if (menuItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No dishes in menu.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(menuItems) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(item.category, fontSize = 10.sp, color = Color.Gray)
                                        Text("\u20B9${item.price}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                viewModel.saveCanteenItem(item.copy(isAvailable = !item.isAvailable))
                                            }
                                        ) {
                                            Checkbox(
                                                checked = item.isAvailable,
                                                onCheckedChange = { checked ->
                                                    viewModel.saveCanteenItem(item.copy(isAvailable = checked))
                                                }
                                            )
                                            Text("Available", fontSize = 11.sp)
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteCanteenItem(item)
                                                Toast.makeText(context, "${item.name} deleted", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // INSIGHTS TAB
                val completedBookings = allBookings.filter { it.status == "COMPLETED" }
                val totalRevenue = completedBookings.sumOf { it.totalCost }
                val activeCount = allBookings.count { it.status == "BOOKED" || it.status == "READY_FOR_COLLECTION" }
                val awaitingCount = allBookings.count { it.status == "BOOKED" }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Total Revenue Realized", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("\u20B9${totalRevenue}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("Calculated from ${completedBookings.size} completed redemptions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Active Prep", fontSize = 11.sp, color = Color.Gray)
                                    Text("$awaitingCount tokens", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Pending Deliveries", fontSize = 11.sp, color = Color.Gray)
                                    Text("$activeCount tokens", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFF59E0B))
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Redeemed Orders History", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                
                                if (completedBookings.isEmpty()) {
                                    Text("No order redemptions recorded yet.", color = Color.Gray, fontSize = 11.sp)
                                } else {
                                    completedBookings.take(15).forEach { bk ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(bk.qrToken, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text("${bk.studentName} • ${bk.itemsJson}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                            }
                                            Text("\u20B9${bk.totalCost}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray)
                                        }
                                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.05f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue to add menu dish
    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add Dish to Canteen Menu", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Dish Name") },
                        modifier = Modifier.fillMaxWidth().testTag("dish_name_field")
                    )

                    OutlinedTextField(
                        value = newItemPrice,
                        onValueChange = { newItemPrice = it },
                        label = { Text("Price (\u20B9)") },
                        modifier = Modifier.fillMaxWidth().testTag("dish_price_field")
                    )

                    Text("Category:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Breakfast", "Lunch", "Snacks", "Dinner", "Beverages").forEach { cat ->
                            FilterChip(
                                selected = newItemCategory == cat,
                                onClick = { newItemCategory = cat },
                                label = { Text(cat, fontSize = 9.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceVal = newItemPrice.toDoubleOrNull()
                        if (newItemName.isNotBlank() && priceVal != null) {
                            viewModel.saveCanteenItem(
                                CanteenItem(
                                    name = newItemName,
                                    price = priceVal,
                                    category = newItemCategory,
                                    isAvailable = true
                                )
                            )
                            Toast.makeText(context, "$newItemName added to live menu!", Toast.LENGTH_SHORT).show()
                            newItemName = ""
                            newItemPrice = ""
                            showAddItemDialog = false
                        } else {
                            Toast.makeText(context, "Invalid inputs", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_dish_btn")
                ) {
                    Text("Save to Menu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Mock QR Scanner Dialog View
    if (showScannerModal) {
        val transition = rememberInfiniteTransition(label = "laser_transition")
        val laserProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laser"
        )

        val unredeemedBookings = allBookings.filter { it.status == "BOOKED" || it.status == "READY_FOR_COLLECTION" }

        AlertDialog(
            onDismissRequest = { showScannerModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Live QR Token Scanner", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Simulating scanner camera viewfinder. Direct the camera lens towards the digital token code.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    // Scanning viewfinder
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing corner indicator brackets
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 8f
                            val lineLength = 40f
                            // Top left corner
                            drawLine(Color.Green, androidx.compose.ui.geometry.Offset(10f, 10f), androidx.compose.ui.geometry.Offset(10f + lineLength, 10f), strokeWidth)
                            drawLine(Color.Green, androidx.compose.ui.geometry.Offset(10f, 10f), androidx.compose.ui.geometry.Offset(10f, 10f + lineLength), strokeWidth)
                            // Top right corner
                            drawLine(Color.Green, androidx.compose.ui.geometry.Offset(size.width - 10f, 10f), androidx.compose.ui.geometry.Offset(size.width - 10f - lineLength, 10f), strokeWidth)
                            drawLine(Color.Green, androidx.compose.ui.geometry.Offset(size.width - 10f, 10f), androidx.compose.ui.geometry.Offset(size.width - 10f, 10f + lineLength), strokeWidth)
                            // Bottom left corner
                            drawLine(Color.Green, androidx.compose.ui.geometry.Offset(10f, size.height - 10f), androidx.compose.ui.geometry.Offset(10f + lineLength, size.height - 10f), strokeWidth)
                            drawLine(Color.Green, androidx.compose.ui.geometry.Offset(10f, size.height - 10f), androidx.compose.ui.geometry.Offset(10f, size.height - 10f - lineLength), strokeWidth)
                            // Bottom right corner
                            drawLine(Color.Green, androidx.compose.ui.geometry.Offset(size.width - 10f, size.height - 10f), androidx.compose.ui.geometry.Offset(size.width - 10f - lineLength, size.height - 10f), strokeWidth)
                            drawLine(Color.Green, androidx.compose.ui.geometry.Offset(size.width - 10f, size.height - 10f), androidx.compose.ui.geometry.Offset(size.width - 10f, size.height - 10f - lineLength), strokeWidth)
                        }

                        // Simulated green glowing laser line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = (180 * laserProgress).dp)
                                .background(Color.Green)
                        )

                        Text(
                            text = if (scannedBookingResult != null) "TOKEN IDENTIFIED" else "AWAITING CODE...",
                            color = if (scannedBookingResult != null) Color.Green else Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (scannedBookingResult == null) {
                        // Dropdown or scroll to choose code to simulate
                        Text(
                            text = "Choose Student Token to Simulate Scanning:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )

                        if (unredeemedBookings.isEmpty()) {
                            Text("No active unredeemed tokens currently.", color = Color.Gray, fontSize = 11.sp)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 100.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(unredeemedBookings) { booking ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                scannedBookingResult = booking
                                                Toast.makeText(context, "Beep! Scanned ${booking.qrToken}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            booking.qrToken,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text("${booking.studentName} (${booking.studentId})", fontSize = 10.sp, color = Color.DarkGray)
                                    }
                                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                                }
                            }
                        }
                    } else {
                        val scan = scannedBookingResult!!
                        // Scanned result card details!
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Token Decoded Successfully!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                DetailRow("Student Name:", scan.studentName)
                                DetailRow("Student ID:", scan.studentId)
                                DetailRow("Meals ordered:", scan.itemsJson)
                                DetailRow("Amt Settled:", "\u20B9${scan.totalCost}")
                                DetailRow("Current Status:", scan.status)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.actionOnCanteenBooking(scan, "COMPLETED")
                                Toast.makeText(context, "Successfully Redeemed ${scan.qrToken}! Student notification sent.", Toast.LENGTH_SHORT).show()
                                showScannerModal = false
                                scannedBookingResult = null
                            },
                            modifier = Modifier.fillMaxWidth().testTag("scanner_redeem_confirm_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Redeem & Issue Meals", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = { scannedBookingResult = null }
                        ) {
                            Text("Scan Another Token", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { 
                        showScannerModal = false 
                        scannedBookingResult = null
                    }
                ) {
                    Text("Close Scanner")
                }
            }
        )
    }
}

