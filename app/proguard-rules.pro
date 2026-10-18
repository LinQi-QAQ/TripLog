# 保留 Room、kotlinx.serialization 生成的类
-keep class com.linqi.triplog.data.local.** { *; }
-keep class com.linqi.triplog.ai.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
