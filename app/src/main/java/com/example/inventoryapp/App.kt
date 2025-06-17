package com.example.inventoryapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
          //  FirebaseApp.initializeApp(this)
          //  Log.d("AppInit", "Firebase initialized")

          //  FirebaseFirestore.getInstance().firestoreSettings =
          //     FirebaseFirestoreSettings.Builder().build()

        } catch (e: Exception) {
            Log.e("AppInit", "Firebase setup failed", e)
        }
    }
}
