package com.arpit.utmesh.domain.offline

import com.arpit.utmesh.data.offline.OfflineRegionRepository

/**
 * Pauses an ongoing offline download if present.
 */
class PauseDownloadUseCase(private val repository: OfflineRegionRepository) {

    /** Invokes the repository pause action. */
    suspend operator fun invoke(regionId: Long) {
        repository.pause(regionId)
    }
}
