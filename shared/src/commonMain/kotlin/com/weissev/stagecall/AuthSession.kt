package com.weissev.stagecall

import com.weissev.stagecall.data.SupabaseConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.postgrest.Postgrest

/** Shared so mobile deep link handlers and the UI use the same auth client. */
object AuthSession {
    val client = if (SupabaseConfig.isConfigured) createSupabaseClient(SupabaseConfig.url, SupabaseConfig.publishableKey) {
        install(Postgrest)
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = "stagecall"
            host = "auth"
        }
    } else null

    suspend fun signIn() { client?.auth?.signInWith(Google) }
    suspend fun signOut() { client?.auth?.signOut() }
}
