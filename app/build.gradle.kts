import java.util.Properties
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.baselineprofile)
    kotlin("kapt")
}

val keystoreProperties = Properties().apply {
    val keystoreFile = rootProject.file("keystore.properties")
    if (keystoreFile.exists()) {
        keystoreFile.inputStream().use(::load)
    }
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use(::load)
    }
}

fun signingValue(propertyKey: String, envKey: String): String? {
    val fromProperties = keystoreProperties.getProperty(propertyKey)?.trim()
    if (!fromProperties.isNullOrEmpty()) return fromProperties
    return System.getenv(envKey)?.trim()?.takeIf { it.isNotEmpty() }
}

fun secretConfigValue(propertyKey: String, envKey: String): String {
    val fromLocalProperties = localProperties.getProperty(propertyKey)?.trim()
    if (!fromLocalProperties.isNullOrEmpty()) return fromLocalProperties
    return System.getenv(envKey)?.trim().orEmpty()
}

fun buildConfigString(value: String): String {
    val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
}

val googleWebClientId =
    secretConfigValue("GOOGLE_WEB_CLIENT_ID", "GOOGLE_WEB_CLIENT_ID")

val mobileSyncBaseUrl =
    secretConfigValue("MOBILE_SYNC_BASE_URL", "MOBILE_SYNC_BASE_URL")

val trustDossierBaseUrl =
    secretConfigValue("TRUST_DOSSIER_BASE_URL", "TRUST_DOSSIER_BASE_URL")
        .ifBlank { "https://crisisconnect.network" }

val mobileSyncPanelId =
    secretConfigValue("MOBILE_SYNC_PANEL_ID", "MOBILE_SYNC_PANEL_ID")

val crisisSentinelOnlineBaseUrl =
    secretConfigValue(
        "CRISIS_SENTINEL_ONLINE_BASE_URL",
        "CRISIS_SENTINEL_ONLINE_BASE_URL"
    ).ifBlank {
        "https://ssrcrisisconnect1-jxxgznalnq-uc.a.run.app"
    }

val enterpriseSsoProviderId =
    secretConfigValue(
        "ENTERPRISE_SSO_PROVIDER_ID",
        "ENTERPRISE_SSO_PROVIDER_ID"
    ).ifBlank {
        "oidc.crisisconnect-sso"
    }

val mapLibreApiKey =
    secretConfigValue("MAPLIBRE_API_KEY", "MAPLIBRE_API_KEY")

val appCheckDebugToken =
    secretConfigValue("APP_CHECK_DEBUG_TOKEN", "APP_CHECK_DEBUG_TOKEN")

val rescueNodeIdHexLengthRaw =
    secretConfigValue(
        "RESCUE_NODE_ID_HEX_LENGTH",
        "RESCUE_NODE_ID_HEX_LENGTH"
    )

val rescueNodeIdHexLength =
    rescueNodeIdHexLengthRaw
        .toIntOrNull()
        ?.coerceIn(8, 24)
        ?: 12

if (googleWebClientId.isBlank()) {
    logger.warn(
        "GOOGLE_WEB_CLIENT_ID is not set. Google Sign-In will be disabled."
    )
}

if (mapLibreApiKey.isBlank()) {
    logger.warn(
        "MAPLIBRE_API_KEY is not set. Map style requests may fail."
    )
}

if (
    rescueNodeIdHexLengthRaw.isNotBlank() &&
    rescueNodeIdHexLengthRaw.toIntOrNull() == null
) {
    logger.warn(
        "RESCUE_NODE_ID_HEX_LENGTH is invalid. Falling back to default value 12."
    )
}

val releaseStoreFilePath =
    signingValue("STORE_FILE", "ANDROID_KEYSTORE_FILE")

val releaseStorePassword =
    signingValue("STORE_PASSWORD", "ANDROID_KEYSTORE_PASSWORD")

val releaseKeyAlias =
    signingValue("KEY_ALIAS", "ANDROID_KEY_ALIAS")

val releaseKeyPassword =
    signingValue("KEY_PASSWORD", "ANDROID_KEYSTORE_PASSWORD")

val hasReleaseSigning =
    listOf(
        releaseStoreFilePath,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword
    ).all { !it.isNullOrEmpty() }

val isReleaseTaskRequested =
    gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("Release", ignoreCase = true)
    }

val isInternalTaskRequested =
    gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("Internal", ignoreCase = true)
    }

android {
    namespace = "com.arpit.utmesh"

    compileSdk = 36

    ndkVersion = "27.0.12077973"

    testBuildType =
        if (hasReleaseSigning) "internal" else "debug"

    dynamicFeatures += setOf(":feature_rescue")

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    defaultConfig {
        applicationId = "com.arpit.utmesh"

        minSdk = 24
        targetSdk = 36

        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters += listOf(
                "armeabi-v7a",
                "arm64-v8a",
                "x86_64"
            )
        }

        manifestPlaceholders["googleWebClientId"] =
            googleWebClientId

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            buildConfigString(googleWebClientId)
        )

        buildConfigField(
            "String",
            "ENTERPRISE_SSO_PROVIDER_ID",
            buildConfigString(enterpriseSsoProviderId)
        )

        buildConfigField(
            "String",
            "MOBILE_SYNC_BASE_URL",
            buildConfigString(mobileSyncBaseUrl)
        )

        buildConfigField(
            "String",
            "MOBILE_SYNC_PANEL_ID",
            buildConfigString(mobileSyncPanelId)
        )

        buildConfigField(
            "String",
            "TRUST_DOSSIER_BASE_URL",
            buildConfigString(trustDossierBaseUrl)
        )

        buildConfigField(
            "String",
            "CRISIS_SENTINEL_ONLINE_BASE_URL",
            buildConfigString(crisisSentinelOnlineBaseUrl)
        )

        buildConfigField(
            "int",
            "RESCUE_NODE_ID_HEX_LENGTH",
            rescueNodeIdHexLength.toString()
        )

        buildConfigField(
            "String",
            "APP_CHECK_DEBUG_TOKEN",
            buildConfigString("")
        )

        buildConfigField(
            "boolean",
            "VOICE_LATENCY_DIAGNOSTICS_ENABLED",
            "false"
        )

        resValue(
            "string",
            "maplibre_api_key",
            mapLibreApiKey
        )

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile =
                    file(requireNotNull(releaseStoreFilePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            versionNameSuffix = "-debug"

            buildConfigField(
                "String",
                "APP_CHECK_DEBUG_TOKEN",
                buildConfigString(appCheckDebugToken)
            )

            buildConfigField(
                "boolean",
                "VOICE_LATENCY_DIAGNOSTICS_ENABLED",
                "true"
            )
        }

        create("internal") {
            initWith(getByName("debug"))

            applicationIdSuffix = ".internal"
            versionNameSuffix = "-internal"

            resValue(
                "string",
                "app_name",
                "UT Mesh Internal"
            )

            buildConfigField(
                "String",
                "APP_CHECK_DEBUG_TOKEN",
                buildConfigString("")
            )

            buildConfigField(
                "boolean",
                "VOICE_LATENCY_DIAGNOSTICS_ENABLED",
                "true"
            )

            matchingFallbacks += listOf(
                "debug",
                "release"
            )

            if (hasReleaseSigning) {
                signingConfig =
                    signingConfigs.getByName("release")
            }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            buildConfigField(
                "boolean",
                "VOICE_LATENCY_DIAGNOSTICS_ENABLED",
                "false"
            )

            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )

            if (hasReleaseSigning) {
                signingConfig =
                    signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            excludes += "**/libsignal_jni_testing.so"
        }

        resources {
            excludes += "libsignal_jni*.dylib"
            excludes += "libsignal_jni*.so"
            excludes += "signal_jni*.dll"
        }
    }

    lint {
        disable += "MissingTranslation"
    }

    installation {
        installOptions += listOf("--user", "0")
    }

    bundle {
        abi {
            enableSplit = true
        }

        density {
            enableSplit = true
        }

        language {
            enableSplit = false
        }
    }
}

if (!hasReleaseSigning && isReleaseTaskRequested) {
    throw GradleException(
        "Release signing is not configured. Set keystore.properties or ANDROID_KEYSTORE_* env vars."
    )
}

if (!hasReleaseSigning && isInternalTaskRequested) {
    throw GradleException(
        "Internal signing is not configured. Set keystore.properties or ANDROID_KEYSTORE_* env vars."
    )
}

dependencies {

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.profileinstaller)

    "baselineProfile"(project(":baselineprofile"))

    // WebRTC
    implementation(
        "io.getstream:stream-webrtc-android:1.3.8"
    )

    // Signal Protocol
    implementation(
        "org.signal:libsignal-android:0.86.5"
    )

    testImplementation(
        "org.signal:libsignal-client:0.86.5"
    )

    coreLibraryDesugaring(
        "com.android.tools:desugar_jdk_libs:2.1.5"
    )

    implementation(
        "androidx.core:core-telecom:1.0.1"
    )

    implementation(
        "androidx.core:core-splashscreen:1.0.1"
    )

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation.layout)

    testImplementation(libs.junit)

    testImplementation(
        "org.json:json:20240303"
    )

    testImplementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3"
    )

    testImplementation(
        "androidx.arch.core:core-testing:2.2.0"
    )

    testImplementation(
        "androidx.test:core:1.6.1"
    )

    testImplementation(
        "org.robolectric:robolectric:4.12.2"
    )

    testImplementation(
        "io.mockk:mockk:1.13.12"
    )

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.ui.test.junit4
    )

    debugImplementation(
        libs.androidx.ui.tooling
    )

    debugImplementation(
        libs.androidx.ui.test.manifest
    )

    add(
        "internalImplementation",
        libs.androidx.ui.test.manifest
    )

    // Data Store
    implementation(
        "androidx.datastore:datastore-preferences:1.0.0"
    )

    // Compose
    implementation(
        "androidx.compose.material:material-icons-extended:1.7.8"
    )

    implementation(
        "androidx.navigation:navigation-compose:2.9.4"
    )

    implementation(
        "androidx.compose.foundation:foundation"
    )

    implementation(
        "com.airbnb.android:lottie-compose:6.4.0"
    )

    implementation(
        "androidx.compose.material:material-ripple"
    )

    // Glance
    implementation(
        "androidx.glance:glance-appwidget:1.2.0-rc01"
    )

    // Media & imaging
    implementation(
        "io.coil-kt:coil-compose:2.6.0"
    )

    implementation(
        "androidx.exifinterface:exifinterface:1.3.7"
    )

    // MapLibre
    implementation(
        "org.maplibre.gl:android-sdk:11.9.0"
    )

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.common)
    implementation(libs.media3.extractor)
    implementation(libs.media3.datasource)

    // QR
    implementation(
        "com.google.zxing:core:3.5.1"
    )

    // Camera
    implementation(
        "androidx.camera:camera-core:1.4.0"
    )

    implementation(
        "androidx.camera:camera-camera2:1.4.0"
    )

    implementation(
        "androidx.camera:camera-lifecycle:1.4.0"
    )

    implementation(
        "androidx.camera:camera-view:1.4.0"
    )

    implementation(
        "androidx.camera:camera-mlkit-vision:1.4.0"
    )

    implementation(
        "com.google.mlkit:barcode-scanning:17.3.0"
    )

    implementation(
        "com.google.android.gms:play-services-mlkit-text-recognition:19.0.1"
    )

    // Encryption
    implementation(
        "com.google.crypto.tink:tink-android:1.18.0"
    )

    implementation(
        "androidx.security:security-crypto-ktx:1.1.0"
    )

    implementation(
        libs.google.play.feature.delivery.ktx
    )

    // Room
    implementation(
        "androidx.room:room-runtime:2.8.4"
    )

    kapt(
        "androidx.room:room-compiler:2.8.4"
    )

    implementation(
        "androidx.room:room-ktx:2.8.4"
    )

    implementation(
        "net.zetetic:sqlcipher-android:4.13.0"
    )

    // Auth System + Firebase
    implementation(
        platform("com.google.firebase:firebase-bom:34.16.0")
    )

    implementation(
        "com.google.firebase:firebase-analytics"
    )

    implementation(
        "com.google.android.gms:play-services-auth:21.1.0"
    )

    implementation(
        "com.google.firebase:firebase-auth"
    )

    implementation(
        "com.google.firebase:firebase-firestore"
    )

    implementation(
        "com.google.firebase:firebase-functions"
    )

    implementation(
        "com.google.firebase:firebase-storage"
    )

    implementation(
        "com.google.firebase:firebase-messaging"
    )

    // Phone authentication
    implementation(
        "com.googlecode.libphonenumber:libphonenumber:8.13.55"
    )

    implementation(
        "com.github.murgupluoglu:flagkit-android:1.2.0"
    )

    implementation(
        "com.google.android.gms:play-services-auth-api-phone:18.2.0"
    )

    implementation(
        "com.google.android.recaptcha:recaptcha:18.9.1"
    )

    // Offline profile photo upload queue
    implementation(
        "androidx.work:work-runtime-ktx:2.9.1"
    )

    implementation(
        "com.google.firebase:firebase-appcheck-playintegrity"
    )

    debugImplementation(
        "com.google.firebase:firebase-appcheck-debug"
    )

    // Play Integrity
    implementation(
        "com.google.android.play:integrity:1.4.0"
    )

    // Google Play In-App Review
    implementation(
        "com.google.android.play:review:2.0.2"
    )

    implementation(
        "com.google.android.play:review-ktx:2.0.2"
    )

    // Networking
    implementation(
        "com.squareup.okhttp3:okhttp:4.11.0"
    )


    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    )

    // Firebase
    implementation(
        "com.google.firebase:firebase-crashlytics"
    )

    implementation(
        "com.google.firebase:firebase-analytics"
    )

    implementation(
        "com.google.firebase:firebase-perf"
    )

    // Opus
    implementation(
        files("libs/opus.aar")
    )

    implementation(
        "androidx.appcompat:appcompat:1.7.0"
    )

    implementation(
        "com.google.android.material:material:1.12.0"
    )
}


