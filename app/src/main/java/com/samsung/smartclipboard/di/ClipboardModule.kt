package com.samsung.smartclipboard.di

import com.samsung.smartclipboard.data.source.clipboard.AndroidClipboardDataSource
import com.samsung.smartclipboard.data.source.clipboard.ClipboardCaptureHandler
import com.samsung.smartclipboard.data.source.clipboard.ClipboardDataSource
import com.samsung.smartclipboard.data.source.clipboard.DefaultClipboardCaptureHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClipboardModule {

    @Binds
    @Singleton
    abstract fun bindClipboardDataSource(
        impl: AndroidClipboardDataSource
    ): ClipboardDataSource

    @Binds
    @Singleton
    abstract fun bindClipboardCaptureHandler(
        impl: DefaultClipboardCaptureHandler
    ): ClipboardCaptureHandler
}