package com.nexus.feature.messages.di

import com.nexus.feature.messages.data.MessagesRepositoryImpl
import com.nexus.feature.messages.domain.repository.MessagesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MessagesModule {

    @Binds
    @Singleton
    abstract fun bindMessagesRepository(impl: MessagesRepositoryImpl): MessagesRepository
}
