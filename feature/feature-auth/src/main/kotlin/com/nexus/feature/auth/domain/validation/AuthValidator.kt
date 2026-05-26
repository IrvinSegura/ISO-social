package com.nexus.feature.auth.domain.validation

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}

object AuthValidator {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) return ValidationResult.Invalid("Email is required.")
        if (!email.matches(emailRegex)) return ValidationResult.Invalid("Enter a valid email address.")
        return ValidationResult.Valid
    }

    fun validatePassword(password: String): ValidationResult {
        if (password.isBlank()) return ValidationResult.Invalid("Password is required.")
        if (password.length < 8) return ValidationResult.Invalid("Password must be at least 8 characters.")
        if (!password.any { it.isDigit() }) return ValidationResult.Invalid("Password must contain at least one number.")
        if (!password.any { it.isLetter() }) return ValidationResult.Invalid("Password must contain at least one letter.")
        return ValidationResult.Valid
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationResult {
        if (confirmPassword.isBlank()) return ValidationResult.Invalid("Please confirm your password.")
        if (password != confirmPassword) return ValidationResult.Invalid("Passwords do not match.")
        return ValidationResult.Valid
    }

    fun validateDisplayName(name: String): ValidationResult {
        if (name.isBlank()) return ValidationResult.Invalid("Name is required.")
        if (name.trim().length < 2) return ValidationResult.Invalid("Name must be at least 2 characters.")
        if (name.trim().length > 40) return ValidationResult.Invalid("Name must be 40 characters or less.")
        return ValidationResult.Valid
    }
}
