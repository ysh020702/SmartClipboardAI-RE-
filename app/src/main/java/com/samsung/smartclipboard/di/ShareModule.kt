package com.samsung.smartclipboard.di

import com.samsung.smartclipboard.data.source.share.AndroidShareContentHandler
import com.samsung.smartclipboard.data.source.share.ShareContentHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ShareModule {

    @Binds
    @Singleton
    abstract fun bindShareContentHandler(
        impl: AndroidShareContentHandler
    ): ShareContentHandler
}