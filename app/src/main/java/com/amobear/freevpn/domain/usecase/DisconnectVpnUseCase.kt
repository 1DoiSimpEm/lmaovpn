package com.amobear.freevpn.domain.usecase

import com.amobear.freevpn.domain.repository.VpnRepository
import javax.inject.Inject

class DisconnectVpnUseCase @Inject constructor(
    private val vpnRepository: VpnRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return vpnRepository.disconnect()
    }
}

