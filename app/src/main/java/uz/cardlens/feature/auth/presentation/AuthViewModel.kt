package uz.cardlens.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.cardlens.core.common.AppResult
import uz.cardlens.core.supabase.AuthError
import uz.cardlens.core.supabase.AuthRepository
import uz.cardlens.core.supabase.AuthUser

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isAuthConfigured: Boolean = false,
    val sessionRestored: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface AuthAction {
    data class EmailChanged(val value: String) : AuthAction
    data class PasswordChanged(val value: String) : AuthAction
    data class SignIn(val email: String, val password: String) : AuthAction
    data class SignUp(val email: String, val password: String) : AuthAction
    data object ContinueDemo : AuthAction
    data object ClearError : AuthAction
}

sealed interface AuthEffect {
    data object SignedIn : AuthEffect
}

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState(isAuthConfigured = authRepository.isConfigured))
    val state = _state.asStateFlow()

    private val _effects = Channel<AuthEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val result = authRepository.restoreSession()) {
                is AppResult.Success -> {
                    val user = result.data
                    _state.update {
                        it.copy(
                            isAuthConfigured = authRepository.isConfigured,
                            email = user?.email ?: "",
                            sessionRestored = true,
                        )
                    }
                }
                is AppResult.Error -> _state.update {
                    it.copy(
                        isAuthConfigured = authRepository.isConfigured,
                        sessionRestored = true,
                    )
                }
            }
        }
    }

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.EmailChanged -> _state.update { it.copy(email = action.value, errorMessage = null) }
            is AuthAction.PasswordChanged -> _state.update { it.copy(password = action.value, errorMessage = null) }
            is AuthAction.SignIn -> signIn(action.email, action.password)
            is AuthAction.SignUp -> signUp(action.email, action.password)
            is AuthAction.ContinueDemo -> continueDemo()
            is AuthAction.ClearError -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signIn(email, password)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(AuthEffect.SignedIn)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.error.toMessage())
                }
            }
        }
    }

    private fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.signUp(email, password)) {
                is AppResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(AuthEffect.SignedIn)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.error.toMessage())
                }
            }
        }
    }

    private fun continueDemo() {
        viewModelScope.launch {
            authRepository.continueDemo()
            _effects.send(AuthEffect.SignedIn)
        }
    }

    private fun AuthError.toMessage(): String = when (this) {
        AuthError.NOT_CONFIGURED -> "Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties first."
        AuthError.INVALID_INPUT -> "Enter a valid email and a password with at least 6 characters."
        AuthError.EMAIL_CONFIRMATION_REQUIRED -> "Check your email to confirm your account."
        AuthError.UNKNOWN -> "Authentication failed. Check your credentials and Supabase settings."
    }
}
