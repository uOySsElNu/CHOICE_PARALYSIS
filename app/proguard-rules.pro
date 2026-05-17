# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.choiceparalysis.turntable.**$$serializer { *; }
-keepclassmembers class com.choiceparalysis.turntable.** {
    *** Companion;
}
-keepclasseswithmembers class com.choiceparalysis.turntable.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializable classes
-keep @kotlinx.serialization.Serializable class com.choiceparalysis.turntable.** { *; }
