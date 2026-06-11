package com.khangkietson.smarthome.ui.scene

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.khangkietson.smarthome.data.model.Scene
import com.khangkietson.smarthome.data.repository.SceneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class SceneUiState(
    val scenes: List<Scene> = emptyList(),
    val aiSuggestedSceneId: String? = null,
    val aiSuggestionMessage: String? = null
)

class SceneViewModel(private val repository: SceneRepository) : ViewModel() {

    private val _aiSuggestionId = MutableStateFlow<String?>(null)
    private val _aiSuggestionMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SceneUiState> = combine(
        repository.scenes,
        _aiSuggestionId,
        _aiSuggestionMessage
    ) { scenes, suggestionId, msg ->
        SceneUiState(
            scenes = scenes,
            aiSuggestedSceneId = suggestionId,
            aiSuggestionMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SceneUiState()
    )

    init {
        updateAiSuggestion()
    }

    fun activateScene(sceneId: String) {
        repository.activateScene(sceneId)
    }

    fun updateAiSuggestion() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 21..23, in 0..4 -> {
                _aiSuggestionId.value = "scene_sleep"
                _aiSuggestionMessage.value = "Dựa vào thói quen lúc $hour:00, hệ thống gợi ý kích hoạt kịch bản \"Đi ngủ\"."
            }
            in 7..16 -> {
                _aiSuggestionId.value = "scene_work"
                _aiSuggestionMessage.value = "Hiện tại là giờ đi làm ($hour:00), hệ thống gợi ý kịch bản \"Đi làm\"."
            }
            in 17..20 -> {
                _aiSuggestionId.value = "scene_home"
                _aiSuggestionMessage.value = "Bạn vừa về nhà ($hour:00), hệ thống gợi ý kích hoạt kịch bản \"Về nhà\"."
            }
            else -> {
                _aiSuggestionId.value = null
                _aiSuggestionMessage.value = null
            }
        }
    }

    companion object {
        fun provideFactory(repository: SceneRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SceneViewModel(repository) as T
                }
            }
    }
}
