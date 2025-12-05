package com.amobear.freevpn.domain.usecase

import com.amobear.freevpn.domain.repository.ServerRepository
import javax.inject.Inject

/**
 * Use case to sync VPN servers from API
 * Follows Clean Architecture - ViewModel should only depend on Use Cases
 */
class SyncServersUseCase @Inject constructor(
    private val serverRepository: ServerRepository
) {
    /**
     * Sync servers from API
     * @param forceRefresh Force refresh even if servers exist locally
     * @return Result with number of servers synced
     */
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<Int> {
        return serverRepository.syncServersFromApi(forceRefresh)
    }
}

