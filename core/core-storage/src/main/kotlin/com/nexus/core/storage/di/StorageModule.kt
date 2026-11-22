package com.nexus.core.storage.di

import com.nexus.core.storage.MediaStorage
import com.nexus.core.storage.SupabaseMediaStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    abstract fun bindMediaStorage(impl: SupabaseMediaStorage): MediaStorage
}
