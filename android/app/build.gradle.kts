plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val ponteKeystorePath = System.getenv("PONTE_SIGNING_KEYSTORE")
val ponteKeystorePassword = System.getenv("PONTE_SIGNING_PASSWORD")
val ponteKeyAlias = System.getenv("PONTE_SIGNING_ALIAS")
val ponteHasPermanentSigning = !ponteKeystorePath.isNullOrBlank() && !ponteKeystorePassword.isNullOrBlank() && !ponteKeyAlias.isNullOrBlank()

android {
    namespace = "com.luizfernando.ponte"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.luizfernando.ponte"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "0.2.5"
    }

    signingConfigs {
        if (ponteHasPermanentSigning) {
            create("pontePermanent") {
                storeFile = file(ponteKeystorePath!!)
                storePassword = ponteKeystorePassword
                keyAlias = ponteKeyAlias
                keyPassword = ponteKeystorePassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (ponteHasPermanentSigning) {
                signingConfig = signingConfigs.getByName("pontePermanent")
            }
        }
        getByName("release") {
            if (ponteHasPermanentSigning) {
                signingConfig = signingConfigs.getByName("pontePermanent")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.matrix.rustcomponents:sdk-android:26.11.17")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
