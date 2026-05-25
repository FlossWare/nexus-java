# JNexus Android ProGuard Rules
#
# Philosophy: Rely on library-provided R8 rules (via AAR files) where possible.
# Only add custom rules for:
#   1. Data models serialized/deserialized via Jackson
#   2. Classes accessed via reflection
#   3. Security-sensitive code that must not be obfuscated
#
# Libraries with built-in R8 rules (no custom rules needed):
#   - OkHttp (okhttp3-*-rules.jar in AAR)
#   - Jackson (jackson-module-kotlin has consumer rules)
#   - Jetpack Compose (built-in R8 rules)
#   - Material3 (built-in R8 rules)
#   - AndroidX Security Crypto (built-in rules)
#   - Kotlin stdlib (built-in rules)

# Suppress warnings for optional dependencies not used by JNexus
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**

# Keep JNexus data models - used for JSON serialization/deserialization
# Records are serialized via Jackson, field names must be preserved
-keep class org.flossware.jnexus.RepoRecord { *; }
-keep class org.flossware.jnexus.ComponentMetadata { *; }
-keep class org.flossware.jnexus.SearchCriteria { *; }
-keep class org.flossware.jnexus.RepositoryStats { *; }

# Keep JNexus interfaces - implemented by Android-specific classes
-keep interface org.flossware.jnexus.NexusHttpClient { *; }
-keep interface org.flossware.jnexus.Credentials { *; }
-keep interface org.flossware.jnexus.ProgressCallback { *; }

# Keep JNexus service layer - accessed via dependency injection
-keep class org.flossware.jnexus.NexusService { *; }

# Keep Android implementations - cannot be obfuscated due to DI
-keep class org.flossware.jnexus.android.NexusApplication { *; }
-keep class org.flossware.jnexus.android.NexusClientOkHttp { *; }
-keep class org.flossware.jnexus.android.CredentialsAndroid { *; }

# Allow obfuscation of Kotlin UI code (Compose screens)
# These are not accessed via reflection and can be safely obfuscated
