package com.amobear.freevpn.domain.usecase

import com.amobear.freevpn.domain.repository.ServerRepository
import javax.inject.Inject

class PingServerUseCase @Inject constructor(
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(serverId: String): Long {
        return serverRepository.pingServer(serverId)
    }
}

