# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Rules to suppress warnings about missing annotation processor classes
# Generated from build/outputs/mapping/release/missing_rules.txt
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedAnnotationTypes
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.element.Modifier
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.SimpleTypeVisitor8

#-assumenosideeffects class android.util.Log {
#    public static *** v(...);
#    public static *** d(...);
#    public static *** i(...);
#    public static *** w(...);
#    public static *** e(...);
#    public static *** wtf(...);
#}

# Keep all classes in the MediaPipe tasks vision and core packages
# This prevents R8/ProGuard from stripping or renaming necessary MediaPipe components
-keep class com.google.mediapipe.tasks.vision.** { *; }
-keep class com.google.mediapipe.tasks.core.** { *; }
-keep class com.google.mediapipe.proto.** { *; } # Important for protobuf definitions

# Explicitly keep protobuf-lite classes used by MediaPipe
# The error message references com.google.protobuf.MessageSchema, so keep its related classes.
# Often, you need to keep specific fields used for reflection.
-keep class com.google.protobuf.** { *; }

# These are crucial for keeping fields and methods within Protobuf-generated messages
# that are accessed via reflection (like 'filePointerMeta_').
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite$Builder { *; }
-keep class * implements com.google.protobuf.Internal$EnumLite { *; }
-keep class * implements com.google.protobuf.Internal$EnumLiteMap { *; }

# Also explicitly keep the class mentioned in the error for safety
-keep class com.google.mediapipe.tasks.core.proto.ExternalFileProto$ExternalFile { *; }

# Rules to preserve MediaPipe's core framework and native components.
# This addresses issues like "no caller found on the stack" and Graph initialization failures.
-keep class com.google.mediapipe.framework.** { *; }
-keep class com.google.mediapipe.glutil.** { *; } # If your app uses MediaPipe's OpenGL utilities

# General JNI rules (to prevent R8 from breaking native calls)
# Keep native methods from being stripped or renamed.
-keepclassmembers class * {
    native <methods>;
}
# Keep classes that contain native methods from being stripped.
-keepclasseswithmembers class * {
    native <methods>;
}

# --- ADD THESE NEW RULES TO YOUR proguard-rules.pro FILE ---

# R8 error: Missing class com.google.mediapipe.proto.CalculatorProfileProto$CalculatorProfile

# R8 error: Missing class com.google.mediapipe.proto.GraphTemplateProto$CalculatorGraphTemplate
#-keep class com.google.mediapipe.proto.CalculatorProfileProto$CalculatorProfile { *; }
#-keep class com.google.mediapipe.proto.GraphTemplateProto$CalculatorGraphTemplate { *; }
