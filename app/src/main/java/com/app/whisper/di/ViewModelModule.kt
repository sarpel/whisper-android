package com.app.whisper.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module for ViewModel-scoped dependencies.
 * 
 * This module provides dependencies that are scoped to the ViewModel lifecycle.
 * These dependencies are created when the ViewModel is created and destroyed
 * when the ViewModel is cleared.
 */
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    
    // ViewModels are automatically provided by Hilt when annotated with @HiltViewModel
    // This module can be used for ViewModel-scoped dependencies if needed
    
    // Example of ViewModel-scoped dependency:
    // @Provides
    // fun provideViewModelScopedDependency(): SomeDependency {
    //     return SomeDependency()
    // }
}
