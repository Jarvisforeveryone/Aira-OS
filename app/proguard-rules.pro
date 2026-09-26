# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep project classes and models
-keep class com.example.aira.** { *; }
-keep class com.aira.voice.** { *; }
-keep class com.example.utils.** { *; }
-keep class com.example.models.** { *; }
-keep class com.example.service.** { *; }
-keep class com.example.data.** { *; }
-keep class com.example.ui.** { *; }
-keep class com.example.** { *; }
-keep class com.google.** { *; }

# ==============================================================================
# JNI & Native Method Rules
# ==============================================================================
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers class * {
    native <methods>;
}

# ==============================================================================
# Vosk Speech Recognition Engine (JNI & JNA)
# ==============================================================================
-keep class org.vosk.** { *; }
-keepclassmembers class org.vosk.** {
    native <methods>;
    public <methods>;
    public <fields>;
}
-keep class com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** {
    native <methods>;
    public <methods>;
    public <fields>;
}
-dontwarn org.vosk.**
-dontwarn com.sun.jna.**

# ==============================================================================
# Piper TTS Engine & ONNX Runtime (JNI / NCNN)
# ==============================================================================
-keep class com.tencent.piperncnn.** { *; }
-keepclassmembers class com.tencent.piperncnn.** {
    native <methods>;
    public <methods>;
    public <fields>;
}
-keep class com.aira.voice.** { *; }
-keepclassmembers class com.aira.voice.** {
    native <methods>;
    public <methods>;
    public <fields>;
}
-keep class com.rhasspy.** { *; }
-keepclassmembers class com.rhasspy.** {
    native <methods>;
    public <methods>;
    public <fields>;
}
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** {
    native <methods>;
    public <methods>;
    public <fields>;
}
-keep class com.example.util.NativeLibraryLoader { *; }
-dontwarn com.tencent.piperncnn.**
-dontwarn com.rhasspy.**
-dontwarn ai.onnxruntime.**

# ==============================================================================
# Room Database Components (Entities, DAOs, Migrations, and SQLite)
# ==============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-keep class * extends androidx.room.migration.Migration
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.Dao
-keep class * extends androidx.sqlite.db.SupportSQLiteOpenHelper$Factory
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }
-keep class androidx.sqlite.db.** { *; }
-keep class androidx.sqlite.db.framework.** { *; }

-keepclassmembers class * extends androidx.room.RoomDatabase {
    public void <init>();
    public <methods>;
}

-keepclassmembers class * implements androidx.room.RoomDatabase$Callback {
    public void <init>();
}

-keep class *_Impl { *; }
-keepclassmembers class *_Impl {
    public <init>(...);
    public <methods>;
    public <fields>;
}

-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
    @androidx.room.TypeConverters <methods>;
}
-dontwarn androidx.room.**
-dontwarn androidx.sqlite.**

# ==============================================================================
# Shizuku & Dev Rikka Components
# ==============================================================================
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-dontwarn dev.rikka.shizuku.**

# ==============================================================================
# Networking & Serialization (OkHttp, Retrofit, Moshi)
# ==============================================================================
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**

-keep class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class * { @com.squareup.moshi.JsonQualifier <fields>; }
-dontwarn com.squareup.moshi.**

# ==============================================================================
# General XML & 3rd Party Warnings Suppression
# ==============================================================================
-dontwarn org.xmlpull.v1.**
-dontwarn javax.xml.stream.**
-dontwarn org.simpleframework.xml.**
-dontwarn com.google.api.client.**
-dontwarn javax.lang.model.**
-dontwarn org.joda.time.**

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
