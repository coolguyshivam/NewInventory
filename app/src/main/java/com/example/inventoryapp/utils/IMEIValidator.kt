package com.example.inventoryapp.utils

/**
 * IMEI validation utility using the Luhn algorithm (IMEI check digit validation).
 * IMEI numbers are 15 digits long and the last digit is a check digit.
 */
object IMEIValidator {
    
    /**
     * Validates an IMEI number using the Luhn algorithm.
     * @param imei The IMEI string to validate
     * @return true if the IMEI is valid, false otherwise
     */
    fun isValidIMEI(imei: String): Boolean {
        // Check if IMEI is exactly 15 digits
        if (imei.length != 15 || !imei.all { it.isDigit() }) {
            return false
        }
        
        return isValidLuhn(imei)
    }
    
    /**
     * Validates a number using the Luhn algorithm.
     * The Luhn algorithm is used to validate IMEI numbers.
     */
    private fun isValidLuhn(number: String): Boolean {
        var sum = 0
        var alternate = false
        
        // Iterate from right to left
        for (i in number.length - 1 downTo 0) {
            val digit = number[i].digitToInt()
            
            if (alternate) {
                val doubled = digit * 2
                sum += if (doubled > 9) doubled - 9 else doubled
            } else {
                sum += digit
            }
            
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
    
    /**
     * Checks if a string could be an IMEI (15 digits)
     */
    fun isIMEIFormat(text: String): Boolean {
        return text.length == 15 && text.all { it.isDigit() }
    }
}