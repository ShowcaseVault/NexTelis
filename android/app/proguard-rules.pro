# Linphone SDK loads its native bridge via JNI/reflection.
-keep class org.linphone.core.** { *; }
-keep class org.linphone.mediastream.** { *; }

# Retrofit / OkHttp — see https://square.github.io/retrofit/ (R2 section) for the standard rule set.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Moshi codegen — generated JsonAdapter classes are looked up via reflection
# by name, and the model classes they serialize need their fields kept.
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-dontwarn com.squareup.moshi.**
