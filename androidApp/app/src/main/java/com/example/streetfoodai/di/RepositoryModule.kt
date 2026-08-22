package com.example.streetfoodai.di

import com.example.streetfoodai.data.api.StreetFoodApi
import com.example.streetfoodai.data.local.TokenManager
import com.example.streetfoodai.data.repository.AuthRepository
import com.example.streetfoodai.data.repository.VendorRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: StreetFoodApi,
        tokenManager: TokenManager
    ): AuthRepository {
        return AuthRepository(api, tokenManager)
    }

    @Provides
    @Singleton
    fun provideVendorRepository(
        api: StreetFoodApi
    ): VendorRepository {
        return VendorRepository(api)
    }
}
