# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Keep Room components
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao
-dontwarn androidx.room.**

# Keep Piper TTS JNI classes and native methods
-keep class com.rhasspy.** { *; }
-keepclassmembers class com.rhasspy.** {
    native <methods>;
}

# Keep our data model classes for Moshi serialization
-keep class com.example.data.** { *; }
-keep class * { @com.squareup.moshi.JsonQualifier <fields>; }
-dontwarn com.squareup.moshi.**

# Kotlin reflection, annotations and signature attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

