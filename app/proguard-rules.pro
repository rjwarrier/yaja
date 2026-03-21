# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\ranji\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools-proguard.html

# Add any custom keep rules here that are specific to your project.

# If you use Gson or other reflection-based libraries, add rules here:
# -keep class com.mj.yaja.models.** { *; }

# Jetpack Compose rules are mostly handled by the compiler now,
# but you can add specific ones if you see issues with missing classes.

# ML Kit Language Identification — R8 strips internal factory/impl classes without these
-keep class com.google.mlkit.nl.languageid.** { *; }
-keep class com.google.android.gms.internal.mlkit_language_id** { *; }
