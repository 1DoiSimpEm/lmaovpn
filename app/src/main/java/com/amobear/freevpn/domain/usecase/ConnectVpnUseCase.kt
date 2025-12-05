package com.amobear.freevpn.domain.usecase

import com.amobear.freevpn.domain.repository.VpnRepository
import javax.inject.Inject

class ConnectVpnUseCase @Inject constructor(
    private val vpnRepository: VpnRepository
) {
    suspend operator fun invoke(serverId: String): Result<Unit> {
        return vpnRepository.connect(serverId)
    }
}

