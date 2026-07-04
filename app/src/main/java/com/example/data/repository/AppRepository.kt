package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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
            status = "PENDING_ADVISOR", // Init status
            qrText = uniqueText
        )
        appDao.insertOutpassRequest(request)
        syncToFirestore("outpasses", "${request.studentId}_${request.timestamp}", request)
        
        // Let's add a self notification for tracing
        createNotification(
            studentId,
            "Outpass Request Submitted",
            "Your outpass request to leave campus on $dateTime in CS dept has been submitted. Status: Pending Advisor approval.",
            "Outpass"
        )
        return request
    }

    suspend fun approveOutpass(request: OutpassRequest, approverRole: String) {
        val nextStatus = when (approverRole) {
            "CLASS_ADVISOR", "MENTOR" -> "PENDING_HOD"
            "HOD", "WARDEN", "PRINCIPAL" -> "APPROVED"
            else -> request.status
        }
        val updatedRequest = if (nextStatus == "APPROVED") {
            request.copy(status = nextStatus, timestamp = System.currentTimeMillis())
        } else {
            request.copy(status = nextStatus)
        }
        appDao.updateOutpassRequest(updatedRequest)
        syncToFirestore("outpasses", "${updatedRequest.studentId}_${updatedRequest.timestamp}", updatedRequest)

        // Notification corresponding to the transition
        val currentLabel = when (approverRole) {
            "CLASS_ADVISOR", "MENTOR" -> "Mentor/Class Advisor approved your outpass. Now status: Pending HOD."
            "HOD" -> "HOD APPROVED your outpass! QR Code Gate Pass is generated."
            "WARDEN", "PRINCIPAL" -> "Warden/Principal APPROVED your outpass! QR Code Gate Pass is generated."
            else -> "Updated status: $nextStatus"
        }
        createNotification(request.studentId, "Outpass Status Update", currentLabel, "Outpass")
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
            status = "PENDING_MENTOR"
        )
        appDao.insertCertificateRequest(request)
        syncToFirestore("certificates", "${request.studentId}_${request.timestamp}", request)
        createNotification(
            studentId = studentId,
            title = "Certificate Request Raised",
            content = "Requested: $certificateType. Track status in Certificate Services.",
            category = "Certificate"
        )
        return request
    }

    suspend fun approveCertificate(request: CertificateRequest, approverRole: String) {
        val nextStatus = when (approverRole) {
            "CLASS_ADVISOR", "MENTOR" -> "PENDING_HOD"
            "HOD" -> "PENDING_PRINCIPAL" // Flow from mentor -> hod -> principal for certificate requests
            "PRINCIPAL" -> "APPROVED"
            "ADMIN" -> "APPROVED"
            else -> request.status
        }
        val updated = if (nextStatus == "APPROVED") {
            request.copy(status = nextStatus, timestamp = System.currentTimeMillis())
        } else {
            request.copy(status = nextStatus)
        }
        appDao.updateCertificateRequest(updated)
        syncToFirestore("certificates", "${updated.studentId}_${updated.timestamp}", updated)
        
        val contentText = when (nextStatus) {
            "PENDING_HOD" -> "Your request has been forwarded by Mentor. Awaiting HOD approval."
            "PENDING_PRINCIPAL" -> "Your request has been forwarded by HOD. Now awaiting final sanction from Principal."
            "APPROVED" -> "Your ${request.certificateType} request has been fully APPROVED by the Principal!"
            else -> "Updated status: $nextStatus"
        }
        
        createNotification(
            studentId = request.studentId,
            title = "Certificate Request Forwarded",
            content = contentText,
            category = "Certificate"
        )
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
        val updatedRequest = request.copy(status = "EXITED")
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
