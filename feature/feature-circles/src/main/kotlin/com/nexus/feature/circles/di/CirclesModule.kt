package com.nexus.feature.circles.di

import com.nexus.feature.circles.data.CirclesRepositoryImpl
import com.nexus.feature.circles.domain.repository.CirclesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CirclesModule {

    @Binds
    @Singleton
    abstract fun bindCirclesRepository(impl: CirclesRepositoryImpl): CirclesRepository
}
