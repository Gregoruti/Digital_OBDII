package com.example.digital_obd_ii.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_obd_ii.domain.model.CustomIconItem
import com.example.digital_obd_ii.domain.model.IconFunction
import com.example.digital_obd_ii.domain.model.IconResolutionCategory
import com.example.digital_obd_ii.domain.repository.CustomIconsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomIconsUiState(
    val selectedCategory: IconResolutionCategory = IconResolutionCategory.SMALL,
    val allIcons: List<CustomIconItem> = emptyList(),
    val isSaving: Boolean = false,
    val saveMessage: String? = null
) {
    val currentCategoryIcons: List<CustomIconItem>
        get() = allIcons.filter { it.category == selectedCategory }.sortedBy { it.slotIndex }
}

@HiltViewModel
class CustomIconsViewModel @Inject constructor(
    private val repository: CustomIconsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomIconsUiState())
    val uiState: StateFlow<CustomIconsUiState> = _uiState.asStateFlow()

    init {
        loadIcons()
    }

    private fun loadIcons() {
        viewModelScope.launch {
            repository.getIconsFlow().collect { icons ->
                _uiState.update { it.copy(allIcons = icons) }
            }
        }
    }

    fun selectCategory(category: IconResolutionCategory) {
        _uiState.update { it.copy(selectedCategory = category, saveMessage = null) }
    }

    fun updateIconImage(slotId: String, imageUri: String?) {
        updateIconItem(slotId) { it.copy(imageUri = imageUri) }
    }

    fun updatePosition(slotId: String, posX: Float, posY: Float) {
        updateIconItem(slotId) { it.copy(posX = posX, posY = posY) }
    }

    fun updateClickable(slotId: String, isClickable: Boolean) {
        updateIconItem(slotId) { it.copy(isClickable = isClickable) }
    }

    fun updateFunction(slotId: String, function: IconFunction) {
        updateIconItem(slotId) { it.copy(function = function) }
    }

    fun clearSlot(slotId: String) {
        updateIconItem(slotId) {
            it.copy(
                imageUri = null,
                isClickable = false,
                function = IconFunction.NONE,
                label = ""
            )
        }
    }

    private fun updateIconItem(slotId: String, transform: (CustomIconItem) -> CustomIconItem) {
        val currentList = _uiState.value.allIcons.toMutableList()
        val index = currentList.indexOfFirst { it.id == slotId }
        if (index >= 0) {
            currentList[index] = transform(currentList[index])
            _uiState.update { it.copy(allIcons = currentList) }
            viewModelScope.launch {
                repository.saveAll(currentList)
            }
        }
    }

    fun resetCurrentCategory() {
        viewModelScope.launch {
            repository.resetCategory(_uiState.value.selectedCategory)
            _uiState.update { it.copy(saveMessage = "Slots restaurados para o padrão de fábrica.") }
        }
    }

    fun clearSaveMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }
}
