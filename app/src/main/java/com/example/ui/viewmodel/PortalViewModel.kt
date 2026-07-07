package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PortalViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    // --- CURRENTS ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private var notificationsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var chatListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        viewModelScope.launch {
            try {
                val loggedInUser = repository.getAllUsers().first().find { it.isLoggedIn }
                if (loggedInUser != null && !loggedInUser.isPaused) {
                    _currentUser.value = loggedInUser
                    setupRealtimeListeners(loggedInUser)
                    cleanCacheAndPullFirestore()
                }
            } catch (e: Exception) {
                android.util.Log.e("PortalViewModel", "Auto-login failed: ${e.message}")
            }
        }
    }

    private fun setupRealtimeListeners(user: User) {
        setupRealtimeNotificationsListener(user)
        setupRealtimeChatListener()
    }

    private fun setupRealtimeChatListener() {
        chatListenerRegistration?.remove()
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            chatListenerRegistration = db.collection("chat_messages")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        android.util.Log.e("RealtimeChat", "Listener failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null && !snapshots.isEmpty) {
                        viewModelScope.launch {
                            for (doc in snapshots.documentChanges) {
                                if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                    try {
                                        val message = doc.document.toObject(ChatMessage::class.java)
                                        if (message != null) {
                                            val exists = repository.getAllChatMessages().first().any { 
                                                it.senderId == message.senderId && 
                                                it.timestamp == message.timestamp && 
                                                it.messageText == message.messageText 
                                            }
                                            if (!exists) {
                                                repository.insertRawChatMessage(message)
                                                android.util.Log.d("RealtimeChat", "Synced new chat message from ${message.senderName}: ${message.messageText}")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("RealtimeChat", "Failed to parse/insert chat message: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("RealtimeChat", "Error setting up chat listener: ${e.message}")
        }
    }

    private fun setupRealtimeNotificationsListener(user: User) {
        notificationsListenerRegistration?.remove()
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            notificationsListenerRegistration = db.collection("notifications")
                .whereIn("targetStudentId", listOf("ALL", user.userId))
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        android.util.Log.e("RealtimeNotifications", "Listener failed: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null && !snapshots.isEmpty) {
                        viewModelScope.launch {
                            for (doc in snapshots.documentChanges) {
                                if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                    try {
                                        val notif = doc.document.toObject(CollegeNotification::class.java)
                                        if (notif != null) {
                                            val exists = repository.doesNotificationExist(notif.title, notif.content, notif.timestamp)
                                            if (!exists) {
                                                repository.insertRawNotification(notif)
                                                android.util.Log.d("RealtimeNotifications", "Synced new realtime notification: ${notif.title}")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("RealtimeNotifications", "Failed to parse/insert notification: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("RealtimeNotifications", "Error setting up listener: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationsListenerRegistration?.remove()
        chatListenerRegistration?.remove()
    }

    val isFirebaseConnected: StateFlow<Boolean> = flow {
        val connected = try {
            com.google.firebase.FirebaseApp.getApps(application).isNotEmpty()
        } catch (e: Exception) {
            false
        }
        emit(connected)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // --- FLOWS ---
    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOutpasses: StateFlow<List<OutpassRequest>> = repository.getAllOutpasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentOutpasses: StateFlow<List<OutpassRequest>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getOutpassesForStudent(user.userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCertificates: StateFlow<List<CertificateRequest>> = repository.getAllCertificateRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentCertificates: StateFlow<List<CertificateRequest>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getCertificatesForStudent(user.userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stationeryItems: StateFlow<List<StationeryItem>> = repository.getAllStationeryItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStationeryRequests: StateFlow<List<StationeryRequest>> = repository.getAllStationeryRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentStationeryRequests: StateFlow<List<StationeryRequest>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getStationeryRequestsForStudent(user.userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPrintRequests: StateFlow<List<PrintRequest>> = repository.getAllPrintRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentPrintRequests: StateFlow<List<PrintRequest>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getPrintRequestsForStudent(user.userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canteenItems: StateFlow<List<CanteenItem>> = repository.getAllCanteenItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCanteenBookings: StateFlow<List<CanteenBooking>> = repository.getAllCanteenBookings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentCanteenBookings: StateFlow<List<CanteenBooking>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getCanteenBookingsForStudent(user.userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<CollegeNotification>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user -> repository.getNotificationsForStudent(user.userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.getAllChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEvents: StateFlow<List<CollegeEvent>> = repository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createEvent(title: String, description: String, date: String, time: String, venue: String, dpt: String = "ALL") {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val event = CollegeEvent(
                title = title.trim(),
                description = description.trim(),
                date = date.trim(),
                time = time.trim(),
                venue = venue.trim(),
                organizerRole = user.role,
                filterDepartment = dpt.trim(),
                isPaused = false
            )
            repository.insertEvent(event)
        }
    }

    fun updateEvent(event: CollegeEvent) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }

    fun deleteEvent(event: CollegeEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    // --- CART STATE (Canteen & Stationery) ---
    private val _canteenCart = MutableStateFlow<Map<Int, Int>>(emptyMap()) // itemId -> qty
    val canteenCart: StateFlow<Map<Int, Int>> = _canteenCart.asStateFlow()

    fun addToCanteenCart(itemId: Int) {
        val current = _canteenCart.value.toMutableMap()
        current[itemId] = (current[itemId] ?: 0) + 1
        _canteenCart.value = current
    }

    fun removeFromCanteenCart(itemId: Int) {
        val current = _canteenCart.value.toMutableMap()
        val qty = current[itemId] ?: 0
        if (qty > 1) {
            current[itemId] = qty - 1
        } else {
            current.remove(itemId)
        }
        _canteenCart.value = current
    }

    fun clearCanteenCart() {
        _canteenCart.value = emptyMap()
    }

    // --- ACTIONS ---
    fun login(userId: String, password: String) {
        viewModelScope.launch {
            _loginError.value = null
            try {
                if (userId.trim().isBlank()) {
                    _loginError.value = "User ID/Email cannot be empty."
                    return@launch
                }
                if (password.trim().isBlank()) {
                    _loginError.value = "Password cannot be empty."
                    return@launch
                }
                val cleanPassword = password.trim()
                var user = repository.getUser(userId.trim())
                if (user == null) {
                    val fetchedUser = repository.getUserFromFirestore(userId.trim())
                    if (fetchedUser != null) {
                        android.util.Log.d("LoginDebug", "Fetched user from Firestore: ${fetchedUser.userId}, dbPassword='${fetchedUser.password}', inputPassword='$cleanPassword'")
                        repository.insertUser(fetchedUser)
                        user = fetchedUser
                    }
                }
                if (user != null) {
                    android.util.Log.d("LoginDebug", "User from local DB: ${user.userId}, dbPassword='${user.password}', inputPassword='$cleanPassword'")
                    if (user.password != cleanPassword) {
                        _loginError.value = "Invalid password."
                        return@launch
                    }
                    if (user.isPaused) {
                        _loginError.value = "Your account has been suspended/paused by Administration."
                        return@launch
                    }
                    val loggedUser = user.copy(isLoggedIn = true)
                    repository.updateUser(loggedUser)
                    _currentUser.value = loggedUser
                    setupRealtimeListeners(loggedUser)
                    cleanCacheAndPullFirestore()
                    
                    // Trigger onboarding welcome alert
                    repository.createNotification(
                        studentId = loggedUser.userId,
                        title = "Session Started",
                        content = "Successfully logged in as ${loggedUser.name} (${loggedUser.role})",
                        category = "General"
                    )
                } else {
                    _loginError.value = "User not found! Try registering a new account."
                }
            } catch (e: Exception) {
                android.util.Log.e("PortalViewModel", "Login error: ${e.message}", e)
                _loginError.value = "Login failed: ${e.localizedMessage}"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                val loggedOutUser = user.copy(isLoggedIn = false)
                repository.updateUser(loggedOutUser)
            }
            notificationsListenerRegistration?.remove()
            notificationsListenerRegistration = null
            chatListenerRegistration?.remove()
            chatListenerRegistration = null
            _currentUser.value = null
            clearCanteenCart()
        }
    }

    fun updateUserProfile(
        name: String,
        email: String,
        phone: String,
        parentContact: String,
        department: String
    ) {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                val updated = user.copy(
                    name = name.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    parentContact = parentContact.trim(),
                    department = department.trim()
                )
                repository.updateUser(updated)
                _currentUser.value = updated
                setupRealtimeListeners(updated)
            }
        }
    }

    fun registerNewStudent(userId: String, name: String, roll: String, dept: String) {
        registerNewUser(userId, name, roll, dept, "${userId.lowercase()}@nrtec.in", "STUDENT", "pass")
    }

    fun registerNewUser(userId: String, name: String, roll: String, dept: String, email: String, role: String, password: String, autoLogin: Boolean = false) {
        viewModelScope.launch {
            _loginError.value = null
            var existing = repository.getUser(userId.trim()) ?: repository.getUser(email.trim())
            if (existing == null) {
                existing = repository.getUserFromFirestore(userId.trim()) ?: repository.getUserFromFirestore(email.trim())
            }
            if (existing != null) {
                _loginError.value = "Registration failed: User ID or Email already exists."
                return@launch
            }
            val user = User(
                userId = userId.trim(),
                name = name.trim(),
                rollNumber = roll.trim(),
                department = dept.trim(),
                email = email.trim(),
                phone = "+91 9000000000",
                parentContact = "+91 9111111111",
                role = role,
                password = password,
                isLoggedIn = false,
                isPaused = false
            )
            repository.insertUser(user)
            if (autoLogin) {
                login(userId.trim(), password)
            }
        }
    }

    fun saveUser(user: User) {
        viewModelScope.launch {
            repository.insertUser(user)
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    fun updateUserAdmin(user: User) {
        viewModelScope.launch {
            repository.updateUser(user)
        }
    }

    // --- MODULE OUTPASS ---
    fun submitOutpass(dateTime: String, reason: String, expectedReturnTime: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createOutpass(
                studentId = user.userId,
                studentName = user.name,
                rollNumber = user.rollNumber,
                department = user.department,
                dateTime = dateTime,
                reason = reason,
                expectedReturnTime = expectedReturnTime,
                parentContact = user.parentContact
            )
        }
    }

    fun actionOnOutpass(request: OutpassRequest, approve: Boolean, comment: String = "") {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            if (approve) {
                repository.approveOutpass(request, user.role)
            } else {
                repository.rejectOutpass(request, user.role, comment)
            }
        }
    }

    // --- MODULE CERTIFICATES ---
    fun submitCertificateRequest(certType: String, purpose: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createCertificate(
                studentId = user.userId,
                studentName = user.name,
                rollNumber = user.rollNumber,
                department = user.department,
                certificateType = certType,
                details = purpose
            )
        }
    }

    fun actionOnCertificate(request: CertificateRequest, approve: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            if (approve) {
                repository.approveCertificate(request, user.role)
            } else {
                repository.rejectCertificate(request, user.role)
            }
        }
    }

    // --- MODULE STATIONERY ---
    fun purchaseStationeryItem(item: StationeryItem, quantity: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val success = repository.placeStationeryOrder(
                studentId = user.userId,
                studentName = user.name,
                itemId = item.id,
                itemName = item.name,
                quantity = quantity,
                pricePerUnit = item.price
            )
            if (success) {
                onSuccess()
            } else {
                onError("Insufficient stock! Only ${item.stock} left.")
            }
        }
    }

    fun changeStationeryRequestStatus(request: StationeryRequest, newStatus: String) {
        viewModelScope.launch {
            repository.completeStationeryRequest(request, newStatus)
        }
    }

    fun addStationeryStock(itemId: String, name: String, additionalStock: Int, category: String, price: Double) {
        viewModelScope.launch {
            val item = repository.getUser(itemId) // check if exist via fallback or fetch
            val existing = repository.getAllStationeryItems().first().find { it.id == itemId }
            if (existing != null) {
                repository.updateStationeryItem(existing.copy(stock = existing.stock + additionalStock))
            } else {
                repository.updateStationeryItem(
                    StationeryItem(id = itemId, name = name, stock = additionalStock, category = category, price = price)
                )
            }
        }
    }

    // --- MODULE PRINTSHOP ---
    fun submitPrintRequest(fileName: String, pages: Int, printType: String, copyType: String, bindingType: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.createPrintRequest(
                studentId = user.userId,
                studentName = user.name,
                fileName = fileName,
                pagesCount = pages,
                printType = printType,
                copyType = copyType,
                bindingType = bindingType
            )
        }
    }

    fun actionOnPrintRequest(request: PrintRequest, newStatus: String) {
        viewModelScope.launch {
            repository.updatePrintStatus(request, newStatus)
        }
    }

    fun actionOnMultiplePrintRequests(requests: List<PrintRequest>, newStatus: String) {
        viewModelScope.launch {
            requests.forEach { req ->
                repository.updatePrintStatus(req, newStatus)
            }
        }
    }

    // --- MODULE CANTEEN ---
    fun bookCanteenCart(onSuccess: (CanteenBooking) -> Unit) {
        val user = _currentUser.value ?: return
        val cartItems = _canteenCart.value
        if (cartItems.isEmpty()) return

        viewModelScope.launch {
            val itemsList = canteenItems.value
            var total = 0.0
            val summaryBuilder = StringBuilder()

            cartItems.forEach { (id, qty) ->
                val item = itemsList.find { it.id == id }
                if (item != null) {
                    total += item.price * qty
                    summaryBuilder.append("${item.name} x$qty, ")
                }
            }
            val summary = summaryBuilder.toString().removeSuffix(", ")

            val booking = repository.placeCanteenBooking(
                studentId = user.userId,
                studentName = user.name,
                itemsSummary = summary,
                totalCost = total
            )
            clearCanteenCart()
            onSuccess(booking)
        }
    }

    fun actionOnCanteenBooking(booking: CanteenBooking, newStatus: String) {
        viewModelScope.launch {
            repository.completeCanteenBooking(booking, newStatus)
        }
    }

    fun saveCanteenItem(item: CanteenItem) {
        viewModelScope.launch {
            repository.insertCanteenItem(item)
        }
    }

    fun deleteCanteenItem(item: CanteenItem) {
        viewModelScope.launch {
            repository.deleteCanteenItem(item)
        }
    }

    // --- UTILITIES ---
    fun markAllNotificationsRead() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(user.userId)
        }
    }

    fun markNotificationRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun broadcastNotification(title: String, content: String, category: String) {
        viewModelScope.launch {
            repository.createNotification("ALL", title, content, category)
        }
    }

    // --- SECURITY DOOR HISTORY & CHAT INTEGRATION ---
    fun verifyExitSecurity(request: OutpassRequest) {
        viewModelScope.launch {
            repository.verifyOutpassExit(request)
        }
    }

    fun sendChatMessage(recipientRole: String, messageText: String, isSheet: Boolean = false, sheetData: String? = null) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val message = ChatMessage(
                senderId = user.userId,
                senderName = user.name,
                senderRole = user.role,
                recipientRole = recipientRole,
                messageText = messageText,
                isSheetAttachment = isSheet,
                attachmentData = sheetData
            )
            repository.sendChatMessage(message)
        }
    }

    // --- FIREBASE SECURITY DOOR CONTROLLER & CONNECTIVITY TESTING ---
    private val _firebaseTestState = MutableStateFlow<String>("IDLE")
    val firebaseTestState: StateFlow<String> = _firebaseTestState.asStateFlow()

    private val _firebaseSyncProgress = MutableStateFlow<String>("IDLE")
    val firebaseSyncProgress: StateFlow<String> = _firebaseSyncProgress.asStateFlow()

    private val _firestorePullStatus = MutableStateFlow<String>("IDLE")
    val firestorePullStatus: StateFlow<String> = _firestorePullStatus.asStateFlow()

    fun resetFirebaseTestState() {
        _firebaseTestState.value = "IDLE"
        _firestorePullStatus.value = "IDLE"
    }

    fun cleanCacheAndPullFirestore() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _firestorePullStatus.value = "SYNCING"
            try {
                val result = repository.pullFromFirestoreAndCleanCache(user.userId)
                if (result == "SUCCESS") {
                    _firestorePullStatus.value = "SUCCESS"
                } else {
                    _firestorePullStatus.value = "FAILED: $result"
                }
            } catch (e: Exception) {
                _firestorePullStatus.value = "FAILED: ${e.localizedMessage}"
            }
        }
    }

    fun testFirebaseWrite() {
        viewModelScope.launch {
            _firebaseTestState.value = "TESTING"
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val testDocRef = db.collection("connectivity_tests").document()
                val testData = mapOf(
                    "testerId" to (_currentUser.value?.userId ?: "anonymous"),
                    "testerName" to (_currentUser.value?.name ?: "System Diagnostics"),
                    "timestamp" to System.currentTimeMillis(),
                    "client" to "Android Jetpack Compose",
                    "status" to "Successful Firestore Connection Test"
                )
                testDocRef.set(testData)
                    .addOnSuccessListener {
                        _firebaseTestState.value = "SUCCESS: Real-time write succeeded! Document created in 'connectivity_tests' with ID: ${testDocRef.id}."
                    }
                    .addOnFailureListener { e ->
                        _firebaseTestState.value = "FAILED: Cloud rejected request: ${e.localizedMessage}. Ensure Firestore Security Rules are set to public during initial setup."
                    }
            } catch (e: Exception) {
                _firebaseTestState.value = "ERROR: Failed to initialize write request. Details: ${e.localizedMessage}"
            }
        }
    }

    fun bulkSyncToFirebase(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            _firebaseSyncProgress.value = "SYNCING"
            try {
                val users = allUsers.value
                val outpasses = allOutpasses.value
                val certificates = allCertificates.value
                val stationery = allStationeryRequests.value
                val prints = allPrintRequests.value
                val canteen = allCanteenBookings.value
                val stationeryItemsVal = stationeryItems.value
                val canteenItemsVal = canteenItems.value
                val chatMessagesVal = chatMessages.value
                val eventsVal = allEvents.value
                val notificationsVal = notifications.value
                
                repository.forceBulkSyncToCloud(
                    users, outpasses, certificates, stationery, prints, canteen,
                    stationeryItemsVal, canteenItemsVal, chatMessagesVal, eventsVal, notificationsVal
                )
                _firebaseSyncProgress.value = "SUCCESS"
                onComplete("Successfully synchronized all data: ${users.size} users, ${outpasses.size} outpasses, ${certificates.size} certificates, ${stationery.size} stationery orders, ${prints.size} prints, ${canteen.size} canteen orders, ${stationeryItemsVal.size} stationery items, ${canteenItemsVal.size} canteen items, ${chatMessagesVal.size} chat messages, ${eventsVal.size} events, and ${notificationsVal.size} notifications to live Cloud Firestore!")
            } catch (e: Exception) {
                _firebaseSyncProgress.value = "FAILED"
                onComplete("Failed to synchronize offline data: ${e.localizedMessage}")
            }
        }
    }

    // --- TPC DEPARTMENTS & FACULTY DIRECTORY & TPC STAFF ---
    private val _tpcStaffList = MutableStateFlow<List<TpcStaff>>(
        listOf(
            TpcStaff("TPC1", "Mr. Gopinath Reddy", "Director - Training & Placements", "gopinath.tpc@college.edu", "+91 98765 43210", "Overall placement execution & corporate relations"),
            TpcStaff("TPC2", "Dr. S. K. Sharma", "Head of Industry Relations", "sksharma.tpc@college.edu", "+91 94412 34567", "MOU signing, industrial visits & guest sessions"),
            TpcStaff("TPC3", "Mrs. Anitha Paul", "Chief Placement Coordinator", "anitha.tpc@college.edu", "+91 81234 56789", "Student registration, eligibility & scheduling"),
            TpcStaff("TPC4", "Mr. Amit Verma", "Senior Placement Officer", "amit.tpc@college.edu", "+91 70123 45678", "Technical training & company coordination")
        )
    )
    val tpcStaffList: StateFlow<List<TpcStaff>> = _tpcStaffList.asStateFlow()

    private val _departments = MutableStateFlow<List<Department>>(
        listOf(
            Department(
                id = "CSE-ET",
                name = "CSE (Emerging Technologies)",
                description = "Focuses on Advanced AI, IoT, Blockchain, and Machine Learning.",
                faculties = listOf(
                    Faculty("F1_CSEET", "Dr. Arshia Khan", "Professor & Head", "arshia.khan@college.edu", "Artificial Intelligence & ML"),
                    Faculty("F2_CSEET", "Mr. Rajesh Kurup", "Assistant Professor", "rajesh.kurup@college.edu", "Cyber Security & Forensic"),
                    Faculty("F3_CSEET", "Mrs. Megha Shrivastav", "Assistant Professor", "megha.s@college.edu", "Blockchain & Distributed Ledgers")
                )
            ),
            Department(
                id = "CSE",
                name = "Computer Science & Engineering",
                description = "Standard Computer Architecture, Algorithms, and Software Engineering methodologies.",
                faculties = listOf(
                    Faculty("F1_CSE", "Dr. V. Krishna", "Professor & HOD", "vkrishna@college.edu", "Algorithms & Databases"),
                    Faculty("F2_CSE", "Dr. Priya Nair", "Associate Professor", "priya.nair@college.edu", "Data Science & NLP"),
                    Faculty("F3_CSE", "Mr. John Wesley", "Assistant Professor", "john.wesley@college.edu", "Cloud Computing & DevOps")
                )
            ),
            Department(
                id = "ECE",
                name = "Electronics & Communication",
                description = "VLSI Systems, Signal Processing, Embedded Systems, and Wireless Communication.",
                faculties = listOf(
                    Faculty("F1_ECE", "Dr. R. G. Sastry", "Professor & HOD", "rgsastry@college.edu", "Digital Signal Processing"),
                    Faculty("F2_ECE", "Dr. Divya Bharathi", "Associate Professor", "divya.b@college.edu", "VLSI & Embedded IoT")
                )
            ),
            Department(
                id = "EEE",
                name = "Electrical & Electronics",
                description = "Core Power Grids, Electric Vehicles, Control Systems, and Power Electronics.",
                faculties = listOf(
                    Faculty("F1_EEE", "Dr. M. N. Rao", "Professor & HOD", "mnrao@college.edu", "High Voltage Power Grids"),
                    Faculty("F2_EEE", "Mr. K. Chaitanya", "Assistant Professor", "chaitanya.k@college.edu", "Renewable Energy & EV Tech")
                )
            ),
            Department(
                id = "CIVIL",
                name = "Civil Engineering",
                description = "Structural Design, Concrete Technology, Hydraulics, and Geotech Engineering.",
                faculties = listOf(
                    Faculty("F1_CIVIL", "Dr. H. S. Patel", "Professor & HOD", "hspatel@college.edu", "Structural Seismic Engineering"),
                    Faculty("F2_CIVIL", "Mrs. Renuka S.", "Assistant Professor", "renuka.s@college.edu", "Geotechnical Mechanics & Surveying")
                )
            ),
            Department(
                id = "MECH",
                name = "Mechanical Engineering",
                description = "Robotics, Thermal Engineering, Fluid Dynamics, and Manufacturing Sciences.",
                faculties = listOf(
                    Faculty("F1_MECH", "Dr. Sanjay Kumar", "Professor & HOD", "sanjay.kumar@college.edu", "Robotics, Nanophase Materials & CAD"),
                    Faculty("F2_MECH", "Mr. Vicky Anand", "Assistant Professor", "vicky.anand@college.edu", "Thermodynamics & Heat Transfer")
                )
            ),
            Department(
                id = "IT",
                name = "Information Technology",
                description = "Information Storage, Distributed Database Systems, and Network Administration.",
                faculties = listOf(
                    Faculty("F1_IT", "Dr. S. Mugunthan", "Professor & HOD", "mugunthan@college.edu", "Network Security & Cyber Defense"),
                    Faculty("F2_IT", "Mrs. Lakshmi Priya", "Assistant Professor", "lakshmi.priya@college.edu", "Mobile Application Development & Software QA")
                )
            )
        )
    )
    val departments: StateFlow<List<Department>> = _departments.asStateFlow()

    fun addDepartment(
        name: String,
        description: String,
        facName: String,
        facRole: String,
        facSpec: String,
        facEmail: String
    ) {
        val currentList = _departments.value.toMutableList()
        val newId = name.replace(" ", "_").uppercase()
        val initialFac = Faculty(
            id = "F1_$newId",
            name = facName.ifBlank { "Dr. Appointed Professor" },
            designation = facRole.ifBlank { "Professor & HOD" },
            specialization = facSpec.ifBlank { "Core Specialized Research" },
            email = facEmail.ifBlank { "hod.${newId.lowercase()}@college.edu" }
        )
        val newDept = Department(
            id = newId,
            name = name,
            description = description,
            faculties = listOf(initialFac)
        )
        currentList.add(newDept)
        _departments.value = currentList

        broadcastNotification(
            title = "New Department Incorporated",
            content = "The Department of $name has been formally set up in our directory. Click to explore core members and curriculum.",
            category = "General"
        )
    }

    fun addFacultyToDepartment(
        deptId: String,
        name: String,
        designation: String,
        email: String,
        specialization: String,
        phone: String = "+91 9988776655"
    ) {
        val currentList = _departments.value.map { dept ->
            if (dept.id == deptId) {
                val newFacultyId = "F${dept.faculties.size + 1}_${dept.id}"
                val newFaculty = Faculty(
                    id = newFacultyId,
                    name = name,
                    designation = designation,
                    email = email,
                    specialization = specialization,
                    phone = phone
                )
                dept.copy(faculties = dept.faculties + newFaculty)
            } else {
                dept
            }
        }
        _departments.value = currentList

        broadcastNotification(
            title = "New Faculty Join Announcement",
            content = "Welcome $name as $designation to the Department of $deptId.",
            category = "General"
        )
    }

    // --- TPO TRAINING & PLACEMENT CELL STATE & OPERATIONS ---
    private val _placementDrives = MutableStateFlow<List<PlacementDrive>>(
        listOf(
            PlacementDrive(
                id = 1,
                companyName = "Ranbidge Solutions Pvt Ltd",
                roleName = "Software Engineer Trainee",
                packageCTC = "12.0 LPA",
                eligibilityCGPA = 7.5,
                eligibleBranches = "CSE, ECE, EEE, IT",
                description = "Build cutting-edge full-stack solutions and Android mobile applications. Candidates must possess strong fundamentals in Kotlin, React, and Database design.",
                status = "Active",
                deadline = "June 30, 2026"
            ),
            PlacementDrive(
                id = 2,
                companyName = "Google India",
                roleName = "Associate Cloud Engineer",
                packageCTC = "24.5 LPA",
                eligibilityCGPA = 8.5,
                eligibleBranches = "CSE, ECE",
                description = "Incorporate scalable cloud infrastructure, deploy distributed systems, and design automated CI/CD microservices with Kubernetes & Google Cloud Platform.",
                status = "Active",
                deadline = "July 05, 2026"
            ),
            PlacementDrive(
                id = 3,
                companyName = "Tata Consultancy Services (TCS)",
                roleName = "Systems Engineer (Digital)",
                packageCTC = "7.2 LPA",
                eligibilityCGPA = 7.0,
                eligibleBranches = "All Branches",
                description = "Enterprise modernization using generative AI, security architecture, and high throughput backend APIs. Exciting technical rotation pathways.",
                status = "Active",
                deadline = "June 25, 2026"
            ),
            PlacementDrive(
                id = 4,
                companyName = "Infosys Ltd",
                roleName = "Power Programmer",
                packageCTC = "9.5 LPA",
                eligibilityCGPA = 7.2,
                eligibleBranches = "CSE, IT, ECE",
                description = "Advisory software consulting and high-scale enterprise engineering using Kotlin/Java, Spring Boot, and Cloud Native architecture.",
                status = "Closed",
                deadline = "June 10, 2026"
            )
        )
    )
    val placementDrives: StateFlow<List<PlacementDrive>> = _placementDrives.asStateFlow()

    private val _placementApplications = MutableStateFlow<List<PlacementApplication>>(
        listOf(
            PlacementApplication(
                id = 101,
                driveId = 1,
                companyName = "Ranbidge Solutions Pvt Ltd",
                roleName = "Software Engineer Trainee",
                studentId = "STU001",
                studentName = "Rahul Sharma",
                department = "Computer Science",
                cgpa = 8.8,
                resumeUrl = "Rahul_Resume_CSE.pdf",
                status = "Shortlisted",
                feedback = "Excellent coding challenge score. Shortlisted for interview round!"
            ),
            PlacementApplication(
                id = 102,
                driveId = 2,
                companyName = "Google India",
                roleName = "Associate Cloud Engineer",
                studentId = "STU002",
                studentName = "Sneha Reddy",
                department = "Electronics & Communication",
                cgpa = 9.2,
                resumeUrl = "Sneha_ECE_Resume.pdf",
                status = "Applied",
                feedback = "Profile currently under review by recruiter."
            ),
            PlacementApplication(
                id = 103,
                driveId = 1,
                companyName = "Ranbidge Solutions Pvt Ltd",
                roleName = "Software Engineer Trainee",
                studentId = "STU003",
                studentName = "Naveen Kumar",
                department = "Electrical Engineering",
                cgpa = 8.1,
                resumeUrl = "Naveen_EE_Resume.pdf",
                status = "Applied",
                feedback = "Received application details."
            )
        )
    )
    val placementApplications: StateFlow<List<PlacementApplication>> = _placementApplications.asStateFlow()

    // Save and track student profile CGPA
    private val _studentCgpa = MutableStateFlow<Map<String, Double>>(
        mapOf(
            "STU001" to 8.8,
            "STU002" to 9.2,
            "STU003" to 8.1
        )
    )
    val studentCgpa: StateFlow<Map<String, Double>> = _studentCgpa.asStateFlow()

    fun updateStudentCgpa(cgpa: Double) {
        val user = _currentUser.value ?: return
        val current = _studentCgpa.value.toMutableMap()
        current[user.userId] = cgpa
        _studentCgpa.value = current
    }

    fun postPlacementDrive(
        company: String,
        role: String,
        ctc: String,
        cgpa: Double,
        branches: String,
        desc: String,
        deadlineStr: String
    ) {
        val current = _placementDrives.value.toMutableList()
        val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
        current.add(
            PlacementDrive(
                id = newId,
                companyName = company,
                roleName = role,
                packageCTC = ctc,
                eligibilityCGPA = cgpa,
                eligibleBranches = branches,
                description = desc,
                status = "Active",
                deadline = deadlineStr
            )
        )
        _placementDrives.value = current

        // Broadcast a notification to all students
        broadcastNotification(
            title = "New Placement: $company",
            content = "Exciting opportunity to join $company as a $role! CTC: $ctc. Check out TPO services.",
            category = "General"
        )
    }

    fun applyToPlacementDrive(driveId: Int, resumeName: String) {
        val user = _currentUser.value ?: return
        val drive = _placementDrives.value.find { it.id == driveId } ?: return
        val current = _placementApplications.value.toMutableList()

        // Check if student already applied
        if (current.any { it.driveId == driveId && it.studentId == user.userId }) {
            return
        }

        val cgpa = _studentCgpa.value[user.userId] ?: 8.0
        val newId = (current.maxOfOrNull { it.id } ?: 100) + 1
        current.add(
            PlacementApplication(
                id = newId,
                driveId = driveId,
                companyName = drive.companyName,
                roleName = drive.roleName,
                studentId = user.userId,
                studentName = user.name,
                department = user.department,
                cgpa = cgpa,
                resumeUrl = resumeName,
                status = "Applied",
                feedback = "Application received successfully."
            )
        )
        _placementApplications.value = current

        // Create alert notification specifically for this student
        viewModelScope.launch {
            repository.createNotification(
                studentId = user.userId,
                title = "Applied: ${drive.companyName}",
                content = "Your application for ${drive.roleName} role has been transmitted to recruitment team.",
                category = "General"
            )
        }
    }

    fun updateApplicationStatus(applicationId: Int, newStatus: String, feedbackText: String) {
        val current = _placementApplications.value.toMutableList()
        val idx = current.indexOfFirst { it.id == applicationId }
        if (idx != -1) {
            val app = current[idx]
            val updated = app.copy(status = newStatus, feedback = feedbackText)
            current[idx] = updated
            _placementApplications.value = current

            // Notify student
            viewModelScope.launch {
                repository.createNotification(
                    studentId = app.studentId,
                    title = "Placement Alert: ${app.companyName}",
                    content = "Your application status for ${app.roleName} updated to: $newStatus. Check details.",
                    category = "General"
                )
            }
        }
    }
}

data class PlacementDrive(
    val id: Int,
    val companyName: String,
    val roleName: String,
    val packageCTC: String,
    val eligibilityCGPA: Double,
    val eligibleBranches: String,
    val description: String,
    val status: String,
    val deadline: String
)

data class PlacementApplication(
    val id: Int,
    val driveId: Int,
    val companyName: String,
    val roleName: String,
    val studentId: String,
    val studentName: String,
    val department: String,
    val cgpa: Double,
    val resumeUrl: String,
    val status: String,
    val feedback: String = ""
)

data class TpcStaff(
    val id: String,
    val name: String,
    val designation: String,
    val email: String,
    val phone: String,
    val responsibility: String
)

data class Faculty(
    val id: String,
    val name: String,
    val designation: String,
    val email: String,
    val specialization: String,
    val phone: String = "+91 9988776655"
)

data class Department(
    val id: String,
    val name: String,
    val description: String,
    val faculties: List<Faculty>
)

// Custom viewmodel provider factory
class PortalViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PortalViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
