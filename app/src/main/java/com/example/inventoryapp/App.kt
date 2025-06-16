package com.example.inventoryapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Firebase Analytics
        try {
            FirebaseAnalytics.getInstance(this)
        } catch (e: Exception) {
            Log.e("AppInit", "FirebaseAnalytics init failed", e)
        }

        // Initialize Crashlytics and enable crash reporting
        try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        } catch (e: Exception) {
            Log.e("AppInit", "Crashlytics init failed", e)
        }

        // Initialize Firestore (optional pre-warming)
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.firestoreSettings = firestore.firestoreSettings.toBuilder()
                .setPersistenceEnabled(true) // Offline support
                .build()
        } catch (e: Exception) {
            Log.e("AppInit", "Firestore init failed", e)
        }
    }
}
