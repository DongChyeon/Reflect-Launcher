package com.dongchyeon.reflect.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.dongchyeon.reflect.core.data.repository.HomeRepositoryImpl
import com.dongchyeon.reflect.core.domain.repository.HomeRepository
import com.dongchyeon.reflect.core.domain.usecase.GetHomeDataUseCase
import com.dongchyeon.reflect.core.domain.usecase.IncrementVisitCountUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reflect_preferences"
)

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideGetHomeDataUseCase(repo: HomeRepository): GetHomeDataUseCase =
        GetHomeDataUseCase(repo)

    @Provides
    @Singleton
    fun provideIncrementVisitCountUseCase(repo: HomeRepository): IncrementVisitCountUseCase =
        IncrementVisitCountUseCase(repo)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
}
