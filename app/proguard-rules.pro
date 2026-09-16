# CardLens — Optimized R8 rules (AGP 9.3.0 / R8 9.3.16 / Kotlin 2.4.10)
# Goal: maximal shrinking + obfuscation while relying on library consumer rules.
# Do NOT add blanket -keep for whole libraries — they ship their own rules.

# --------------------------------------------------------------------------
# 1) Attributes — keep what Room / Compose / crash reporters need
# --------------------------------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes RuntimeVisible*Annotations,RuntimeInvisible*Annotations

# --------------------------------------------------------------------------
# 2) General optimizations (complements proguard-android-optimize.txt)
#    R8 fullMode already aggressively shrinks; these are safe additive flags.
# --------------------------------------------------------------------------
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Strip debug logs in release — measurable size + tiny perf win
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# --------------------------------------------------------------------------
# 3) Entry points — never obfuscate or remove
# --------------------------------------------------------------------------
-keep public class com.neopulsar.cardlens.CardLensApplication { <init>(); *; }
-keep public class com.neopulsar.cardlens.MainActivity { <init>(); *; }

# --------------------------------------------------------------------------
# 4) Room — consumer rules already keep most, but be explicit for DB/Worker
#    Avoid: -keep class com.neopulsar.cardlens.core.data.** { *; }  (keeps everything)
# --------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * {
    @androidx.room.PrimaryKey <fields>;
    <fields>;
    <init>(...);
    *;
}
-keep @androidx.room.Dao class * { *; }
# Keep database holder itself (referenced via Room.databaseBuilder)
-keep class com.neopulsar.cardlens.core.data.CardLensDatabase { *; }

# Enums stored as String via name/valueOf — keep names stable across releases
-keepclassmembers enum com.neopulsar.cardlens.core.domain.** { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --------------------------------------------------------------------------
# 5) WorkManager — workers are instantiated via reflection
# --------------------------------------------------------------------------
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}
-keep public class com.neopulsar.cardlens.core.notifications.FollowUpReminderWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# --------------------------------------------------------------------------
# 6) Koin + ViewModels — Koin creates via reflection (get(), viewModel{})
#    Keep only constructors; do NOT -keep org.koin.** { *; }
# --------------------------------------------------------------------------
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class com.neopulsar.cardlens.feature.**.*ViewModel { <init>(...); }
-keep class com.neopulsar.cardlens.core.ocr.MlKitOcrProcessor { <init>(...); }
-keep class com.neopulsar.cardlens.core.notifications.ReminderScheduler { <init>(...); }
-keep class com.neopulsar.cardlens.core.data.RoomCardLensRepository { <init>(...); }
-keep class com.neopulsar.cardlens.core.datastore.AppPreferences { <init>(...); }

# --------------------------------------------------------------------------
# 7) Compose / Navigation / SplashScreen / DataStore — rely on consumer rules
#    No manual -keep needed. Keep Kotlin metadata for Compose stability.
# --------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keep class androidx.compose.runtime.** { *; }

# --------------------------------------------------------------------------
# 8) CameraX / ML Kit / Coil — rely on consumer rules
#    Previous: -keep androidx.camera.** { *; } + com.google.mlkit.** + coil3.**
#    is unnecessary and prevents shrinking. Removed.
#    If you see ClassNotFoundException in release for these, add narrow keep:
#    -keep class com.google.mlkit.vision.text.** { *; }
# --------------------------------------------------------------------------

# --------------------------------------------------------------------------
# 9) Kotlin coroutines / serialization — keep suspend helpers
# --------------------------------------------------------------------------
-keepclassmembers class ** {
    kotlinx.coroutines.** *;
}
# If you add kotlinx.serialization later, uncomment:
# -keepattributes RuntimeVisibleAnnotations,AnnotationDefault
# -keepclassmembers,allowobfuscation class * {
#     @kotlinx.serialization.Serializable <fields>;
# }
# -keepclassmembers class **$$serializer { *; }

# --------------------------------------------------------------------------
# 10) AndroidX — suppress notes, keep support compat if needed
# --------------------------------------------------------------------------
-dontnote **
-dontwarn java.lang.invoke.StringConcatFactory
# Keep legacy support check (tiny) — no need for android.support.** blanket
