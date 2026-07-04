package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppDao
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        OutpassRequest::class,
        CertificateRequest::class,
        StationeryItem::class,
        StationeryRequest::class,
        PrintRequest::class,
        CanteenItem::class,
        CanteenBooking::class,
        CollegeNotification::class,
        ChatMessage::class,
        CollegeEvent::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campus_connect_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.appDao())
                }
            }
        }

        suspend fun populateDatabase(dao: AppDao) {
            // Pre-populate Users
            val users = listOf(
                User(
                    userId = "23CS101",
                    name = "Alex Rivera",
                    rollNumber = "23CS101",
                    department = "Computer Science",
                    email = "alex.rivera@nrtec.in",
                    phone = "+91 9441234567",
                    parentContact = "+91 9885123456",
                    role = "STUDENT"
                ),
                User(
                    userId = "student",
                    name = "Alex Rivera (Student)",
                    rollNumber = "23CS101",
                    department = "Computer Science",
                    email = "student@nrtec.in",
                    phone = "+91 9441234567",
                    parentContact = "+91 9885123456",
                    role = "STUDENT"
                ),
                User(
                    userId = "23EC204",
                    name = "Emily Watson",
                    rollNumber = "23EC204",
                    department = "Electronics & Communication",
                    email = "emily.watson@nrtec.in",
                    phone = "+91 9441234568",
                    parentContact = "+91 9885123457",
                    role = "STUDENT"
                ),
                User(
                    userId = "advisor",
                    name = "Dr. Robert Finch (Class Advisor)",
                    rollNumber = "STAFF_ADVISOR",
                    department = "Computer Science",
                    email = "advisor@technoelite.com",
                    phone = "+91 9441234569",
                    parentContact = "N/A",
                    role = "CLASS_ADVISOR"
                ),
                User(
                    userId = "mentor",
                    name = "Prof. Sarah Miller (Academic Mentor)",
                    rollNumber = "STAFF_MENTOR",
                    department = "Computer Science",
                    email = "mentor@technoelite.com",
                    phone = "+91 9441234577",
                    parentContact = "N/A",
                    role = "MENTOR"
                ),
                User(
                    userId = "hod",
                    name = "Prof. Martha Vance (HOD, CS)",
                    rollNumber = "STAFF_HOD",
                    department = "Computer Science",
                    email = "hod@hod.com",
                    phone = "+91 9441234570",
                    parentContact = "N/A",
                    role = "HOD"
                ),
                User(
                    userId = "warden",
                    name = "Major Suresh Kumar (Warden/Principal)",
                    rollNumber = "STAFF_WARDEN",
                    department = "Campus Administration",
                    email = "suresh.kumar@college.edu",
                    phone = "+91 9441234571",
                    parentContact = "N/A",
                    role = "WARDEN"
                ),
                User(
                    userId = "principal",
                    name = "Dr. Robert Finch (Principal)",
                    rollNumber = "STAFF_PRINCIPAL",
                    department = "Administration Office",
                    email = "principal@principal.com",
                    phone = "+91 9441234578",
                    parentContact = "N/A",
                    role = "PRINCIPAL"
                ),
                User(
                    userId = "canteen",
                    name = "Chef Ram (Canteen Manager)",
                    rollNumber = "STAFF_CANTEEN",
                    department = "Campus Dining",
                    email = "canteen@college.edu",
                    phone = "+91 9441234579",
                    parentContact = "N/A",
                    role = "CANTEEN"
                ),
                User(
                    userId = "store",
                    name = "Mr. Rajan (Academic Store Clerk)",
                    rollNumber = "STAFF_STORE",
                    department = "Stationery Store",
                    email = "store@college.edu",
                    phone = "+91 9441234580",
                    parentContact = "N/A",
                    role = "STORE"
                ),
                User(
                    userId = "security",
                    name = "Officer Bahadur (Gate Security)",
                    rollNumber = "STAFF_SECURITY",
                    department = "Campus Security Desk",
                    email = "security@college.edu",
                    phone = "+91 9441234581",
                    parentContact = "N/A",
                    role = "SECURITY"
                ),
                User(
                    userId = "admin",
                    name = "Campus Portal Administrator",
                    rollNumber = "ADMIN_CS",
                    department = "IT Services",
                    email = "admin@college.edu",
                    phone = "+91 9441234572",
                    parentContact = "N/A",
                    role = "ADMIN"
                )
            )

            for (user in users) {
                dao.insertUser(user)
            }

            // Pre-populate Stationery Items
            val stationeryList = listOf(
                StationeryItem("pens", "Standard Gel Pens (Blue/Black)", 50, "Writing", 15.0),
                StationeryItem("pencils", "Nataraj 2B Pencils", 80, "Writing", 5.0),
                StationeryItem("notebooks", "Classmate Long Notebook (Ruled, 180 pgs)", 30, "Notebooks", 45.0),
                StationeryItem("record_books", "Chemistry/Physics Record Book", 25, "Notebooks", 65.0),
                StationeryItem("drawing_sheets", "Engineers Drawing A2 Paper", 100, "Laboratory", 12.0),
                StationeryItem("lab_manuals", "Python programming Lab Manual", 35, "Laboratory", 50.0),
                StationeryItem("files_folders", "Cobalt Blue Executive File Folder", 40, "Folders", 35.0)
            )
            dao.insertStationeryItems(stationeryList)

            // Pre-populate Canteen Menu
            val canteenList = listOf(
                CanteenItem(name = "Idli (2 pcs)", price = 20.0, category = "Breakfast"),
                CanteenItem(name = "Masala Dosa", price = 40.0, category = "Breakfast"),
                CanteenItem(name = "Poori Sabji", price = 35.0, category = "Breakfast"),
                CanteenItem(name = "Veg Meals (Full)", price = 80.0, category = "Lunch"),
                CanteenItem(name = "Egg Fried Rice", price = 60.0, category = "Lunch"),
                CanteenItem(name = "Veg Samosa (2 pcs)", price = 15.0, category = "Snacks"),
                CanteenItem(name = "Bread Omlette", price = 30.0, category = "Snacks"),
                CanteenItem(name = "Chapati with Dal (2 pcs)", price = 45.0, category = "Dinner"),
                CanteenItem(name = "Ginger Tea", price = 10.0, category = "Beverages"),
                CanteenItem(name = "Filter Coffee", price = 12.0, category = "Beverages"),
                CanteenItem(name = "Fresh Lime Soda", price = 20.0, category = "Beverages")
            )
            dao.insertCanteenItems(canteenList)

            // Pre-populate initial Notifications
            val notifications = listOf(
                CollegeNotification(
                    targetStudentId = "ALL",
                    title = "Semester Exams Registration Open",
                    content = "Please register for your upcoming semester examinations before next Friday. Ensure all pending library dues are cleared.",
                    category = "General"
                ),
                CollegeNotification(
                    targetStudentId = "ALL",
                    title = "Smart India Hackathon 2026",
                    content = "SIH Internal hackathon selection registrations are now active. Submit your innovative project proposals to Department Coordinators.",
                    category = "General"
                ),
                CollegeNotification(
                    targetStudentId = "23CS101",
                    title = "Welcome to Campus Connect Portal",
                    content = "Dear Alex, you can now seamlessly submit outpasses, book canteen meals, request certificates, and book stationery items online!",
                    category = "General"
                )
            )

            for (notif in notifications) {
                dao.insertNotification(notif)
            }

            // Pre-populate initial College Events
            val events = listOf(
                CollegeEvent(
                    title = "Annual Science & Tech Fest 2026",
                    description = "A grand inter-college technical symposium featuring robotics, hackathons, and research exhibitions.",
                    date = "2026-07-15",
                    time = "09:00 AM",
                    venue = "Main Auditorium & Block-A Seminars",
                    organizerRole = "PRINCIPAL",
                    filterDepartment = "ALL"
                ),
                CollegeEvent(
                    title = "Graduation Day Ceremony",
                    description = "Annual convocation ceremony for the graduating batch of 2026.",
                    date = "2026-08-01",
                    time = "10:30 AM",
                    venue = "Open Air Theatre (OAT)",
                    organizerRole = "ADMIN",
                    filterDepartment = "ALL"
                ),
                CollegeEvent(
                    title = "Campus Placement Prep Talk",
                    description = "Special training session on cracking technical coding interviews and resume building.",
                    date = "2026-06-25",
                    time = "02:00 PM",
                    venue = "Placement Cell Seminar Hall",
                    organizerRole = "HOD",
                    filterDepartment = "Computer Science"
                )
            )

            for (evt in events) {
                dao.insertEvent(evt)
            }
        }
    }
}
