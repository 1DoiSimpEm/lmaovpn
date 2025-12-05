package com.amobear.freevpn.di

import com.amobear.freevpn.data.vpn.usecases.ComputeAllowedIPs
import com.amobear.freevpn.data.vpn.usecases.SettingsForConnection
import com.amobear.freevpn.data.vpn.usecases.SettingsForConnectionSync
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VpnModule {

    @Provides
    @Singleton
    fun provideSettingsForConnection(): SettingsForConnection {
        return SettingsForConnection()
    }

    @Provides
    @Singleton
    fun provideSettingsForConnectionSync(): SettingsForConnectionSync {
        return SettingsForConnectionSync()
    }

    @Provides
    @Singleton
    fun provideComputeAllowedIPs(): ComputeAllowedIPs {
        return ComputeAllowedIPs()
    }

    @Provides
    @Singleton
    fun provideVpnCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }
}

