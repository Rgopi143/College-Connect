package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.coroutines.resumeWithException

class AppRepository(private val appDao: AppDao) {

    // --- FIREBASE FIRESTORE SYNC SYSTEM ---
    private val firestoreInstance: com.google.firebase.firestore.FirebaseFirestore? by lazy {
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSync", "Firestore not available: ${e.message}")
            null
        }
    }

    private fun <T : Any> syncToFirestore(collectionName: String, documentId: String, data: T) {
        try {
            firestoreInstance?.collection(collectionName)?.document(documentId)?.set(data)
                ?.addOnSuccessListener {
                    android.util.Log.d("FirebaseSync", "Synced $documentId to collection $collectionName successfully!")
                }
                ?.addOnFailureListener { e ->
                    android.util.Log.e("FirebaseSync", "Failed to sync $documentId to collection $collectionName: ${e.message}")
                }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSync", "Firestore sync exception: ${e.message}")
        }
    }

    // --- USER PROFILE & LOGIN ---
    suspend fun getUser(userId: String): User? = appDao.getUserByIdOrEmail(userId)
    
    fun getAllUsers(): Flow<List<User>> = appDao.getAllUsers()

    suspend fun insertUser(user: User) {
        appDao.insertUser(user)
        syncToFirestore("users", user.userId, user)
    }
    
    suspend fun updateUser(user: User) {
        appDao.updateUser(user)
        syncToFirestore("users", user.userId, user)
    }

    suspend fun deleteUser(user: User) {
        appDao.deleteUser(user)
        // Optionally delete from firestore too
        try {
            firestoreInstance?.collection("users")?.document(user.userId)?.delete()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSync", "Failed to delete user from firestore: ${e.message}")
        }
    }

    // --- EVENTS WORKFLOW ---
    fun getAllEvents(): Flow<List<CollegeEvent>> = appDao.getAllEvents()

    suspend fun insertEvent(event: CollegeEvent) {
        appDao.insertEvent(event)
        syncToFirestore("events", event.id.toString(), event)
    }

    suspend fun updateEvent(event: CollegeEvent) {
        appDao.updateEvent(event)
        syncToFirestore("events", event.id.toString(), event)
    }

    suspend fun deleteEvent(event: CollegeEvent) {
        appDao.deleteEvent(event)
        try {
            firestoreInstance?.collection("events")?.document(event.id.toString())?.delete()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSync", "Failed to delete event from firestore: ${e.message}")
        }
    }

    // --- OUTPASS REQUESTS & WORKFLOW ---
    fun getAllOutpasses(): Flow<List<OutpassRequest>> = appDao.getAllOutpassRequests()
    
    fun getOutpassesForStudent(studentId: String): Flow<List<OutpassRequest>> = 
        appDao.getOutpassRequestsByStudent(studentId)

    suspend fun createOutpass(
        studentId: String,
        studentName: String,
        rollNumber: String,
        department: String,
        dateTime: String,
        reason: String,
        expectedReturnTime: String,
        parentContact: String
    ): OutpassRequest {
        val uniqueText = "OUTPASS-${UUID.randomUUID().toString().take(8).uppercase()}"
        val request = OutpassRequest(
            studentId = studentId,
            studentName = studentName,
            rollNumber = rollNumber,
            department = department,
            dateTime = dateTime,
            reason = reason,
            expectedReturnTime = expectedReturnTime,
            parentContact = parentContact,
            status = "PENDING_MENTOR", // Init status goes to Mentor first
            qrText = uniqueText
        )
        appDao.insertOutpassRequest(request)
        syncToFirestore("outpasses", "${request.studentId}_${request.timestamp}", request)
        
        // Let's add a self notification for tracing
        createNotification(
            studentId,
            "Outpass Request Submitted",
            "Your outpass request to leave campus on $dateTime has been submitted. Status: Pending Mentor approval.",
            "Outpass"
        )
        // Notify the Mentor
        createNotification(
            studentId = "mentor",
            title = "Outpass Clearance Pending",
            content = "Student $studentName ($rollNumber) has requested outpass clearance. Awaiting your approval.",
            category = "Outpass"
        )
        return request
    }

    suspend fun approveOutpass(request: OutpassRequest, approverRole: String) {
        val nextStatus = when (approverRole) {
            "CLASS_ADVISOR", "MENTOR" -> "PENDING_HOD"
            "HOD" -> "PENDING_SECURITY"
            "SECURITY" -> "APPROVED"
            "WARDEN", "ADMIN" -> "APPROVED"
            else -> request.status
        }
        val updatedRequest = request.copy(status = nextStatus)
        appDao.updateOutpassRequest(updatedRequest)
        syncToFirestore("outpasses", "${updatedRequest.studentId}_${updatedRequest.timestamp}", updatedRequest)

        // Notification corresponding to the transition
        val currentLabel = when (nextStatus) {
            "PENDING_HOD" -> "Mentor approved your outpass. Now status: Pending HOD."
            "PENDING_SECURITY" -> "HOD approved your outpass. Now status: Pending Security Gate Check."
            "APPROVED" -> "Your outpass request has been fully APPROVED by security/administration!"
            else -> "Updated status: $nextStatus"
        }
        createNotification(request.studentId, "Outpass Status Update", currentLabel, "Outpass")

        // Notify the next actor in the workflow
        when (nextStatus) {
            "PENDING_HOD" -> createNotification(
                studentId = "hod",
                title = "Outpass Approval Pending",
                content = "Outpass request for ${request.studentName} is cleared by Mentor. Awaiting HOD approval.",
                category = "Outpass"
            )
            "PENDING_SECURITY" -> createNotification(
                studentId = "security",
                title = "Gate Outpass Clearance Pending",
                content = "Outpass request for ${request.studentName} has been approved by HOD. Verify exit at gate.",
                category = "Outpass"
            )
        }
    }

    suspend fun rejectOutpass(request: OutpassRequest, approverRole: String, comments: String = "Rejected by $approverRole") {
        val updatedRequest = request.copy(status = "REJECTED")
        appDao.updateOutpassRequest(updatedRequest)
        syncToFirestore("outpasses", "${updatedRequest.studentId}_${updatedRequest.timestamp}", updatedRequest)
        createNotification(request.studentId, "Outpass REJECTED", "Your outpass request was rejected: $comments", "Outpass")
    }

    // --- CERTIFICATE REQUESTS ---
    fun getAllCertificateRequests(): Flow<List<CertificateRequest>> = appDao.getAllCertificateRequests()
    
    fun getCertificatesForStudent(studentId: String): Flow<List<CertificateRequest>> = 
        appDao.getCertificateRequestsByStudent(studentId)

    suspend fun createCertificate(
        studentId: String,
        studentName: String,
        rollNumber: String,
        department: String,
        certificateType: String,
        details: String
    ): CertificateRequest {
        val request = CertificateRequest(
            studentId = studentId,
            studentName = studentName,
            rollNumber = rollNumber,
            department = department,
            certificateType = certificateType,
            details = details,
            status = "PENDING_HOD" // Initiates at HOD level directly
        )
        appDao.insertCertificateRequest(request)
        syncToFirestore("certificates", "${request.studentId}_${request.timestamp}", request)
        createNotification(
            studentId = studentId,
            title = "Certificate Request Raised",
            content = "Requested: $certificateType. Sent to HOD for approval.",
            category = "Certificate"
        )
        // Notify HOD
        createNotification(
            studentId = "hod",
            title = "Certificate Approval Pending",
            content = "Student $studentName ($rollNumber) has requested $certificateType. Awaiting your approval.",
            category = "Certificate"
        )
        return request
    }

    suspend fun approveCertificate(request: CertificateRequest, approverRole: String) {
        val nextStatus = when (approverRole) {
            "CLASS_ADVISOR", "MENTOR" -> "PENDING_HOD"
            "HOD" -> "PENDING_PRINCIPAL"
            "PRINCIPAL" -> "PENDING_PA_PRINT"
            "PA" -> "APPROVED"
            "ADMIN" -> "APPROVED"
            else -> request.status
        }
        val updated = request.copy(status = nextStatus)
        appDao.updateCertificateRequest(updated)
        syncToFirestore("certificates", "${updated.studentId}_${updated.timestamp}", updated)
        
        val contentText = when (nextStatus) {
            "PENDING_HOD" -> "Your request is submitted. Awaiting HOD approval."
            "PENDING_PRINCIPAL" -> "Your request has been forwarded by HOD. Now awaiting final sanction from Principal."
            "PENDING_PA_PRINT" -> "Your request has been approved by the Principal. Now forwarding to PA for printing."
            "APPROVED" -> "Your ${request.certificateType} request has been printed and is ready for collection!"
            else -> "Updated status: $nextStatus"
        }
        
        createNotification(
            studentId = request.studentId,
            title = "Certificate Status Update",
            content = contentText,
            category = "Certificate"
        )

        // Notify the next actor in the workflow
        when (nextStatus) {
            "PENDING_PRINCIPAL" -> createNotification(
                studentId = "principal",
                title = "Certificate Sanction Pending",
                content = "Certificate request (${request.certificateType}) for ${request.studentName} is forwarded by HOD. Awaiting Principal sanction.",
                category = "Certificate"
            )
            "PENDING_PA_PRINT" -> createNotification(
                studentId = "pa",
                title = "Certificate Printing Pending",
                content = "Certificate request (${request.certificateType}) for ${request.studentName} has been sanctioned by Principal. Please print.",
                category = "Certificate"
            )
        }
    }

    suspend fun rejectCertificate(request: CertificateRequest, approverRole: String) {
        val updated = request.copy(status = "REJECTED")
        appDao.updateCertificateRequest(updated)
        syncToFirestore("certificates", "${updated.studentId}_${updated.timestamp}", updated)
        createNotification(
            studentId = request.studentId,
            title = "Certificate Request REJECTED",
            content = "Your request for ${request.certificateType} was rejected by $approverRole. Please contact Department Administration.",
            category = "Certificate"
        )
    }

    // --- STATIONERY ---
    fun getAllStationeryItems(): Flow<List<StationeryItem>> = appDao.getAllStationeryItems()
    
    suspend fun updateStationeryItem(item: StationeryItem) {
        appDao.updateStationeryItem(item)
        syncToFirestore("stationery_items", item.id, item)
    }

    fun getAllStationeryRequests(): Flow<List<StationeryRequest>> = appDao.getAllStationeryRequests()
    
    fun getStationeryRequestsForStudent(studentId: String): Flow<List<StationeryRequest>> = 
        appDao.getStationeryRequestsByStudent(studentId)

    suspend fun placeStationeryOrder(
        studentId: String,
        studentName: String,
        itemId: String,
        itemName: String,
        quantity: Int,
        pricePerUnit: Double
    ): Boolean {
        val item = appDao.getStationeryItemById(itemId)
        if (item == null || item.stock < quantity) return false

        // Deduct stock
        val updatedItem = item.copy(stock = item.stock - quantity)
        updateStationeryItem(updatedItem)

        // Create Request
        val request = StationeryRequest(
            studentId = studentId,
            studentName = studentName,
            itemName = itemName,
            quantity = quantity,
            status = "PENDING",
            totalCost = pricePerUnit * quantity
        )
        appDao.insertStationeryRequest(request)
        syncToFirestore("stationery_requests", "${request.studentId}_${request.timestamp}", request)

        createNotification(
            studentId,
            "Stationery Order Received",
            "Placed order for $quantity x $itemName. Total price: \u20B9${pricePerUnit * quantity}. Collection token is active.",
            "Store"
        )
        return true
    }

    suspend fun completeStationeryRequest(request: StationeryRequest, newStatus: String) {
        val updated = request.copy(status = newStatus)
        appDao.updateStationeryRequest(updated)
        syncToFirestore("stationery_requests", "${updated.studentId}_${updated.timestamp}", updated)
        val detail = if (newStatus == "READY_FOR_COLLECTION") {
            "Your order of ${request.quantity}x ${request.itemName} is ready for pickup at the College Store!"
        } else {
            "You have collected your stationery order. Transaction complete."
        }
        createNotification(request.studentId, "Stationery Order Update", detail, "Store")
    }

    // --- PRINTING ---
    fun getAllPrintRequests(): Flow<List<PrintRequest>> = appDao.getAllPrintRequests()
    
    fun getPrintRequestsForStudent(studentId: String): Flow<List<PrintRequest>> = 
        appDao.getPrintRequestsByStudent(studentId)

    suspend fun createPrintRequest(
        studentId: String,
        studentName: String,
        fileName: String,
        pagesCount: Int,
        printType: String,
        copyType: String,
        bindingType: String
    ): PrintRequest {
        val ratePerPage = if (printType == "Color") 10.0 else 2.0
        val bindingRate = if (bindingType == "Spiral Binding") 40.0 else 0.0
        val cost = (pagesCount * ratePerPage) + bindingRate
        
        val request = PrintRequest(
            studentId = studentId,
            studentName = studentName,
            fileName = fileName,
            pagesCount = pagesCount,
            printType = printType,
            copyType = copyType,
            bindingType = bindingType,
            totalCost = cost,
            status = "QUEUED"
        )
        appDao.insertPrintRequest(request)
        syncToFirestore("print_requests", "${request.studentId}_${request.timestamp}", request)
        createNotification(
            studentId,
            "Print Project Submitted",
            "Uploaded $fileName ($pagesCount pages, $printType). Total: \u20B9$cost. Track progress in Printout Services.",
            "Print"
        )
        return request
    }

    suspend fun updatePrintStatus(request: PrintRequest, newStatus: String) {
        val updated = request.copy(status = newStatus)
        appDao.updatePrintRequest(updated)
        syncToFirestore("print_requests", "${updated.studentId}_${updated.timestamp}", updated)
        val alert = when (newStatus) {
            "PRINTING" -> "Your document ${request.fileName} is being printed now."
            "READY" -> "Your printout ${request.fileName} of ${request.pagesCount} pages is ready! Please collect at the Resource Center."
            "COMPLETED" -> "Printout collected. Receipt generated."
            else -> "Print job status: $newStatus"
        }
        createNotification(request.studentId, "Print Resource Update", alert, "Print")
    }

    // --- CANTEEN TOKEN BOOKING ---
    fun getAllCanteenItems(): Flow<List<CanteenItem>> = appDao.getAllCanteenItems()
    
    suspend fun insertCanteenItem(item: CanteenItem) {
        appDao.insertCanteenItem(item)
        syncToFirestore("canteen_items", item.id.toString(), item)
    }
    
    suspend fun updateCanteenItem(item: CanteenItem) {
        appDao.updateCanteenItem(item)
        syncToFirestore("canteen_items", item.id.toString(), item)
    }
    
    suspend fun deleteCanteenItem(item: CanteenItem) {
        appDao.deleteCanteenItem(item)
        try {
            firestoreInstance?.collection("canteen_items")?.document(item.id.toString())?.delete()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSync", "Failed to delete canteen item from firestore: ${e.message}")
        }
    }

    fun getAllCanteenBookings(): Flow<List<CanteenBooking>> = appDao.getAllCanteenBookings()
    
    fun getCanteenBookingsForStudent(studentId: String): Flow<List<CanteenBooking>> = 
        appDao.getCanteenBookingsByStudent(studentId)

    suspend fun placeCanteenBooking(
        studentId: String,
        studentName: String,
        itemsSummary: String,
        totalCost: Double
    ): CanteenBooking {
        val token = "CANTEEN-${UUID.randomUUID().toString().take(6).uppercase()}"
        val booking = CanteenBooking(
            studentId = studentId,
            studentName = studentName,
            itemsJson = itemsSummary,
            totalCost = totalCost,
            status = "BOOKED",
            qrToken = token
        )
        appDao.insertCanteenBooking(booking)
        syncToFirestore("canteen_bookings", "${booking.studentId}_${booking.timestamp}", booking)
        createNotification(
            studentId,
            "Canteen Token Booked",
            "Pre-booked $itemsSummary. Total: \u20B9$totalCost. Show QR token at counter to collect.",
            "Canteen"
        )
        return booking
    }

    suspend fun completeCanteenBooking(booking: CanteenBooking, newStatus: String) {
        val updated = booking.copy(status = newStatus)
        appDao.updateCanteenBooking(updated)
        syncToFirestore("canteen_bookings", "${updated.studentId}_${updated.timestamp}", updated)
        val notice = if (newStatus == "READY_FOR_COLLECTION") {
            "Your canteen meal order is ready! Show your QR code token to collect."
        } else {
            "Meal collected! Enjoy your food."
        }
        createNotification(booking.studentId, "Canteen Order Delivery", notice, "Canteen")
    }

    // --- NOTIFICATIONS & UTILITY ---
    fun getNotificationsForStudent(studentId: String): Flow<List<CollegeNotification>> = 
        appDao.getNotificationsForStudent(studentId)

    suspend fun createNotification(studentId: String, title: String, content: String, category: String) {
        val notif = CollegeNotification(
            targetStudentId = studentId,
            title = title,
            content = content,
            category = category
        )
        appDao.insertNotification(notif)
        syncToFirestore("notifications", "${studentId}_${notif.timestamp}", notif)
    }

    suspend fun doesNotificationExist(title: String, content: String, timestamp: Long): Boolean =
        appDao.doesNotificationExist(title, content, timestamp)

    suspend fun insertRawNotification(notif: CollegeNotification) {
        appDao.insertNotification(notif)
    }

    suspend fun markAllNotificationsAsRead(studentId: String) = appDao.markAllAsRead(studentId)

    suspend fun markNotificationAsRead(id: Int) = appDao.markAsRead(id)

    // --- SECURITY DOOR HISTORY & CHAT INTEGRATION ---
    suspend fun verifyOutpassExit(request: OutpassRequest) {
        val updatedRequest = request.copy(status = "APPROVED")
        appDao.updateOutpassRequest(updatedRequest)
        syncToFirestore("outpasses", "${updatedRequest.studentId}_${updatedRequest.timestamp}", updatedRequest)
        createNotification(request.studentId, "Outpass Exit Verified", "Your exit gate-pass has been successfully verified by security.", "Outpass")
    }

    fun getAllChatMessages(): Flow<List<ChatMessage>> = appDao.getAllChatMessages()

    suspend fun sendChatMessage(message: ChatMessage) {
        appDao.insertChatMessage(message)
        syncToFirestore("chat_messages", "${message.senderId}_${message.timestamp}", message)
    }

    suspend fun insertRawChatMessage(message: ChatMessage) {
        appDao.insertChatMessage(message)
    }

    // --- FIRESTORE PULL AND CLEAN CACHE WORKFLOW ---
    
    suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result, null)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Task failed"))
            }
        }
    }

    private fun documentToEvent(doc: com.google.firebase.firestore.DocumentSnapshot): CollegeEvent {
        return CollegeEvent(
            id = doc.getLong("id")?.toInt() ?: 0,
            title = doc.getString("title") ?: "",
            description = doc.getString("description") ?: "",
            date = doc.getString("date") ?: "",
            time = doc.getString("time") ?: "",
            venue = doc.getString("venue") ?: "",
            organizerRole = doc.getString("organizerRole") ?: "",
            filterDepartment = doc.getString("filterDepartment") ?: "ALL",
            isPaused = doc.getBoolean("isPaused") ?: false,
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        )
    }

    private fun documentToNotification(doc: com.google.firebase.firestore.DocumentSnapshot): CollegeNotification {
        return CollegeNotification(
            id = doc.getLong("id")?.toInt() ?: 0,
            targetStudentId = doc.getString("targetStudentId") ?: "",
            title = doc.getString("title") ?: "",
            content = doc.getString("content") ?: "",
            category = doc.getString("category") ?: "General",
            isRead = doc.getBoolean("isRead") ?: false,
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        )
    }

    private fun documentToOutpassRequest(doc: com.google.firebase.firestore.DocumentSnapshot): OutpassRequest {
        return OutpassRequest(
            id = doc.getLong("id")?.toInt() ?: 0,
            studentId = doc.getString("studentId") ?: "",
            studentName = doc.getString("studentName") ?: "",
            rollNumber = doc.getString("rollNumber") ?: "",
            department = doc.getString("department") ?: "",
            dateTime = doc.getString("dateTime") ?: "",
            reason = doc.getString("reason") ?: "",
            expectedReturnTime = doc.getString("expectedReturnTime") ?: "",
            parentContact = doc.getString("parentContact") ?: "",
            status = doc.getString("status") ?: "",
            qrText = doc.getString("qrText") ?: "",
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        )
    }

    private fun documentToCertificateRequest(doc: com.google.firebase.firestore.DocumentSnapshot): CertificateRequest {
        return CertificateRequest(
            id = doc.getLong("id")?.toInt() ?: 0,
            studentId = doc.getString("studentId") ?: "",
            studentName = doc.getString("studentName") ?: "",
            rollNumber = doc.getString("rollNumber") ?: "",
            department = doc.getString("department") ?: "",
            certificateType = doc.getString("certificateType") ?: "",
            details = doc.getString("details") ?: "",
            status = doc.getString("status") ?: "",
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        )
    }

    private fun documentToCanteenBooking(doc: com.google.firebase.firestore.DocumentSnapshot): CanteenBooking {
        return CanteenBooking(
            id = doc.getLong("id")?.toInt() ?: 0,
            studentId = doc.getString("studentId") ?: "",
            studentName = doc.getString("studentName") ?: "",
            itemsJson = doc.getString("itemsJson") ?: "",
            totalCost = doc.getDouble("totalCost") ?: 0.0,
            status = doc.getString("status") ?: "",
            qrToken = doc.getString("qrToken") ?: "",
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        )
    }

    private fun documentToStationeryRequest(doc: com.google.firebase.firestore.DocumentSnapshot): StationeryRequest {
        return StationeryRequest(
            id = doc.getLong("id")?.toInt() ?: 0,
            studentId = doc.getString("studentId") ?: "",
            studentName = doc.getString("studentName") ?: "",
            itemName = doc.getString("itemName") ?: "",
            quantity = doc.getLong("quantity")?.toInt() ?: 0,
            status = doc.getString("status") ?: "",
            totalCost = doc.getDouble("totalCost") ?: 0.0,
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        )
    }

    private fun documentToPrintRequest(doc: com.google.firebase.firestore.DocumentSnapshot): PrintRequest {
        return PrintRequest(
            id = doc.getLong("id")?.toInt() ?: 0,
            studentId = doc.getString("studentId") ?: "",
            studentName = doc.getString("studentName") ?: "",
            fileName = doc.getString("fileName") ?: "",
            pagesCount = doc.getLong("pagesCount")?.toInt() ?: 0,
            printType = doc.getString("printType") ?: "",
            copyType = doc.getString("copyType") ?: "",
            bindingType = doc.getString("bindingType") ?: "",
            totalCost = doc.getDouble("totalCost") ?: 0.0,
            status = doc.getString("status") ?: "",
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        )
    }

    private fun documentToChatMessage(doc: com.google.firebase.firestore.DocumentSnapshot): ChatMessage {
        return ChatMessage(
            id = doc.getLong("id")?.toInt() ?: 0,
            senderId = doc.getString("senderId") ?: "",
            senderName = doc.getString("senderName") ?: "",
            senderRole = doc.getString("senderRole") ?: "",
            recipientRole = doc.getString("recipientRole") ?: "",
            messageText = doc.getString("messageText") ?: "",
            isSheetAttachment = doc.getBoolean("isSheetAttachment") ?: false,
            attachmentData = doc.getString("attachmentData"),
            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        )
    }

    suspend fun pullFromFirestoreAndCleanCache(currentUserId: String): String {
        val firestore = firestoreInstance ?: return "Firestore not available"
        
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val startOfTodayMillis = try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(todayStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
        
        try {
            // 1. Sync Events
            val eventsSnap = firestore.collection("events").get().awaitTask()
            val eventsList = eventsSnap.documents.map { documentToEvent(it) }
                .filter { it.date >= todayStr } // Filter: upcoming / present
            appDao.clearEvents()
            eventsList.forEach { appDao.insertEvent(it) }

            // 2. Sync Notifications
            val notificationsSnap = firestore.collection("notifications").get().awaitTask()
            val notificationsList = notificationsSnap.documents.map { documentToNotification(it) }
                .filter { it.timestamp >= startOfTodayMillis } // Filter: upcoming / present
            appDao.clearNotifications()
            notificationsList.forEach { appDao.insertNotification(it) }

            // 3. Sync Outpasses
            val user = appDao.getUserById(currentUserId)
            if (user?.role == "PRINCIPAL") {
                appDao.clearOutpassRequests()
            } else {
                val outpassesSnap = firestore.collection("outpasses").get().awaitTask()
                val outpassesList = outpassesSnap.documents.map { documentToOutpassRequest(it) }
                    .filter { 
                        it.timestamp >= startOfTodayMillis || 
                        it.status.startsWith("PENDING") || 
                        it.dateTime >= todayStr
                    }
                appDao.clearOutpassRequests()
                outpassesList.forEach { appDao.insertOutpassRequest(it) }
            }

            // 4. Sync Certificates
            val certsSnap = firestore.collection("certificates").get().awaitTask()
            val certsList = certsSnap.documents.map { documentToCertificateRequest(it) }
                .filter { it.timestamp >= startOfTodayMillis || it.status.startsWith("PENDING") }
            appDao.clearCertificateRequests()
            certsList.forEach { appDao.insertCertificateRequest(it) }

            // 5. Sync Canteen Bookings
            val canteenSnap = firestore.collection("canteen_bookings").get().awaitTask()
            val canteenList = canteenSnap.documents.map { documentToCanteenBooking(it) }
                .filter { it.timestamp >= startOfTodayMillis || it.status != "COMPLETED" }
            appDao.clearCanteenBookings()
            canteenList.forEach { appDao.insertCanteenBooking(it) }

            // 6. Sync Stationery Requests
            val stationerySnap = firestore.collection("stationery_requests").get().awaitTask()
            val stationeryList = stationerySnap.documents.map { documentToStationeryRequest(it) }
                .filter { it.timestamp >= startOfTodayMillis || it.status != "COLLECTED" }
            appDao.clearStationeryRequests()
            stationeryList.forEach { appDao.insertStationeryRequest(it) }

            // 7. Sync Print Requests
            val printsSnap = firestore.collection("print_requests").get().awaitTask()
            val printsList = printsSnap.documents.map { documentToPrintRequest(it) }
                .filter { it.timestamp >= startOfTodayMillis || it.status != "COMPLETED" }
            appDao.clearPrintRequests()
            printsList.forEach { appDao.insertPrintRequest(it) }

            // 8. Sync Chat Messages
            val chatSnap = firestore.collection("chat_messages").get().awaitTask()
            val chatList = chatSnap.documents.map { documentToChatMessage(it) }
                .filter { it.timestamp >= (System.currentTimeMillis() - 24 * 60 * 60 * 1000) }
            appDao.clearChatMessages()
            chatList.forEach { appDao.insertChatMessage(it) }

            return "SUCCESS"
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSync", "Failed to pull/clean database cache: ${e.message}")
            return "ERROR: ${e.message}"
        }
    }

    private fun documentToUser(doc: com.google.firebase.firestore.DocumentSnapshot): User {
        return User(
            userId = doc.getString("userId") ?: "",
            name = doc.getString("name") ?: "",
            rollNumber = doc.getString("rollNumber") ?: "",
            department = doc.getString("department") ?: "",
            email = doc.getString("email") ?: "",
            phone = doc.getString("phone") ?: "",
            parentContact = doc.getString("parentContact") ?: "",
            role = doc.getString("role") ?: "STUDENT",
            password = doc.getString("password") ?: "pass",
            isLoggedIn = doc.getBoolean("isLoggedIn") ?: false,
            isPaused = doc.getBoolean("isPaused") ?: false,
            assignedMentorId = doc.getString("assignedMentorId"),
            assignedMentorName = doc.getString("assignedMentorName"),
            assignedAdvisorId = doc.getString("assignedAdvisorId"),
            assignedAdvisorName = doc.getString("assignedAdvisorName")
        )
    }

    suspend fun getUserFromFirestore(userIdOrEmail: String): User? = kotlinx.coroutines.withTimeoutOrNull(3000) {
        val firestore = firestoreInstance ?: return@withTimeoutOrNull null
        try {
            // 1. Try to get document by userId
            val docSnap = firestore.collection("users").document(userIdOrEmail).get().awaitTask()
            if (docSnap.exists()) {
                return@withTimeoutOrNull documentToUser(docSnap)
            }
            // 2. Try to query by email
            val querySnap = firestore.collection("users")
                .whereEqualTo("email", userIdOrEmail)
                .limit(1)
                .get()
                .awaitTask()
            if (!querySnap.isEmpty) {
                return@withTimeoutOrNull documentToUser(querySnap.documents[0])
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSync", "Failed to fetch user from firestore: ${e.message}")
        }
        null
    }

    // --- BULK FIRESTORE COMPREHENSIVE PUSH ---
    fun forceBulkSyncToCloud(
        allUsersList: List<User>,
        allOutpassesList: List<OutpassRequest>,
        allCertificatesList: List<CertificateRequest>,
        allStationeryList: List<StationeryRequest>,
        allPrintList: List<PrintRequest>,
        allCanteenList: List<CanteenBooking>,
        allStationeryItemsList: List<StationeryItem>,
        allCanteenItemsList: List<CanteenItem>,
        allChatMessagesList: List<ChatMessage>,
        allEventsList: List<CollegeEvent>,
        allNotificationsList: List<CollegeNotification>
    ) {
        allUsersList.forEach { syncToFirestore("users", it.userId, it) }
        allOutpassesList.forEach { syncToFirestore("outpasses", "${it.studentId}_${it.timestamp}", it) }
        allCertificatesList.forEach { syncToFirestore("certificates", "${it.studentId}_${it.timestamp}", it) }
        allStationeryList.forEach { syncToFirestore("stationery_requests", "${it.studentId}_${it.timestamp}", it) }
        allPrintList.forEach { syncToFirestore("print_requests", "${it.studentId}_${it.timestamp}", it) }
        allCanteenList.forEach { syncToFirestore("canteen_bookings", "${it.studentId}_${it.timestamp}", it) }
        allStationeryItemsList.forEach { syncToFirestore("stationery_items", it.id, it) }
        allCanteenItemsList.forEach { syncToFirestore("canteen_items", it.id.toString(), it) }
        allChatMessagesList.forEach { syncToFirestore("chat_messages", "${it.senderId}_${it.timestamp}", it) }
        allEventsList.forEach { syncToFirestore("events", it.id.toString(), it) }
        allNotificationsList.forEach { syncToFirestore("notifications", "${it.targetStudentId}_${it.timestamp}", it) }
    }
}
