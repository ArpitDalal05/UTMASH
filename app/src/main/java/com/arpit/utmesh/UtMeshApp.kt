package com.arpit.utmesh

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.arpit.utmesh.data.hasAnyBleGattContacts
import com.arpit.utmesh.data.normalizeClassicCapableBleContacts
import com.arpit.utmesh.data.database.LocalKeyStorage
import com.arpit.utmesh.service.p2p.P2pGattServerService
import com.arpit.utmesh.telecom.RfcommTelecomCoordinator
import com.arpit.utmesh.util.initializeMapLibreSafely
import com.google.android.play.core.splitcompat.SplitCompat
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class UtMeshApp : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
    }

    override fun onCreate() {
        super.onCreate()
disableAutofillToPreventComposeAnr()

        val savedLanguage = runCatching {
            getSavedLanguageSync(this)
        }.getOrDefault("en")
        setLocale(this, savedLanguage, shouldRecreate = false)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                clearAutoPinnedSystemLocaleIfNeeded(this@UtMeshApp)
            }.onFailure { throwable ->
                Log.w(TAG, "Auto-pinned locale recovery failed", throwable)
            }
            runCatching {
                LocalKeyStorage.getOrCreateRescueDeviceId(this@UtMeshApp)
                LocalKeyStorage.getOrCreateP2pDeviceId(this@UtMeshApp)
                LocalKeyStorage.getOrCreateP2pSessionCode(this@UtMeshApp)
            }.onFailure { throwable ->
                Log.w(TAG, "Failed to initialize local device identities", throwable)
            }
            runCatching {
                normalizeClassicCapableBleContacts(this@UtMeshApp)
            }.onFailure { throwable ->
                Log.w(TAG, "Failed to normalize classic-capable BLE contacts", throwable)
            }
            runCatching {
                if (hasAnyBleGattContacts(this@UtMeshApp)) {
                    P2pGattServerService.ensureHosting(this@UtMeshApp)
                }
            }.onFailure { throwable ->
                Log.w(TAG, "Failed to bootstrap P2P GATT host", throwable)
            }
            // Warm up the MapLibre native runtime so the first map open is fast. MapLibre
            // enforces UI-thread init (CalledFromWorkerThreadException on this IO dispatcher),
            // but running it synchronously in onCreate blocked background cold starts and
            // ANR'd low-end devices (Crashlytics issue 79031ef1). So: hop to the main thread,
            // then wait for its looper to go IDLE — the warm-up never competes with start-up
            // work. Map screens that open first still lazy-init via createMapViewSafely.
            Handler(Looper.getMainLooper()).post {
                Looper.myQueue().addIdleHandler {
                    runCatching {
                        if (initializeMapLibreSafely(this@UtMeshApp, TAG)) {
                            Log.i(TAG, "MapLibre warm-up complete")
                        } else {
                            Log.w(TAG, "Map runtime is unavailable on this device. Map features will be disabled.")
                        }
                    }.onFailure { throwable ->
                        Log.w(TAG, "MapLibre warm-up failed", throwable)
                    }
                    false
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            RfcommTelecomCoordinator.initialize(this)
        }
clearStaleCallNotifications()

        
        // Keep an existing rescue certificate fresh in the background (renews when <24h
        // of its 72h validity remains, network permitting). No-op for devices without one.
    }

    /**
     * Foreground detection by started-activity counting (no lifecycle-process dependency): drives
     * the "last seen" presence heartbeat — starts on first activity, final stamp when the last stops.
     */
    private fun disableAutofillToPreventComposeAnr() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                runCatching {
                    activity.window?.decorView?.importantForAutofill =
                        View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
    /**
     * Resolves the user's rescue role the same way the backend does (Firestore
     * users/{uid}.role, with "field-team"/"field_team" normalised to "fieldteam"),
     * falling back to the locally cached role when Firestore is unreachable.
     * Returns null when no role can be determined — treated as "not authorized".
     */
    /**
     * Removes call notifications orphaned by a process death. Call state never survives the process
     * (both call engines are in-memory), so at Application start ANY ongoing call-category
     * notification is stale by definition. The GATT ongoing/incoming and internet-ring notifications
     * are posted via plain notify() (not attached to a foreground service), so the system does NOT
     * clear them when the process is killed mid-call — without this sweep they linger unswipeably
     * until reboot. Call-ended summaries are kept (category CALL but not ongoing).
     */
    private fun clearStaleCallNotifications() {
        runCatching {
            val manager = getSystemService(android.app.NotificationManager::class.java) ?: return
            manager.activeNotifications
                .filter {
                    it.notification.category == android.app.Notification.CATEGORY_CALL &&
                        (it.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
                }
                .forEach { manager.cancel(it.tag, it.id) }
        }.onFailure { Log.w(TAG, "Stale call-notification sweep failed", it) }
    }


    private companion object {
        private const val TAG = "UtMeshApp"
    }
}





