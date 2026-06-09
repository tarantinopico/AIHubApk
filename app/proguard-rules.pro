# Add project specific ProGuard rules here.

-keepattributes *Annotation*,Signature,Exception,InnerClasses,EnclosingMethod

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Ktor
-keep class io.ktor.** { *; }

# Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKd
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.internal.**
-keep,allowobfuscation,allowshrinking class * implements kotlinx.serialization.KSerializer {
    <init>();
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Compose
-keep class androidx.compose.** { *; }

# Models
-keep class com.example.domain.model.** { *; }
-keep class com.example.data.remote.model.** { *; }
-keep class com.example.data.local.entity.** { *; }

# Hilt & Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager
-keep class * extends dagger.hilt.android.internal.managers.FragmentComponentManager

