package com.weissev.stagecall.data.auth

import com.weissev.stagecall.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/** Shared so mobile deep link handlers and the UI use the same auth client. */
object AuthSession {
    // App.kt observes this client’s auth status. MainActivity.kt and MainViewController.kt use it to finish the sign-in redirect.
    val client = if (SupabaseConfig.isConfigured) createSupabaseClient(SupabaseConfig.url, SupabaseConfig.publishableKey) {
        install(Postgrest) // Let the feature stores read Supabase tables and call SQL functions.
        install(Storage) // Let OrganizationRoleStore.kt upload images and MediaCache.kt download them.
        install(Auth) {
            flowType = FlowType.PKCE
            scheme = "stagecall"
            host = "auth"
        }
    } else null

    // LoginScreen.kt reports the button tap to App.kt, which starts Google sign-in here.
    suspend fun signIn() { client?.auth?.signInWith(Google) }
    suspend fun authenticateWithEmail(address: String, password: String, registering: Boolean) {
        val auth = requireNotNull(client).auth
        if (registering) {
            auth.signUpWith(Email) { email = address; this.password = password }
        } else {
            auth.signInWith(Email) { email = address; this.password = password }
        }
    }
    suspend fun signOut() { client?.auth?.signOut() }
}
