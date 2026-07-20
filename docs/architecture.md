# CardLens Architecture Notes

CardLens is implemented as a native Android MVP with Kotlin, Compose, Room, CameraX, ML Kit OCR, WorkManager reminders, Koin, and Supabase-compatible backend artifacts.

The first implementation keeps code in the `:app` Gradle module but separates it by package boundaries:

- `core/domain`: product entities and status models.
- `core/data`: Room database, DAO, entities, and repository contract.
- `core/ocr`: ML Kit OCR and OCR text parser.
- `core/supabase`: Auth repository, Supabase client factory, Edge Function client, and local AI fallback.
- `core/notifications`: WorkManager reminder scheduler.
- `feature/*`: presentation areas for the MVP screens.

The app now has two data modes:

- Demo mode uses owner `demo` and never calls Supabase.
- Signed-in mode scopes Room rows by Supabase user id, writes locally first, and syncs contacts/tags/follow-ups through PostgREST.

The next extraction step is to move these package boundaries into Gradle modules once the first demo flow is stable.
