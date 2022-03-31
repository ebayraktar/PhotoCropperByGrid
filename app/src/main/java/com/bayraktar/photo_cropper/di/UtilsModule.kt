package com.bayraktar.photo_cropper.di

import android.content.Context
import com.bayraktar.photo_cropper.repositories.ImageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by emrebayraktar on 31,March,2022
 */

@InstallIn(SingletonComponent::class)
@Module
class UtilsModule {

    @Provides
    @Singleton
    fun provideImageRepository(@ApplicationContext context: Context): ImageRepository {
        return ImageRepository(context)
    }
}