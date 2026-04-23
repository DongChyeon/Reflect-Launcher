package com.dongchyeon.reflect.feature.home.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dongchyeon.reflect.core.domain.model.DomainError
import com.dongchyeon.reflect.core.domain.usecase.GetHomeDataUseCase
import com.dongchyeon.reflect.core.domain.usecase.IncrementVisitCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHomeDataUseCase: GetHomeDataUseCase,
    private val incrementVisitCountUseCase: IncrementVisitCountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<HomeSideEffect>()
    val sideEffect: SharedFlow<HomeSideEffect> = _sideEffect.asSharedFlow()

    init {
        observeHomeData()
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.ScreenResumed -> onScreenResumed()
            HomeUiEvent.PermissionGuideClicked -> onPermissionGuideClicked()
            is HomeUiEvent.AppSlotClicked -> onAppSlotClicked(event.packageName)
        }
    }

    private fun observeHomeData() {
        viewModelScope.launch {
            getHomeDataUseCase().collect { result ->
                result
                    .onSuccess { data ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                hasUsagePermission = data.hasUsagePermission,
                                timeWithoutPhone = data.timeWithoutPhone,
                                visitCount = data.visitCount,
                                intention = data.intention,
                                appSlots = data.appSlots.map { slot ->
                                    AppSlotUi(
                                        packageName = slot.packageName,
                                        label = slot.label,
                                        alpha = lerp(1.0f, 0.2f, slot.usageFraction)
                                    )
                                }
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.toUiMessage()
                            )
                        }
                    }
            }
        }
    }

    private fun onScreenResumed() {
        viewModelScope.launch { incrementVisitCountUseCase() }
    }

    private fun onPermissionGuideClicked() {
        viewModelScope.launch { _sideEffect.emit(HomeSideEffect.OpenUsageAccessSettings) }
    }

    private fun onAppSlotClicked(packageName: String) {
        viewModelScope.launch { _sideEffect.emit(HomeSideEffect.LaunchApp(packageName)) }
    }

    private fun lerp(start: Float, stop: Float, fraction: Float) =
        start + (stop - start) * fraction

    private fun Throwable?.toUiMessage(): String = when (this) {
        is DomainError.Permission -> "사용 통계 권한이 필요합니다"
        is DomainError.Storage -> "데이터를 불러오지 못했습니다"
        else -> "오류가 발생했습니다"
    }
}
