package uz.cardlens.core.supabase

import io.github.jan.supabase.SupabaseClient
import uz.cardlens.core.supabase.SupabaseClientRef
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.cardlens.core.common.AppError
import uz.cardlens.core.common.AppResult

data class AuthUser(
    val id: String,
    val email: String,
    val isDemo: Boolean = false,
    val needsEmailConfirmation: Boolean = false
)

enum class AuthError : AppError {
    NOT_CONFIGURED,
    INVALID_INPUT,
    EMAIL_CONFIRMATION_REQUIRED,
    UNKNOWN
}

interface AuthRepository {
    val isConfigured: Boolean
    val currentUser: StateFlow<AuthUser?>
    suspend fun restoreSession(): AppResult<AuthUser?, AuthError>
    suspend fun signIn(email: String, password: String): AppResult<AuthUser, AuthError>
    suspend fun signUp(email: String, password: String): AppResult<AuthUser, AuthError>
    suspend fun continueDemo(): AuthUser
    suspend fun signOut()
    fun currentOwnerId(): String
}

class SupabaseAuthRepository(
    private val ref: SupabaseClientRef,
) : AuthRepository {
    private val client: SupabaseClient? = ref.client
    private val _currentUser = MutableStateFlow<AuthUser?>(null)

    override val isConfigured: Boolean = client != null
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    override suspend fun restoreSession(): AppResult<AuthUser?, AuthError> {
        val supabase = client ?: return AppResult.Success(null)
        return try {
            supabase.auth.awaitInitialization()
            val user = supabase.auth.currentUserOrNull()
                ?: supabase.auth.currentSessionOrNull()?.user
            val authUser = user?.let {
                AuthUser(
                    id = it.id,
                    email = it.email.orEmpty()
                )
            }
            _currentUser.value = authUser
            AppResult.Success(authUser)
        } catch (_: Exception) {
            AppResult.Error(AuthError.UNKNOWN)
        }
    }

    override suspend fun signIn(email: String, password: String): AppResult<AuthUser, AuthError> {
        val supabase = client ?: return AppResult.Error(AuthError.NOT_CONFIGURED)
        if (email.isBlank() || password.length < 6) return AppResult.Error(AuthError.INVALID_INPUT)
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val user = supabase.auth.currentUserOrNull()
                ?: supabase.auth.retrieveUserForCurrentSession(updateSession = true)
            val authUser = AuthUser(id = user.id, email = user.email ?: email.trim())
            _currentUser.value = authUser
            AppResult.Success(authUser)
        } catch (_: Exception) {
            AppResult.Error(AuthError.UNKNOWN)
        }
    }

    override suspend fun signUp(email: String, password: String): AppResult<AuthUser, AuthError> {
        val supabase = client ?: return AppResult.Error(AuthError.NOT_CONFIGURED)
        if (email.isBlank() || password.length < 6) return AppResult.Error(AuthError.INVALID_INPUT)
        return try {
            val signedUpUser = supabase.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            val activeUser = supabase.auth.currentUserOrNull()
            val authUser = if (activeUser != null) {
                AuthUser(id = activeUser.id, email = activeUser.email ?: email.trim())
            } else {
                AuthUser(
                    id = signedUpUser?.id.orEmpty(),
                    email = signedUpUser?.email ?: email.trim(),
                    needsEmailConfirmation = true
                )
            }
            if (!authUser.needsEmailConfirmation) {
                _currentUser.value = authUser
            }
            AppResult.Success(authUser)
        } catch (_: Exception) {
            AppResult.Error(AuthError.UNKNOWN)
        }
    }

    override suspend fun continueDemo(): AuthUser {
        val demoUser = AuthUser(
            id = DEMO_OWNER_ID,
            email = "demo@cardlens.app",
            isDemo = true
        )
        _currentUser.value = demoUser
        return demoUser
    }

    override suspend fun signOut() {
        runCatching { client?.auth?.signOut() }
        _currentUser.value = null
    }

    override fun currentOwnerId(): String {
        return currentUser.value?.id ?: DEMO_OWNER_ID
    }

    companion object {
        const val DEMO_OWNER_ID = "demo"
    }
}
