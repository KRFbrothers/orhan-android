# ============================================================================
#  Orhan AI — R8 rules for the release build
#
#  The release variant runs with isMinifyEnabled = true and
#  isShrinkResources = true, so this file is the complete contract between the
#  app and R8: everything below is a *runtime* lookup that static analysis
#  cannot see. Anything not listed here is deliberately left to R8 to strip —
#  in particular the ~2,700 unused icons that ship inside
#  androidx.compose.material:material-icons-extended.
#
#  Base rules come from "proguard-android-optimize.txt" (see app/build.gradle.kts),
#  which enables optimization passes. That file drops the attribute keeps that
#  "proguard-android.txt" used to add, so they are re-declared in section 1.
# ============================================================================

# ---------------------------------------------------------------------------
# 1. Attributes R8 would otherwise delete.
#
#    Without these, generic signatures and annotations vanish, which breaks
#    Kotlin reflection-free libraries (kotlinx/Compose runtime, DataStore) and
#    turns every stack trace into an unreadable name. The annotation block is
#    the documented requirement for androidx libraries.
# ---------------------------------------------------------------------------
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*,AnnotationDefault
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations
-keepattributes Exceptions,MethodParameters
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# 2. AndroidViewModel / ViewModel instantiation.
#
#    androidx.lifecycle.viewmodel.compose#viewModel() (used in OrhanNavHost.kt:306
#    and SettingsNavigation.kt:130) resolves the no-argument constructor
#    reflectively. R8 cannot prove the constructor is used and will remove it,
#    which is the classic "Cannot create an instance of class ...ViewModel"
#    crash that only appears in release builds.
# ---------------------------------------------------------------------------
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <init>();
}

# ---------------------------------------------------------------------------
# 3. Compose runtime.
#
#    @Composable functions and the runtime's slot table rely on generated
#    classes/members being reachable by name. AGP ships consumer rules for
#    Compose, but the compiler plugin's synthetic members still need the
#    package-level keep.
# ---------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-dontwarn androidx.compose.**

# ---------------------------------------------------------------------------
# 4. Compose tooling / previews are debug-only.
#
#    ui-tooling is an `implementation` dependency (it carries the preview
#    annotations compiled into the main source set), so its runtime half is on
#    the release classpath even though nothing calls it at runtime.
# ---------------------------------------------------------------------------
-dontwarn androidx.compose.ui.tooling.**

# ---------------------------------------------------------------------------
# 5. DataStore — Preferences serializers are looked up by name.
# ---------------------------------------------------------------------------
-keepclassmembers class * extends androidx.datastore.core.Serializer { *; }
-dontwarn androidx.datastore.**

# ---------------------------------------------------------------------------
# 6. Enum values read back by name.
#
#    Orhan stores user preferences (reply style, agent tone, appearance) as
#    enum *names* in DataStore and re-hydrates them with
#    `Enum.valueOf(...)` / `enumValueOf(...)`. R8's optimizer is allowed to
#    rewrite enum accessors, which breaks that round-trip.
# ---------------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum ai.orhan.data.prefs.** { *; }
-keepclassmembers enum ai.orhan.design.theme.** { *; }

# ---------------------------------------------------------------------------
# 7. Resources loaded by name.
#
#    design/theme/FallbackFonts.kt:100-101 resolves the Inter font files with
#    resources.getIdentifier(RES_UPRIGHT, "font", packageName) instead of R.font,
#    so the resource shrinker would consider them unreferenced. The fonts are
#    also referenced through R.font in the same file, but the getIdentifier path
#    is what decides between Inter and the Lato fallback at runtime.
# ---------------------------------------------------------------------------
-keep class ai.orhan.design.theme.FallbackFonts { *; }

# ---------------------------------------------------------------------------
# 8. Keep the app's own entry points and model types readable.
#
#    MainActivity is referenced from AndroidManifest at runtime; the data model
#    classes are persisted to disk as JSON keys, so renaming them is safe but
#    keeping them makes field-level crash reports and the on-disk format obvious.
#    (Kept intentionally narrow: no `-keep class ai.orhan.**` blanket rule, which
#    would defeat shrinking of the 2,700 unused icons.)
# ---------------------------------------------------------------------------
-keep class ai.orhan.MainActivity { *; }
-keep class ai.orhan.data.** { <fields>; }

# ---------------------------------------------------------------------------
# 9. Debugging aids for a shrinking-but-shipping build: keep line numbers useful
#    and do not strip the source file name (does not leak source code).
# ---------------------------------------------------------------------------
-keepnames class ai.orhan.** { *; }
