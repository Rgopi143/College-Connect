package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- USER ---
    @Query("SELECT * FROM users WHERE LOWER(userId) = LOWER(:userId) LIMIT 1")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE LOWER(userId) = LOWER(:identifier) OR LOWER(email) = LOWER(:identifier) LIMIT 1")
    suspend fun getUserByIdOrEmail(identifier: String): User?

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: String): Flow<List<User>>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUserById(userId: String)

    @Delete
    suspend fun deleteUser(user: User)

    // --- OUTPASS REQUESTS ---
    @Query("SELECT * FROM outpass_requests ORDER BY timestamp DESC")
    fun getAllOutpassRequests(): Flow<List<OutpassRequest>>

    @Query("SELECT * FROM outpass_requests WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getOutpassRequestsByStudent(studentId: String): Flow<List<OutpassRequest>>

    @Query("SELECT * FROM outpass_requests WHERE status = :status ORDER BY timestamp DESC")
    fun getOutpassRequestsByStatus(status: String): Flow<List<OutpassRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutpassRequest(request: OutpassRequest)

    @Update
    suspend fun updateOutpassRequest(request: OutpassRequest)

    // --- CERTIFICATE REQUESTS ---
    @Query("SELECT * FROM certificate_requests ORDER BY timestamp DESC")
    fun getAllCertificateRequests(): Flow<List<CertificateRequest>>

    @Query("SELECT * FROM certificate_requests WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getCertificateRequestsByStudent(studentId: String): Flow<List<CertificateRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificateRequest(request: CertificateRequest)

    @Update
    suspend fun updateCertificateRequest(request: CertificateRequest)

    // --- STATIONERY ITEMS ---
    @Query("SELECT * FROM stationery_items")
    fun getAllStationeryItems(): Flow<List<StationeryItem>>

    @Query("SELECT * FROM stationery_items WHERE id = :id LIMIT 1")
    suspend fun getStationeryItemById(id: String): StationeryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStationeryItem(item: StationeryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStationeryItems(items: List<StationeryItem>)

    @Update
    suspend fun updateStationeryItem(item: StationeryItem)

    // --- STATIONERY REQUESTS ---
    @Query("SELECT * FROM stationery_requests ORDER BY timestamp DESC")
    fun getAllStationeryRequests(): Flow<List<StationeryRequest>>

    @Query("SELECT * FROM stationery_requests WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getStationeryRequestsByStudent(studentId: String): Flow<List<StationeryRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStationeryRequest(request: StationeryRequest)

    @Update
    suspend fun updateStationeryRequest(request: StationeryRequest)

    // --- PRINT REQUESTS ---
    @Query("SELECT * FROM print_requests ORDER BY timestamp DESC")
    fun getAllPrintRequests(): Flow<List<PrintRequest>>

    @Query("SELECT * FROM print_requests WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getPrintRequestsByStudent(studentId: String): Flow<List<PrintRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrintRequest(request: PrintRequest)

    @Update
    suspend fun updatePrintRequest(request: PrintRequest)

    // --- CANTEEN ITEMS ---
    @Query("SELECT * FROM canteen_items")
    fun getAllCanteenItems(): Flow<List<CanteenItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanteenItem(item: CanteenItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanteenItems(items: List<CanteenItem>)

    @Update
    suspend fun updateCanteenItem(item: CanteenItem)

    @Delete
    suspend fun deleteCanteenItem(item: CanteenItem)

    // --- CANTEEN BOOKINGS ---
    @Query("SELECT * FROM canteen_bookings ORDER BY timestamp DESC")
    fun getAllCanteenBookings(): Flow<List<CanteenBooking>>

    @Query("SELECT * FROM canteen_bookings WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getCanteenBookingsByStudent(studentId: String): Flow<List<CanteenBooking>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanteenBooking(booking: CanteenBooking)

    @Update
    suspend fun updateCanteenBooking(booking: CanteenBooking)

    // --- NOTIFICATIONS ---
    @Query("SELECT * FROM notifications WHERE targetStudentId = 'ALL' OR targetStudentId = :studentId ORDER BY timestamp DESC")
    fun getNotificationsForStudent(studentId: String): Flow<List<CollegeNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: CollegeNotification)

    @Query("SELECT EXISTS(SELECT 1 FROM notifications WHERE title = :title AND content = :content AND timestamp = :timestamp)")
    suspend fun doesNotificationExist(title: String, content: String, timestamp: Long): Boolean

    @Query("UPDATE notifications SET isRead = 1 WHERE targetStudentId = 'ALL' OR targetStudentId = :studentId")
    suspend fun markAllAsRead(studentId: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    // --- CHAT MESSAGES ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    // --- EVENTS ---
    @Query("SELECT * FROM college_events ORDER BY date ASC, time ASC")
    fun getAllEvents(): Flow<List<CollegeEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CollegeEvent)

    @Update
    suspend fun updateEvent(event: CollegeEvent)

    @Delete
    suspend fun deleteEvent(event: CollegeEvent)

    // --- BULK CLEAR CACHE QUERIES ---
    @Query("DELETE FROM outpass_requests")
    suspend fun clearOutpassRequests()

    @Query("DELETE FROM certificate_requests")
    suspend fun clearCertificateRequests()

    @Query("DELETE FROM stationery_requests")
    suspend fun clearStationeryRequests()

    @Query("DELETE FROM print_requests")
    suspend fun clearPrintRequests()

    @Query("DELETE FROM canteen_bookings")
    suspend fun clearCanteenBookings()

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()

    @Query("DELETE FROM college_events")
    suspend fun clearEvents()

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()
}
