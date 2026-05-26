package com.nexus.feature.auth.presentation.di

import com.google.firebase.auth.FirebaseAuth
import com.nexus.feature.auth.data.datasource.AuthRemoteDataSource
import com.nexus.feature.auth.data.datasource.AuthRemoteDataSourceImpl
import com.nexus.feature.auth.data.repository.AuthRepositoryImpl
import com.nexus.feature.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(impl: AuthRemoteDataSourceImpl): AuthRemoteDataSource

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}
