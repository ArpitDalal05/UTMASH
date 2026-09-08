package com.arpit.utmesh.domain.offline

import com.arpit.utmesh.data.offline.OfflineRegionRepository

/**
 * Renames an existing offline region.
 */
class RenameRegionUseCase(private val repository: OfflineRegionRepository) {

    /** Executes the rename operation. */
    suspend operator fun invoke(regionId: Long, newName: String) {
        repository.rename(regionId, newName)
    }
}
