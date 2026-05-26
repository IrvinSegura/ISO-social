package com.nexus.feature.auth.domain.usecase

import com.nexus.core.common.result.NexusResult
import com.nexus.feature.auth.domain.model.AuthProvider
import com.nexus.feature.auth.domain.model.User
import com.nexus.feature.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegisterWithEmailUseCaseTest {

    private lateinit var repository: AuthRepository
    private lateinit var useCase: RegisterWithEmailUseCase

    private val fakeUser = User(
        uid = "uid-123",
        email = "test@nexus.com",
        displayName = "Test User",
        photoUrl = null,
        isEmailVerified = false,
        provider = AuthProvider.EMAIL,
    )

    @Before
    fun setup() {
        repository = mockk {
            coEvery { authStateFlow } returns emptyFlow()
        }
        useCase = RegisterWithEmailUseCase(repository)
    }

    @Test
    fun `returns Success when all inputs are valid`() = runTest {
        coEvery { repository.registerWithEmail(any(), any(), any()) } returns NexusResult.Success(fakeUser)

        val result = useCase(
            RegisterParams(
                email = "test@nexus.com",
                password = "Secure123",
                confirmPassword = "Secure123",
                displayName = "Test User",
            )
        )

        assertTrue(result is RegisterResult.Success)
        assertEquals(fakeUser, (result as RegisterResult.Success).user)
    }

    @Test
    fun `returns ValidationErrors when email is invalid`() = runTest {
        val result = useCase(
            RegisterParams(
                email = "bad-email",
                password = "Secure123",
                confirmPassword = "Secure123",
                displayName = "Test User",
            )
        )

        assertTrue(result is RegisterResult.ValidationErrors)
        val errors = (result as RegisterResult.ValidationErrors).errors
        assertTrue(errors.any { it is RegisterValidationError.EmailError })
    }

    @Test
    fun `returns ValidationErrors when passwords do not match`() = runTest {
        val result = useCase(
            RegisterParams(
                email = "test@nexus.com",
                password = "Secure123",
                confirmPassword = "Different1",
                displayName = "Test User",
            )
        )

        assertTrue(result is RegisterResult.ValidationErrors)
        val errors = (result as RegisterResult.ValidationErrors).errors
        assertTrue(errors.any { it is RegisterValidationError.ConfirmPasswordError })
    }

    @Test
    fun `returns ValidationErrors when display name is blank`() = runTest {
        val result = useCase(
            RegisterParams(
                email = "test@nexus.com",
                password = "Secure123",
                confirmPassword = "Secure123",
                displayName = "",
            )
        )

        assertTrue(result is RegisterResult.ValidationErrors)
        val errors = (result as RegisterResult.ValidationErrors).errors
        assertTrue(errors.any { it is RegisterValidationError.DisplayNameError })
    }

    @Test
    fun `returns multiple ValidationErrors when multiple fields are invalid`() = runTest {
        val result = useCase(
            RegisterParams(
                email = "bad",
                password = "123",
                confirmPassword = "456",
                displayName = "",
            )
        )

        assertTrue(result is RegisterResult.ValidationErrors)
        val errors = (result as RegisterResult.ValidationErrors).errors
        assertTrue(errors.size >= 3)
    }
}
