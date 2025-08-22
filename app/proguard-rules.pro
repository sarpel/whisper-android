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
