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


# Ignore warnings about Android's incomplete Java 8 support
# See https://stackoverflow.com/a/48629518/487559
-dontwarn java.lang.invoke.**

# Keep SDK classes and their methods to be preserved as entry points to the code
-keep public class com.myfiziq.sdk.** {
    public protected *;
}

-keepattributes *Annotation*, Exceptions, Signature, InnerClasses, EnclosingMethod, LineNumberTable
