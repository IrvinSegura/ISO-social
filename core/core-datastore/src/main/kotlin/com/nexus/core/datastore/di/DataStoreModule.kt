package com.nexus.core.datastore.di

import com.nexus.core.datastore.UserSessionDataStore
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// UserSessionDataStore is @Singleton and @Inject constructor — Hilt wires it automatically.
// This module is a placeholder for future proto DataStore or additional store bindings.
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule
