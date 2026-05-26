package com.nexus.feature.auth.domain.usecase

import com.nexus.core.common.result.NexusException
import com.nexus.core.common.result.NexusResult
import com.nexus.feature.auth.domain.model.AuthProvider
import com.nexus.feature.auth.domain.model.User
import com.nexus.feature.auth.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignInWithEmailUseCaseTest {

    private lateinit var repository: AuthRepository
    private lateinit var useCase: SignInWithEmailUseCase

    private val fakeUser = User(
        uid = "uid-456",
        email = "test@nexus.com",
        displayName = "Test User",
        photoUrl = null,
        isEmailVerified = true,
        provider = AuthProvider.EMAIL,
    )

    @Before
    fun setup() {
        repository = mockk {
            coEvery { authStateFlow } returns emptyFlow()
        }
        useCase = SignInWithEmailUseCase(repository)
    }

    @Test
    fun `returns Success when credentials are valid`() = runTest {
        coEvery { repository.signInWithEmail(any(), any()) } returns NexusResult.Success(fakeUser)

        val result = useCase("test@nexus.com", "Secure123")

        assertTrue(result is NexusResult.Success)
    }

    @Test
    fun `returns Error when email is invalid format`() = runTest {
        val result = useCase("not-an-email", "Secure123")

        assertTrue(result is NexusResult.Error)
        val error = (result as NexusResult.Error).exception
        assertTrue(error is NexusException.InvalidCredentialsException)
    }

    @Test
    fun `returns Error when password is blank`() = runTest {
        val result = useCase("test@nexus.com", "")

        assertTrue(result is NexusResult.Error)
    }

    @Test
    fun `returns Error when repository returns InvalidCredentials`() = runTest {
        coEvery { repository.signInWithEmail(any(), any()) } returns
            NexusResult.Error(NexusException.InvalidCredentialsException())

        val result = useCase("test@nexus.com", "WrongPass1")

        assertTrue(result is NexusResult.Error)
        assertTrue((result as NexusResult.Error).exception is NexusException.InvalidCredentialsException)
    }
}
