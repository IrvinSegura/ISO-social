# Keep line numbers for readable crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firestore maps documents to DTOs via reflection (toObject / set)
-keepattributes Signature,*Annotation*
-keepclassmembers class com.nexus.**.data.model.** {
    <init>(...);
    <fields>;
    public <methods>;
}
-keep class com.nexus.**.data.model.** { *; }

# kotlinx.serialization (Retrofit converter and type-safe navigation routes)
-keepattributes InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.nexus.**$$serializer { *; }

# Retrofit service interfaces
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Credential Manager (Google Sign-In)
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }

# Release builds drop verbose/debug/info logs (ours and libraries'), so no internal ids or
# payloads end up in logcat. Warnings and errors stay for crash diagnosis
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
# No Timber tree is planted in release; this also skips building the message strings
-assumenosideeffects class timber.log.Timber$Forest {
    public void v(...);
    public void d(...);
    public void i(...);
}
