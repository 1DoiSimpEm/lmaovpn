package com.amobear.freevpn.di

import com.amobear.freevpn.data.repository.ServerRepositoryImpl
import com.amobear.freevpn.data.repository.VpnRepositoryImpl
import com.amobear.freevpn.domain.repository.ServerRepository
import com.amobear.freevpn.domain.repository.VpnRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVpnRepository(
        vpnRepositoryImpl: VpnRepositoryImpl
    ): VpnRepository

    @Binds
    @Singleton
    abstract fun bindServerRepository(
        serverRepositoryImpl: ServerRepositoryImpl
    ): ServerRepository
}

