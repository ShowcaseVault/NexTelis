plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.showcasevault.nextelis"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.showcasevault.nextelis"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // App only ships English strings — this drops translated strings
        // pulled in transitively from AndroidX/Material for other locales.
        resourceConfigurations += listOf("en")
    }

    splits {
        abi {
            isEnable = true
            reset()
            // arm64-v8a covers virtually all devices from the last ~8 years;
            // the others are kept for older/emulator hardware.
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                // Linphone ships native .so libraries with debug symbols;
                // release builds don't need them and they're a large chunk
                // of APK size.
                debugSymbolLevel = "NONE"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.adapters)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.okhttp.logging)
    implementation(libs.linphone.sdk.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}