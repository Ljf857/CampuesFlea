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

# ---- APM 性能监控 ----
# 数据模型靠 Gson 反射读写本地缓存，字段名即 JSON key，
# 一旦被混淆，重启后就再也读不回之前缓存的数据（表现是统计突然归零）
-keep class com.example.campusflea.apm.** { *; }
-keepattributes Signature
-keepattributes *Annotation*