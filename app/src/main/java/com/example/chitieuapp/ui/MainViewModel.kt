package com.example.chitieuapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chitieuapp.data.auth.AuthRepository
import com.example.chitieuapp.data.repository.AuthState
import com.example.chitieuapp.data.repository.CategoryRepository
import com.example.chitieuapp.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val categories: CategoryRepository,
    private val scheduler: SyncScheduler,
) : ViewModel() {

    val authState: StateFlow<AuthState> = auth.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    init {
        // Mỗi khi đăng nhập: nạp danh mục mặc định và đồng bộ dữ liệu.
        viewModelScope.launch {
            auth.authState.collect { state ->
                if (state is AuthState.SignedIn) {
                    categories.ensureDefaults()
                    scheduler.requestSync()
                    scheduler.schedulePeriodic()
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}
