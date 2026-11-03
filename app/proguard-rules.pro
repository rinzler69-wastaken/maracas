# ── Maracas ProGuard / R8 Rules ──────────────────────────────────────
# Jetpack Compose + Material 3 + ProfileInstaller

# Keep Compose runtime internals that R8 cannot detect via reflection
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }

# Keep ProfileInstaller for baseline profile injection
-keep class androidx.profileinstaller.** { *; }

# Keep @Composable metadata (needed for runtime introspection)
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Google Material dynamic-colors reflection
-keep class com.google.android.material.color.** { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Suppress warnings for missing classes in optional dependencies
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
