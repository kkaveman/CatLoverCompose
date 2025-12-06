package com.example.catlovercompose.di

import com.example.catlovercompose.core.repository.ChatRepository
import com.example.catlovercompose.core.repository.PostRepository
import com.example.catlovercompose.core.repository.UserRepository
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
    fun provideUserRepository(): UserRepository {
        return UserRepository()
    }

    @Provides
    @Singleton
    fun provideChatRepository(userRepository: UserRepository): ChatRepository {
        return ChatRepository(userRepository)
    }

    @Provides
    @Singleton
    fun providePostRepository(userRepository: UserRepository): PostRepository {
        return PostRepository(userRepository)
    }
}