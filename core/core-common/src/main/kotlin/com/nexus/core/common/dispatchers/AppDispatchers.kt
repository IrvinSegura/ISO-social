package com.nexus.core.common.dispatchers

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class MainDispatcher

// Scope that lives as long as the process: for work that must outlive a screen
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
