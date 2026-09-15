# CardLens ProGuard Rules — Release
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# Keep Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep Koin (reflection)
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Keep Coil3
-keep class coil3.** { *; }
-dontwarn coil3.**

# Keep CameraX / MLKit
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep domain / data models (Room entities, UI)
-keep class uz.cardlens.core.data.** { *; }
-keep class uz.cardlens.core.domain.** { *; }

# General Android
-dontwarn android.**
-keep class android.support.** { *; }
