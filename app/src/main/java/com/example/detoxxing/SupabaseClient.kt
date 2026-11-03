package com.example.detoxxing

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

val supabase = createSupabaseClient(
    supabaseUrl = "https://bjjqgfxqxzkxkgwdllik.supabase.co",
    supabaseKey = "sb_publishable_45rpFpqce4puNSYTs_OuqQ_gMt3FHIg"
) {
    install(Auth)
    install(Postgrest)
}

@Serializable
data class UserProfile(val id: String, val username: String? = null)

suspend fun fetchUsername(): String? {
    val userId = supabase.auth.currentUserOrNull()?.id ?: return null
    return supabase.from("user").select(columns = Columns.ALL) { filter { eq("id", userId) } }
        .decodeSingleOrNull<UserProfile>()?.username
}