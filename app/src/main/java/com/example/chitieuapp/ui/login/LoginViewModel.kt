package com.example.chitieuapp.ui.login

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitieuapp.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /** activityContext phải là Activity để Credential Manager hiển thị bottom sheet. */
    fun signIn(activityContext: Context) {
        if (_state.value.loading) return
        _state.value = LoginUiState(loading = true)
        viewModelScope.launch {
            auth.signInWithGoogle(activityContext)
                .onSuccess { _state.value = LoginUiState() } // MainViewModel thấy SignedIn và điều hướng
                .onFailure { e ->
                    _state.update {
                        LoginUiState(
                            loading = false,
                            error = when (e) {
                                is GetCredentialCancellationException -> null // người dùng tự đóng
                                is NoCredentialException ->
                                    "Thiết bị chưa có tài khoản Google. Hãy thêm tài khoản trong Cài đặt."
                                else -> "Đăng nhập thất bại: ${e.localizedMessage ?: "lỗi không xác định"}"
                            },
                        )
                    }
                }
        }
    }
}
