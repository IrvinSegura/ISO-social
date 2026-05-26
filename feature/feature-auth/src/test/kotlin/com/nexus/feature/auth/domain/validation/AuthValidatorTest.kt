package com.nexus.feature.auth.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun `validateEmail returns Valid for correct email`() {
        val result = AuthValidator.validateEmail("user@example.com")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `validateEmail returns Invalid for blank email`() {
        val result = AuthValidator.validateEmail("")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validateEmail returns Invalid for malformed email`() {
        val result = AuthValidator.validateEmail("not-an-email")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validatePassword returns Valid for strong password`() {
        val result = AuthValidator.validatePassword("Secure123")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `validatePassword returns Invalid for short password`() {
        val result = AuthValidator.validatePassword("abc1")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validatePassword returns Invalid for password without digits`() {
        val result = AuthValidator.validatePassword("onlyletters")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validatePassword returns Invalid for password without letters`() {
        val result = AuthValidator.validatePassword("12345678")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validateConfirmPassword returns Valid when passwords match`() {
        val result = AuthValidator.validateConfirmPassword("pass123", "pass123")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `validateConfirmPassword returns Invalid when passwords differ`() {
        val result = AuthValidator.validateConfirmPassword("pass123", "different")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validateDisplayName returns Valid for correct name`() {
        val result = AuthValidator.validateDisplayName("John Doe")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `validateDisplayName returns Invalid for blank name`() {
        val result = AuthValidator.validateDisplayName("")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `validateDisplayName returns Invalid for name exceeding max length`() {
        val result = AuthValidator.validateDisplayName("A".repeat(41))
        assertTrue(result is ValidationResult.Invalid)
    }
}
