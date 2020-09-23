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

-libraryjars <java.home>/lib/rt.jar
-dontpreverify
-verbose

-keepattributes *Annotation*, Exceptions, Signature, InnerClasses, EnclosingMethod, LineNumberTable

# Sqlite
-keep class org.sqlite.database.sqlite.** { *; }

-keep class com.myfiziq.sdk.db.** { *; }
-dontwarn com.myfiziq.sdk.db.**

-dontwarn com.myfiziq.sdk.db.ORMModelProcessor

-keepclassmembers class com.google.** {
    private void finalizeReferent();
    protected void finalizeReferent();
    public void finalizeReferent();
    void finalizeReferent();

    private *** startFinalizer(java.lang.Class,java.lang.Object);
    protected *** startFinalizer(java.lang.Class,java.lang.Object);
    public *** startFinalizer(java.lang.Class,java.lang.Object);
    *** startFinalizer(java.lang.Class,java.lang.Object);
}

-keep class **.Finalizer
-keepclassmembers class ** { *** startFinalizer( ... ); }

# Google Play Services
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

# Gson
-dontnote com.google.gson.internal.UnsafeAllocator

# Android Framework
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View { public <init>(android.content.Context); public <init>(android.content.Context, android.util.AttributeSet); public <init>(android.content.Context, android.util.AttributeSet, int); public void set*(...); }

-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

-keepclassmembers class * extends android.app.Activity { public void *(android.view.View); }
-keepclassmembers class android.app.Fragment { *** getActivity(); public *** onCreate(); public *** onCreateOptionsMenu(...); }

-keepclasseswithmembers class * { public <init>(android.content.Context, android.util.AttributeSet); }
-keepclasseswithmembers class * { public <init>(android.content.Context, android.util.AttributeSet, int); }
-keepclassmembers class * extends android.content.Context { public void *(android.view.View); public void *(android.view.MenuItem); }
-keepclassmembers class * implements android.os.Parcelable { static android.os.Parcelable$Creator CREATOR; }
-keepclassmembers class **.R$* { public static <fields>; }

-keep class com.google.j2objc.annotations.** { *; }
-dontwarn   com.google.j2objc.annotations.**
-keep class java.lang.ClassValue { *; }
-dontwarn   java.lang.ClassValue
-dontwarn javax.annotation.**
-dontwarn javax.inject.**

#AWS
-keep class org.apache.commons.logging.**               { *; }
-keep class com.amazonaws.services.sqs.QueueUrlHandler  { *; }
-keep class com.amazonaws.javax.xml.transform.sax.*     { public *; }
-keep class com.amazonaws.javax.xml.stream.**           { *; }
-keep class com.amazonaws.services.**.model.*Exception* { *; }
-keep class org.codehaus.**                             { *; }
-keepattributes Signature,*Annotation*
# For GSON serialization internally in AWS
-keepclassmembers enum com.amazonaws.** { *; }


# Class names are needed in reflection
-keepnames class com.amazonaws.**
-keepnames class com.amazon.**
# Request handlers defined in request.handlers
-keep class com.amazonaws.services.**.*Handler
# The following are referenced but aren't required to run
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.logging.**
# Android 6.0 release removes support for the Apache HTTP client
-dontwarn org.apache.http.**
# The SDK has several references of Apache HTTP client
-dontwarn com.amazonaws.http.**
-dontwarn com.amazonaws.metrics.**

-dontwarn javax.xml.stream.events.**
-dontwarn org.codehaus.jackson.**
-dontwarn org.apache.commons.logging.impl.**
-dontwarn org.apache.http.conn.scheme.**

-ignorewarnings