# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/qasim/old folders/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:


-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}
-keepclassmembers class ar.com.daidalos.afiledialog {
   public *;
}
-keepclassmembers class de.bitsharesmunich.autobahn {
   public *;
}

-keep class de.bitsharesmunich.models.** { *; }


#-keepclassmembers class de.bitsharesmunich.blockpos {
#   public *;
#}
#-keepattributes class de.bitsharesmunich.blockpos.** { *; }

-keep class de.bitsharesmunich.models.** { *; }
#-keep class de.bitsharesmunich.** { *; }

# ButterKnife
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }

-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}

-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

-keepnames class * { @butterknife.Bind *;}

#-keep class org.spongycastle.** { *; }
#-dontwarn org.spongycastle.**

-keep class com.itextpdf.** { *; }
-keep class com.squareup.** { *; }
-keep class com.nostra13.** { *; }
-keep class com.google.zxing.** { *; }
-keep class com.github.** { *; }
-keep class me.grantland.** { *; }
-keep class de.codecrafters.** { *; }
-keep class cz.msebera.** { *; }
-keep class com.koushikdutta.** { *; }
-keep class com.itextpdf.** { *; }

#-dontwarn java.awt.**,javax.security.**,java.beans.**
-dontwarn okio.**
-dontwarn com.itextpdf.**



-keep class android.support.** { *; }
-keep interface android.support.** { *; }
#-keep class java.lang.** { *; }
#-keep interface java.lang.** { *; }
#-keep class java.util.** { *; }
#-keep interface java.util.** { *; }

-dontwarn android.support.**
#-dontwarn java.lang.**

-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }
-keep class android.support.v7.app.** { *; }
-keep interface android.support.v7.app.** { *; }
#-keep class com.google.android.gms.** { *; }
#-keep class com.google.android.** { *; }
#-keep class com.android.vending.** { *; }
-keepattributes Exceptions

# Keep GSON stuff
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# Keep Jackson stuff
-keep class org.codehaus.** { *; }
-keep class com.fasterxml.jackson.annotation.** { *; }

# Keep these for GSON and Jackson
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Keep Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.** *;
}
-keepclassmembers class * {
    @retrofit2.** *;
}
-dontwarn retrofit2.**

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn com.google.common.**;
-dontwarn org.bitcoinj.**;
