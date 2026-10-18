package com.linqi.triplog.ui.me

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linqi.triplog.data.backup.BackupManager
import com.linqi.triplog.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BackupUiState(
    val tripCount: Int = 0,
    val imageCount: Int = 0,
    val busy: Boolean = false,
    val message: String = "",
    val isError: Boolean = false
)

class BackupViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppContainer.of(app).tripRepository

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        refreshCounts()
    }

    fun refreshCounts() {
        viewModelScope.launch {
            val trips = repo.getAllSnapshot()
            _state.value = _state.value.copy(
                tripCount = trips.size,
                imageCount = trips.count { it.ticketImagePath.isNotBlank() }
            )
        }
    }

    fun export(target: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = "", isError = false)
            try {
                val result = BackupManager.export(getApplication(), target)
                _state.value = _state.value.copy(
                    busy = false,
                    message = "已导出 ${result.count} 条行程、${result.imageCount} 张车票图",
                    isError = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    busy = false,
                    message = "导出失败：${e.message ?: e.javaClass.simpleName}",
                    isError = true
                )
            }
        }
    }

    fun import(source: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = "", isError = false)
            try {
                val result = BackupManager.import(getApplication(), source)
                _state.value = _state.value.copy(
                    busy = false,
                    message = "已恢复 ${result.imported} 条行程、${result.imagesRestored} 张车票图",
                    isError = false
                )
                refreshCounts()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    busy = false,
                    message = "恢复失败：${e.message ?: e.javaClass.simpleName}",
                    isError = true
                )
            }
        }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = "", isError = false)
    }
}
