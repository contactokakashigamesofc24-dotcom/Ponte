package com.luizfernando.ponte

import android.app.Application
import android.util.Log
import org.matrix.rustcomponents.sdk.LogLevel
import org.matrix.rustcomponents.sdk.TracingConfiguration
import org.matrix.rustcomponents.sdk.initPlatform
import org.matrix.rustsdk.Android

/** Initialize Matrix's Android JNI context and TLS certificate verifier before any network call. */
class PonteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // The JNI entry point is exported by the Matrix FFI native library.
            System.loadLibrary("matrix_sdk_ffi")
            Android.init(this)
            initPlatform(
                TracingConfiguration(
                    logLevel = LogLevel.WARN,
                    traceLogPacks = emptyList(),
                    extraTargets = emptyList(),
                    writeToStdoutOrSystem = true,
                    writeToFiles = null,
                    sentryConfig = null
                ),
                useLightweightTokioRuntime = false
            )
            Log.i("Ponte", "Matrix Android TLS verifier and runtime initialized")
        } catch (error: Throwable) {
            Log.e("Ponte", "Could not initialize Matrix Android network security", error)
            throw error
        }
    }
}
