# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Firebase
-keep class com.google.firebase.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep class com.example.visualvibefincal.data.local.entity.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class com.example.visualvibefincal.utils.BackupUtils$BackupData { *; }

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations, AnnotationDefault

# Credentials Manager
-keep class androidx.credentials.** { *; }
