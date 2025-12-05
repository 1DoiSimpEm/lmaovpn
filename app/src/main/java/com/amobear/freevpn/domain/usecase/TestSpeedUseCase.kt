package com.amobear.freevpn.domain.usecase

import com.amobear.freevpn.domain.model.SpeedTestResult
import com.amobear.freevpn.domain.repository.ServerRepository
import javax.inject.Inject

class TestSpeedUseCase @Inject constructor(
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(serverId: String): SpeedTestResult {
        return serverRepository.testSpeed(serverId)
    }
}

