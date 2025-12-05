package com.amobear.freevpn.domain.usecase

import com.amobear.freevpn.domain.model.TrafficStats
import com.amobear.freevpn.domain.repository.VpnRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MonitorTrafficUseCase @Inject constructor(
    private val vpnRepository: VpnRepository
) {
    operator fun invoke(): Flow<TrafficStats> {
        return vpnRepository.observeTrafficStats()
    }
}

