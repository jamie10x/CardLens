package uz.cardlens.core.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

data class SupabaseClientRef(val client: SupabaseClient?)

object SupabaseClientFactory {
    fun create(url: String, anonKey: String): SupabaseClient? {
        if (url.isBlank() || anonKey.isBlank()) return null
        return try {
            createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = anonKey
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
            }
        } catch (_: Exception) {
            null
        }
    }
}
