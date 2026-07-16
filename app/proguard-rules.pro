# R8 rules for release builds.
#
# The enum rule below is the one that actually matters — the rest is defensive
# or warning suppression.

# ---------------------------------------------------------------------------
# Model classes — R8 must not rename these.
#
# Enums are persisted by NAME: BoardAspect / GradientPreset / TextureKind /
# ClockStyle / OverlayPosition are written to SQLite as strings (BackgroundCodec,
# ClockCodec, CollageEntities). R8 renames enum constants by default, so
# WIDE_16_9.name could serialise as "a" in one release and "b" in the next —
# every saved collage would silently decode to its fallback after an update.
# Quietly losing the user's collages on upgrade is about the worst bug this app
# could ship, so the model package is kept verbatim.
-keep class com.papercut.collage.model.** { *; }

# Same reasoning — these are persisted to SharedPreferences by name.
-keep class com.papercut.collage.ui.theme.AccentPalette { *; }
-keep class com.papercut.collage.data.ThemeMode { *; }

# General enum safety net (values()/valueOf are reached reflectively).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------------------------------------------------------------------
# Room — codegen rather than reflection, so mostly R8-safe, but the generated
# implementation is resolved by name from the abstract class.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# ML Kit subject segmentation — delivered via Play services, reflection-heavy.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# ---------------------------------------------------------------------------
# Widget entry points. These are reached from the manifest and from broadcasts,
# not from app code. CollageWidgetUpdater also calls
# setCharSequence(id, "setFormat12Hour", ...) reflectively on TextClock — the
# target is a framework class so R8 won't touch it, but keep ours intact.
-keep class com.papercut.collage.widget.** { *; }

# ---------------------------------------------------------------------------
# Kotlin / coroutines. The libraries ship their own rules; these cover the
# metadata R8 tends to warn about.
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlin.Metadata { *; }

# Readable crash reports in Play Console, without exposing original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
