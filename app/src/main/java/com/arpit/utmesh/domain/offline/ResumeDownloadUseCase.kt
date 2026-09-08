package com.arpit.utmesh.domain.offline

import com.arpit.utmesh.data.offline.OfflineRegionRepository

/**
 * Resumes a paused offline download.
 */
class ResumeDownloadUseCase(private val repository: OfflineRegionRepository) {

    /** Invokes the repository resume action. */
    suspend operator fun invoke(regionId: Long) {
        repository.resume(regionId)
    }
}
