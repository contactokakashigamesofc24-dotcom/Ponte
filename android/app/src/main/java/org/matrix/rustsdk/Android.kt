package org.matrix.rustsdk

import android.content.Context

/** JNI bridge exported by matrix-sdk-ffi for Android TLS/runtime initialization. */
object Android {
    @JvmStatic
    external fun init(context: Context)
}
