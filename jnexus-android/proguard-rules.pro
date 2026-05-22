# JNexus Android ProGuard Rules

# Keep Error Prone annotations (used by Google Crypto Tink library)
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.errorprone.annotations.** { *; }

# Keep Tink crypto library classes
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Keep Jackson JSON classes
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# Keep org.json classes
-keep class org.json.** { *; }

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep jnexus-core data models (records)
-keep class org.flossware.jnexus.** { *; }

# Keep Jetpack Compose classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Material3 classes
-keep class androidx.compose.material3.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Android EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
