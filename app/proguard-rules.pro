# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Project Models for Firestore and Serialization
-keep class com.enosh.fincalc.data.model.** { *; }
-keepclassmembers class * {
    <init>();
}

# Firebase
-keep class com.google.firebase.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep class com.enosh.fincalc.data.local.entity.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class com.enosh.fincalc.utils.BackupUtils$BackupData { *; }

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations, AnnotationDefault

# Credentials Manager
-keep class androidx.credentials.** { *; }

# Remove Log statements in Release build
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}
