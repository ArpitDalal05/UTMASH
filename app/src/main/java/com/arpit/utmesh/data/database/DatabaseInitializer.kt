package com.arpit.utmesh.data.database

import android.content.Context
import android.util.Log

/**
 * Initializes local UT Mesh state.
 *
 * This class intentionally contains no cloud
 * authentication, or server initialization.
 */
class DatabaseInitializer {

    fun initializeDatabase(context: Context) {
        try {
            // Local encryption/storage initialization.
            LocalKeyStorage.getOrCreateAesKey(context)

            // Stable local UT identity.
            val savedUid = LocalKeyStorage.getSavedUid(context)

            if (savedUid.isNullOrBlank()) {
                val generatedUid = generateLocalUid()
                LocalKeyStorage.saveUid(context, generatedUid)

                Log.i(
                    TAG,
                    "Created local UT identity: $generatedUid"
                )
            } else {
                Log.i(
                    TAG,
                    "Using existing local UT identity: $savedUid"
                )
            }

            // Initialize the additional local P2P identities if
            // the existing LocalKeyStorage implementation provides them.
            runCatching {
                LocalKeyStorage.getOrCreateRescueDeviceId(context)
            }.onFailure {
                Log.w(
                    TAG,
                    "Rescue device identity initialization unavailable",
                    it
                )
            }

            runCatching {
                LocalKeyStorage.getOrCreateP2pDeviceId(context)
            }.onFailure {
                Log.w(
                    TAG,
                    "P2P device identity initialization unavailable",
                    it
                )
            }

            runCatching {
                LocalKeyStorage.getOrCreateP2pSessionCode(context)
            }.onFailure {
                Log.w(
                    TAG,
                    "P2P session identity initialization unavailable",
                    it
                )
            }

            Log.i(
                TAG,
                "Local UT Mesh database/identity initialization complete"
            )

        } catch (error: Throwable) {
            Log.e(
                TAG,
                "Local UT Mesh initialization failed",
                error
            )
        }
    }

    private fun generateLocalUid(): String {
        return "UT-" +
            java.util.UUID.randomUUID()
                .toString()
                .replace("-", "")
                .take(12)
                .uppercase()
    }

    companion object {
        private const val TAG = "UTMESH_DB"
    }
}
