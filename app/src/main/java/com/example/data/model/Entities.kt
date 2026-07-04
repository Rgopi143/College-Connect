package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String, // String ID/Roll Number
    val name: String,
    val rollNumber: String,
    val department: String,
    val email: String,
    val phone: String,
    val parentContact: String,
    val role: String, // STUDENT, CLASS_ADVISOR, HOD, PRINCIPAL, WARDEN, ADMIN
    val password: String = "pass",
    val isLoggedIn: Boolean = false,
    val isPaused: Boolean = false,
    val assignedMentorId: String? = null,
    val assignedMentorName: String? = null,
    val assignedAdvisorId: String? = null,
    val assignedAdvisorName: String? = null
)

@Entity(tableName = "outpass_requests")
data class OutpassRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val rollNumber: String,
    val department: String,
    val dateTime: String,
    val reason: String,
    val expectedReturnTime: String,
    val parentContact: String,
    val status: String, // PENDING_ADVISOR, PENDING_HOD, PENDING_WARDEN, APPROVED, REJECTED
    val qrText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "certificate_requests")
data class CertificateRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val rollNumber: String,
    val department: String,
    val certificateType: String, // Bonafide Certificate, Study Certificate, Transfer Certificate, Conduct Certificate, Internship Letter, NOC, Fee Structure
    val details: String, // Purpose, academic year, etc.
    val status: String, // PENDING, APPROVED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stationery_items")
data class StationeryItem(
    @PrimaryKey val id: String, // unique key e.g. "pens", "pencils", etc.
    val name: String,
    val stock: Int,
    val category: String, // Writing, Notebooks, Laboratory, Folders
    val price: Double
)

@Entity(tableName = "stationery_requests")
data class StationeryRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val itemName: String,
    val quantity: Int,
    val status: String, // PENDING, READY_FOR_COLLECTION, COLLECTED
    val totalCost: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "print_requests")
data class PrintRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val fileName: String,
    val pagesCount: Int,
    val printType: String, // Black & White, Color
    val copyType: String, // Normal, Xerox, Project Report
    val bindingType: String, // None, Spiral Binding
    val totalCost: Double,
    val status: String, // QUEUED, PRINTING, READY, COMPLETED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "canteen_items")
data class CanteenItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val category: String, // Breakfast, Lunch, Snacks, Dinner, Beverages
    val isAvailable: Boolean = true
)

@Entity(tableName = "canteen_bookings")
data class CanteenBooking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val itemsJson: String, // comma-separated items like "Idli x2, Tea x1"
    val totalCost: Double,
    val status: String, // BOOKED, READY_FOR_COLLECTION, COMPLETED
    val qrToken: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class CollegeNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val targetStudentId: String = "", // "ALL" or specific student's ID/Roll Number
    val title: String = "",
    val content: String = "",
    val category: String = "General", // General, Outpass, Certificate, Canteen, Store, Print
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val recipientRole: String = "", // HOD or PRINCIPAL
    val messageText: String = "",
    val isSheetAttachment: Boolean = false,
    val attachmentData: String? = null, // JSON/Text summary representing verified outpass exits structured sheet
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "college_events")
data class CollegeEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val venue: String,
    val organizerRole: String, // e.g. PRINCIPAL, HOD, ADMIN, MENTOR, CLASS_ADVISOR
    val filterDepartment: String = "ALL", // ALL or specific
    val isPaused: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

