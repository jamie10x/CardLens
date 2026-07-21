# CardLens ProGuard Rules

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class uz.cardlens.**$$serializer { *; }
-keepclassmembers class uz.cardlens.** { *** Companion; }
-keepclasseswithmembers class uz.cardlens.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep Koin
-keep class org.koin.** { *; }

# Keep Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Supabase
-keep class io.github.jan.supabase.** { *; }

# Keep Coil
-keep class coil3.** { *; }

# Keep CameraX
-keep class androidx.camera.** { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }
