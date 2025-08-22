# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ================================
# Whisper Android Specific Rules
# ================================

# Keep native methods and JNI classes
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep WhisperNative class and all its methods
-keep class com.app.whisper.native.WhisperNative {
    public *;
    native <methods>;
}

# Keep JNI callback classes
-keep class com.app.whisper.native.** {
    public *;
}

# Keep data classes used in JNI
-keep class com.app.whisper.data.model.** {
    public *;
}

# Keep domain entities
-keep class com.app.whisper.domain.entity.** {
    public *;
}

# ================================
# Kotlin Specific Rules
# ================================

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcher {}

# ================================
# Android Architecture Components
# ================================

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Keep LiveData and StateFlow
-keep class androidx.lifecycle.** { *; }
-keep class androidx.lifecycle.viewmodel.** { *; }

# Whisper native library
-keep class com.app.whisper.native.** { *; }
-keepclassmembers class com.app.whisper.native.WhisperNative {
    native <methods>;
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-keep class androidx.compose.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp

# Keep data classes
-keep class com.app.whisper.data.model.** { *; }
-keep class com.app.whisper.domain.model.** { *; }

# ================================
# Performance Optimization Rules
# ================================

# Keep performance monitoring classes
-keep class com.app.whisper.performance.** { *; }

# Keep baseline profile classes
-keep class androidx.profileinstaller.** { *; }

# ================================
# Security and Encryption Rules
# ================================

# Keep encrypted preferences classes
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# ================================
# Networking Rules
# ================================

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep HTTP logging interceptor
-keep class okhttp3.logging.HttpLoggingInterceptor { *; }

# ================================
# Logging Rules
# ================================

# Keep Timber logging
-keep class timber.log.** { *; }
-keep class timber.log.Timber$Tree { *; }

# Remove debug logging in release builds
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
}

# ================================
# Audio Processing Rules
# ================================

# Keep audio processing classes
-keep class com.app.whisper.data.audio.** { *; }
-keep class android.media.AudioRecord { *; }
-keep class android.media.AudioFormat { *; }

# ================================
# Optimization Settings
# ================================

# Enable aggressive optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove unused code warnings
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
