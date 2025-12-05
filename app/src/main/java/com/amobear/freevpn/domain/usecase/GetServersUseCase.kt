package com.amobear.freevpn.domain.usecase

import com.amobear.freevpn.domain.model.Server
import com.amobear.freevpn.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetServersUseCase @Inject constructor(
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(): Flow<List<Server>> {
        return serverRepository.getAllServers()
    }

    suspend fun getByCountry(countryCode: String): Flow<List<Server>> {
        return serverRepository.getServersByCountry(countryCode)
    }
}

