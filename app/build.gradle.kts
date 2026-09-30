plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.digital_obd_ii"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.digital_obd_ii"
        minSdk = 26      // engine-audio requer API 26+ (AAudio estável). Era 24 — ajuste mínimo necessário.
        targetSdk = 37
        versionCode = 505
        versionName = "4.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Necessário para o módulo :engine-audio (compilação C++ com NDK)
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.compose.material.icons)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ─── Módulo de Áudio V6 Twin-Turbo ────────────────────────────────────────
    // Síntese procedural C++ (Oboe/AAudio) com latência ~5ms.
    // API: V6AudioEngine(context).startEngine() / .updateTelemetry(rpm, throttle, gear, speed)
    implementation(project(":engine-audio"))

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}


// ─── Distribuição Automática de APK (Google Drive + E-mail) ────────────────
tasks.register<Exec>("uploadApkToDrive") {
    group = "distribution"
    description = "Faz upload do APK gerado para o Google Drive e envia link por e-mail."
    workingDir = rootDir
    commandLine("python", "scripts/upload_to_drive_and_email.py")
}

tasks.register("assembleAndUpload") {
    group = "distribution"
    description = "Compila o APK Debug e envia automaticamente para o Google Drive e e-mail."
    dependsOn("assembleDebug")
    finalizedBy("uploadApkToDrive")
}
