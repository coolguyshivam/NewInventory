package com.example.inventoryapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class App : Application() {
    override fun onCreate() {
        super.onCreate()
		Log.d("AppInit", "App.kt onCreate() called")

        try {
            FirebaseApp.initializeApp(this)
            Log.d("AppInit", "Firebase initialized successfully")

            FirebaseAnalytics.getInstance(this)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

            FirebaseFirestore.getInstance().firestoreSettings =
                FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
        } catch (e: Exception) {
            Log.e("AppInit", "Firebase setup failed", e)
        }
    }
}
