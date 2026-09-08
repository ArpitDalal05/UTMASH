package com.arpit.utmesh.domain.offline

import com.arpit.utmesh.data.offline.OfflineRegionEntity
import com.arpit.utmesh.data.offline.OfflineRegionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Exposes a flow of persisted offline regions for UI consumption.
 */
class ListRegionsUseCase(private val repository: OfflineRegionRepository) {

    /** Returns the underlying repository flow. */
    operator fun invoke(): Flow<List<OfflineRegionEntity>> = repository.regionsFlow
}
