# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.jlees.mibudget.**$$serializer { *; }
-keepclassmembers class com.jlees.mibudget.** { *** Companion; }
-keepclasseswithmembers class com.jlees.mibudget.** { kotlinx.serialization.KSerializer serializer(...); }

# Gemini Nano (ML Kit GenAI). Its native code looks up Java classes, fields and methods by their exact names through
# JNI, and R8 renames or removes them in release builds; it also talks to Android's AICore service.
-keep class com.google.mlkit.genai.** { *; }
-dontwarn com.google.mlkit.genai.**

# Any class with native methods keeps those methods' names.
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
