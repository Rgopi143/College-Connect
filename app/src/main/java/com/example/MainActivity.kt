package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.screens.CampusConnectApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PortalViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase with safety try-catch block for full system robustness
        try {
            Firebase.initialize(this)
            Log.d("FirebaseConnect", "Firebase initialized successfully in Campus Connect Portal!")
        } catch (e: Exception) {
            Log.e("FirebaseConnect", "Firebase initialization alert: ${e.message}")
        }

        // 1. Initialize Room Database and Repository
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = AppRepository(database.appDao())
        
        // 2. Instantiate unified PortalViewModel using Factory
        val factory = PortalViewModelFactory(application, repository)

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 3. Render Master Portal Container
                    CampusConnectApp(
                        viewModel = viewModel(factory = factory),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
