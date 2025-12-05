package com.amobear.freevpn.di

import android.content.Context
import androidx.room.Room
import com.amobear.freevpn.data.local.VpnDatabase
import com.amobear.freevpn.data.local.dao.ServerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVpnDatabase(
        @ApplicationContext context: Context
    ): VpnDatabase {
        return Room.databaseBuilder(
            context,
            VpnDatabase::class.java,
            VpnDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideServerDao(database: VpnDatabase): ServerDao {
        return database.serverDao()
    }
}

