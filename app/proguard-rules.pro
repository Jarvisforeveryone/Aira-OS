# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Keep project classes and models
-keep class com.example.aira.** { *; }
-keep class com.aira.voice.** { *; }
-keep class org.vosk.** { *; }
-keep class com.tencent.piperncnn.** { *; }
-keep class com.example.utils.** { *; }
-keep class com.example.models.** { *; }
-keep class com.example.service.** { *; }
-keep class com.example.data.** { *; }
-keep class com.example.ui.** { *; }
-keep class com.example.** { *; }
-keep class com.google.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-dontwarn org.xmlpull.v1.**
-dontwarn javax.xml.stream.**
-dontwarn org.simpleframework.xml.**
-dontwarn com.google.api.client.**
-dontwarn javax.lang.model.**
-dontwarn org.joda.time.**

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

# Strip Logcat logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

