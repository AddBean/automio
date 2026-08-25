# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlinx Serialization rules
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontwarn kotlinx.serialization.**

# Keep serializer classes
-keep,includedescriptorclasses class com.hive.agent.**$$serializer { *; }
-keep,includedescriptorclasses class com.hive.plugin.agent.**$$serializer { *; }
-keep,includedescriptorclasses class com.hive.plugin.mcp.**$$serializer { *; }

# Keep serializable classes and their serializers
-keepclassmembers class com.hive.agent.** {
    *** Companion;
}
-keepclassmembers class com.hive.plugin.agent.** {
    *** Companion;
}
-keepclassmembers class com.hive.plugin.mcp.** {
    *** Companion;
}

# Keep classes annotated with @Serializable
-keep @kotlinx.serialization.Serializable class com.hive.agent.** { *; }
-keep @kotlinx.serialization.Serializable class com.hive.plugin.agent.** { *; }
-keep @kotlinx.serialization.Serializable class com.hive.plugin.mcp.** { *; }

# Keep Kotlinx Serialization infrastructure
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
    public static kotlinx.serialization.json.Json$* INSTANCE;
    public static kotlinx.serialization.json.Json$* defaultSerializer();
}
-keepclassmembers class kotlinx.serialization.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor rules
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class kotlin.coroutines.** { *; }
-dontwarn kotlin.coroutines.**

# Keep serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep data classes used in serialization (more specific)
-keepclassmembers class com.hive.agent.** {
    <fields>;
    <methods>;
}
-keepclassmembers class com.hive.plugin.agent.** {
    <fields>;
    <methods>;
}
-keepclassmembers class com.hive.plugin.mcp.** {
    <fields>;
    <methods>;
}

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**