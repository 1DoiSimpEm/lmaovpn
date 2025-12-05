package com.amobear.freevpn.domain.usecase

import com.amobear.freevpn.domain.model.VpnConnection
import com.amobear.freevpn.domain.repository.VpnRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectionUseCase @Inject constructor(
    private val vpnRepository: VpnRepository
) {
    operator fun invoke(): Flow<VpnConnection> {
        return vpnRepository.observeConnectionState()
    }
}

