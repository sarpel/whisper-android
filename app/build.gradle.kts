plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.app.whisper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.app.whisper"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")  // ARM v8 only
        }

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17", "-O3", "-ffast-math")
                arguments(
                    "-DANDROID_ARM_NEON=ON",
                    "-DANDROID_STL=c++_shared"
                )
            }
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // Load signing configuration from local.properties or environment variables
            val keystoreFile = project.findProperty("KEYSTORE_FILE") as String?
                ?: System.getenv("KEYSTORE_FILE")
            val keystorePassword = project.findProperty("KEYSTORE_PASSWORD") as String?
                ?: System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = project.findProperty("KEY_ALIAS") as String?
                ?: System.getenv("KEY_ALIAS")
            val keyPassword = project.findProperty("KEY_PASSWORD") as String?
                ?: System.getenv("KEY_PASSWORD")

            if (keystoreFile != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keyAlias
                keyPassword = keyPassword

                // Enable v1 and v2 signing for maximum compatibility
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            } else {
                // Fallback to debug keystore for development
                storeFile = file("debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
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

            // Use release signing configuration
            signingConfig = signingConfigs.getByName("release")

            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }

            // Performance optimizations
            isDebuggable = false
            isJniDebuggable = false
            isPseudoLocalesEnabled = false
            isCrunchPngs = true

            // Release-specific build configuration
            buildConfigField("String", "API_BASE_URL", "\"https://api.whisper-android.com\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            buildConfigField("boolean", "ENABLE_CRASH_REPORTING", "true")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "true")
        }

        debug {
            isDebuggable = true
            isJniDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            // Debug-specific optimizations
            isMinifyEnabled = false
            isShrinkResources = false
            isRenderscriptDebuggable = true
            isPseudoLocalesEnabled = true
        }

        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            isDebuggable = true

            // Staging-specific configuration
            buildConfigField("String", "API_BASE_URL", "\"https://staging-api.whisper-android.com\"")
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            buildConfigField("boolean", "ENABLE_CRASH_REPORTING", "true")
        }

        create("benchmark") {
            initWith(getByName("release"))
            applicationIdSuffix = ".benchmark"
            versionNameSuffix = "-benchmark"
            isDebuggable = false

            // Benchmark-specific optimizations
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true

            // Enable baseline profile generation
            buildConfigField("boolean", "ENABLE_BASELINE_PROFILE", "true")
        }
    }

    flavorDimensions += listOf("version", "environment")

    productFlavors {
        create("free") {
            dimension = "version"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"

            // Free version configuration
            buildConfigField("boolean", "IS_PRO_VERSION", "false")
            buildConfigField("int", "MAX_RECORDING_DURATION_MINUTES", "5")
            buildConfigField("boolean", "ENABLE_CLOUD_SYNC", "false")

            // Resource configuration for free version
            resValue("string", "app_name", "Whisper Free")
        }

        create("pro") {
            dimension = "version"
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"

            // Pro version configuration
            buildConfigField("boolean", "IS_PRO_VERSION", "true")
            buildConfigField("int", "MAX_RECORDING_DURATION_MINUTES", "60")
            buildConfigField("boolean", "ENABLE_CLOUD_SYNC", "true")

            // Resource configuration for pro version
            resValue("string", "app_name", "Whisper Pro")
        }

        create("development") {
            dimension = "environment"

            // Development environment configuration
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.whisper-android.com\"")
            buildConfigField("boolean", "ENABLE_DEBUG_TOOLS", "true")
            buildConfigField("boolean", "ENABLE_MOCK_DATA", "true")
        }

        create("production") {
            dimension = "environment"

            // Production environment configuration
            buildConfigField("String", "API_BASE_URL", "\"https://api.whisper-android.com\"")
            buildConfigField("boolean", "ENABLE_DEBUG_TOOLS", "false")
            buildConfigField("boolean", "ENABLE_MOCK_DATA", "false")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON Serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Performance Optimization
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    implementation("androidx.tracing:tracing:1.2.0")

    // Room Database (temporarily disabled for build fix)
    // implementation("androidx.room:room-runtime:2.6.1")
    // implementation("androidx.room:room-ktx:2.6.1")
    // kapt("androidx.room:room-compiler:2.6.1")

    // Testing - Unit Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.1.4")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.robolectric:robolectric:4.11.1")

    // Testing - Instrumentation Tests
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing - Hilt
    testImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    kaptTest("com.google.dagger:hilt-android-compiler:2.48.1")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.48.1")

    // Testing - OkHttp MockWebServer
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
