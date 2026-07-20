# Supabase Setup

CardLens can run in local demo mode without Supabase credentials. To enable real auth and remote sync:

1. Create a Supabase project.
2. Run `supabase/migrations/20260608000000_cardlens_mvp.sql`.
3. Deploy the Edge Functions in `supabase/functions`.
4. Add this to `local.properties`:

```properties
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_ANON_KEY=your-publishable-or-anon-key
SUPABASE_FUNCTION_URL=https://your-project-ref.functions.supabase.co
```

5. Add `OPENAI_API_KEY` as a Supabase Edge Function secret if AI generation should use the server function.

The Android app keeps Room as the local source of truth. Signed-in saves are written locally first, then synced to Supabase. Manual sync pushes local signed-in records and pulls remote records back into Room.
