package com.example.inventoryapp.util

import android.util.Patterns
import com.example.inventoryapp.util.Constants.AADHAAR_NUMBER_LENGTH
import com.example.inventoryapp.util.Constants.DATE_FORMAT
import com.example.inventoryapp.util.Constants.PHONE_NUMBER_LENGTH
import java.text.SimpleDateFormat
import java.util.*

object ValidationUtils {
    
    /**
     * Validates phone number format and length
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        if (phone.isBlank()) return true // Optional field
        return phone.length == PHONE_NUMBER_LENGTH && phone.all { it.isDigit() }
    }
    
    /**
     * Validates Aadhaar number format and length
     */
    fun isValidAadhaarNumber(aadhaar: String): Boolean {
        if (aadhaar.isBlank()) return true // Optional field
        return aadhaar.length == AADHAAR_NUMBER_LENGTH && aadhaar.all { it.isDigit() }
    }
    
    /**
     * Validates email format
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return true // Optional field
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Validates URL format
     */
    fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return true // Optional field
        return Patterns.WEB_URL.matcher(url).matches()
    }
    
    /**
     * Validates date format and ensures it's not in the future
     */
    fun isValidDate(dateString: String): Boolean {
        return try {
            val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            sdf.isLenient = false
            val date = sdf.parse(dateString)
            val today = Date()
            date != null && !date.after(today)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validates serial number format
     */
    fun isValidSerialNumber(serial: String): Boolean {
        return serial.isNotBlank() && serial.trim().length >= 3
    }
    
    /**
     * Validates model name
     */
    fun isValidModelName(model: String): Boolean {
        return model.isNotBlank() && model.trim().length >= 2
    }
    
    /**
     * Validates customer name
     */
    fun isValidCustomerName(name: String): Boolean {
        return name.isNotBlank() && name.trim().length >= 2
    }
    
    /**
     * Validates amount
     */
    fun isValidAmount(amount: String): Boolean {
        return try {
            val value = amount.toDouble()
            value > 0.0
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    /**
     * Validates quantity
     */
    fun isValidQuantity(quantity: String): Boolean {
        return try {
            val value = quantity.toInt()
            value > 0
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    /**
     * Validates password strength
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6 && password.any { it.isDigit() } && password.any { it.isLetter() }
    }
    
    /**
     * Sanitizes input string
     */
    fun sanitizeInput(input: String): String {
        return input.trim().replace(Regex("\\s+"), " ")
    }
    
    /**
     * Validates description length
     */
    fun isValidDescription(description: String): Boolean {
        return description.length <= Constants.MAX_DESCRIPTION_LENGTH
    }
}