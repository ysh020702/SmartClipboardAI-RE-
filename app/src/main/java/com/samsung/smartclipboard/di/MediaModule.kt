package com.samsung.smartclipboard.di

import com.samsung.smartclipboard.data.source.media.AndroidMediaStoreDataSource
import com.samsung.smartclipboard.data.source.media.DefaultMediaImportHandler
import com.samsung.smartclipboard.data.source.media.MediaImportHandler
import com.samsung.smartclipboard.data.source.media.MediaStoreDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds
    @Singleton
    abstract fun bindMediaStoreDataSource(
        impl: AndroidMediaStoreDataSource
    ): MediaStoreDataSource

    @Binds
    @Singleton
    abstract fun bindMediaImportHandler(
        impl: DefaultMediaImportHandler
    ): MediaImportHandler
}