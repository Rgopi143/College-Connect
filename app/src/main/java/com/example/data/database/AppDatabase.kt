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
    version = 8,
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

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
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
                    userId = "pa",
                    name = "Mrs. Sarah Connor (Personal Assistant)",
                    rollNumber = "STAFF_PA",
                    department = "Administration Office",
                    email = "pa@pa.com",
                    phone = "+91 9441234585",
                    parentContact = "N/A",
                    role = "PA"
                ),
                User(
                    userId = "canteen",
                    name = "Chef Ram (Canteen Manager)",
                    rollNumber = "STAFF_CANTEEN",
                    department = "Campus Dining",
                    email = "canteen@neccanteen.com",
                    phone = "+91 9441234579",
                    parentContact = "N/A",
                    role = "CANTEEN"
                ),
                User(
                    userId = "store",
                    name = "Mr. Rajan (Academic Store Clerk)",
                    rollNumber = "STAFF_STORE",
                    department = "Stationery Store",
                    email = "store@necstationary.in",
                    phone = "+91 9441234580",
                    parentContact = "N/A",
                    role = "STORE"
                ),
                User(
                    userId = "security",
                    name = "Officer Bahadur (Gate Security)",
                    rollNumber = "STAFF_SECURITY",
                    department = "Campus Security Desk",
                    email = "security@necsecurity.in",
                    phone = "+91 9441234581",
                    parentContact = "N/A",
                    role = "SECURITY"
                ),
                User(
                    userId = "admin",
                    name = "Campus Portal Administrator",
                    rollNumber = "ADMIN_CS",
                    department = "IT Services",
                    email = "admin@ranbidge.com",
                    phone = "+91 9441234572",
                    parentContact = "N/A",
                    role = "ADMIN"
                )
            )

            for (user in users) {
                if (dao.getUserById(user.userId) == null) {
                    dao.insertUser(user)
                }
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
        }
    }
}
