package com.example.inventoryapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d("AppInit", "Firebase initialized")
        } catch (e: Exception) {
            Log.e("AppInit", "Firebase init failed", e)
        }

        // Enable Firebase Analytics
        try {
            FirebaseAnalytics.getInstance(this)
            Log.d("AppInit", "Firebase Analytics initialized")
        } catch (e: Exception) {
            Log.e("AppInit", "Analytics init failed", e)
        }

        // Enable Crashlytics
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            Log.d("AppInit", "Crashlytics enabled")
        } catch (e: Exception) {
            Log.e("AppInit", "Crashlytics init failed", e)
        }

        // Enable Firestore persistence
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // ✅ works across SDK versions
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
            Log.d("AppInit", "Firestore initialized with persistence")
        } catch (e: Exception) {
            Log.e("AppInit", "Firestore init failed", e)
        }
    }
}
