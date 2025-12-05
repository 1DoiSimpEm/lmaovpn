package com.amobear.freevpn.domain.usecase

import com.amobear.freevpn.domain.repository.ServerRepository
import javax.inject.Inject

/**
 * Use case to initialize sample data if database is empty
 * Follows Clean Architecture - ViewModel should only depend on Use Cases
 */
class InitializeDataUseCase @Inject constructor(
    private val serverRepository: ServerRepository
) {
    /**
     * Initialize data (sample data if database is empty)
     * This will be handled by repository implementation
     */
    suspend operator fun invoke(): Result<Unit> {
        val syncResult = serverRepository.syncServersFromApi(forceRefresh = false)
        return if (syncResult.isSuccess) {
            Result.success(Unit)
        } else {
            Result.success(Unit)
        }
    }
}

