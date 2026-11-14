package com.roma.jamiyati.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.roma.jamiyati.data.local.Frequency
import com.roma.jamiyati.data.repository.JamiyatiRepository
import com.roma.jamiyati.data.repository.MemberDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MemberFormItem(
    val localKey: Long,
    val id: Long = 0L,
    val name: String = "",
    val phone: String = ""
)

data class CircleEditorUiState(
    val circleId: Long? = null,
    val isEditing: Boolean = false,
    val name: String = "",
    val installmentInput: String = "",
    val frequency: Frequency = Frequency.MONTHLY,
    val startDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val members: List<MemberFormItem> = listOf(
        MemberFormItem(localKey = 1L),
        MemberFormItem(localKey = 2L)
    ),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val savedCircleId: Long? = null,
    val isDeleted: Boolean = false
) {
    val installmentAmount: Double
        get() = installmentInput.toDoubleOrNull() ?: 0.0

    val validMemberCount: Int
        get() = members.count { it.name.isNotBlank() }

    val totalPayoutPreview: Double
        get() = installmentAmount * validMemberCount
}

class CircleEditorViewModel(
    private val repository: JamiyatiRepository,
    circleId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CircleEditorUiState(
            circleId = circleId?.takeIf { it > 0L },
            isEditing = (circleId ?: 0L) > 0L
        )
    )
    val uiState: StateFlow<CircleEditorUiState> = _uiState.asStateFlow()

    private var nextLocalKey = 100L

    init {
        val id = circleId?.takeIf { it > 0L }
        if (id != null) {
            viewModelScope.launch {
                val existing = repository.getCircleOnce(id) ?: return@launch
                val sorted = existing.members.sortedBy { it.payoutOrder }
                _uiState.update { state ->
                    state.copy(
                        circleId = existing.circle.id,
                        isEditing = true,
                        name = existing.circle.name,
                        installmentInput = if (existing.circle.installmentAmount % 1.0 == 0.0) {
                            existing.circle.installmentAmount.toLong().toString()
                        } else {
                            existing.circle.installmentAmount.toString()
                        },
                        frequency = existing.circle.frequency,
                        startDateEpochDay = existing.circle.startDateEpochDay,
                        members = sorted.mapIndexed { idx, m ->
                            MemberFormItem(
                                localKey = (idx + 1).toLong(),
                                id = m.id,
                                name = m.name,
                                phone = m.phone
                            )
                        }.ifEmpty {
                            listOf(MemberFormItem(localKey = 1L), MemberFormItem(localKey = 2L))
                        }
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }

    fun onInstallmentChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(installmentInput = filtered, errorMessage = null) }
    }

    fun onFrequencyChange(frequency: Frequency) = _uiState.update { it.copy(frequency = frequency) }

    fun onStartDateChange(epochDay: Long) = _uiState.update { it.copy(startDateEpochDay = epochDay) }

    fun addMemberRow() {
        nextLocalKey += 1
        _uiState.update {
            it.copy(members = it.members + MemberFormItem(localKey = nextLocalKey))
        }
    }

    fun removeMemberRow(localKey: Long) {
        _uiState.update { state ->
            if (state.members.size <= 2) {
                state.copy(errorMessage = "يجب أن تحتوي الجمعية على مشتركَين على الأقل")
            } else {
                state.copy(
                    members = state.members.filterNot { it.localKey == localKey },
                    errorMessage = null
                )
            }
        }
    }

    fun onMemberNameChange(localKey: Long, name: String) {
        _uiState.update { state ->
            state.copy(
                members = state.members.map {
                    if (it.localKey == localKey) it.copy(name = name) else it
                },
                errorMessage = null
            )
        }
    }

    fun onMemberPhoneChange(localKey: Long, phone: String) {
        _uiState.update { state ->
            state.copy(
                members = state.members.map {
                    if (it.localKey == localKey) it.copy(phone = phone) else it
                },
                errorMessage = null
            )
        }
    }

    fun saveCircle() {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        val amount = state.installmentInput.toDoubleOrNull()
        val cleanMembers = state.members.filter { it.name.isNotBlank() }

        when {
            trimmedName.isEmpty() -> {
                _uiState.update { it.copy(errorMessage = "يرجى إدخال اسم الجمعية") }
                return
            }
            amount == null || amount <= 0.0 -> {
                _uiState.update { it.copy(errorMessage = "يرجى إدخال قيمة قسط صحيحة بالدينار الليبي") }
                return
            }
            cleanMembers.size < 2 -> {
                _uiState.update { it.copy(errorMessage = "يرجى إضافة مشتركَين على الأقل") }
                return
            }
            cleanMembers.any { it.phone.filter(Char::isDigit).length < 9 } -> {
                _uiState.update { it.copy(errorMessage = "يرجى التأكد من أرقام هواتف جميع المشتركين") }
                return
            }
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val savedId = repository.saveCircleWithMembers(
                circleId = state.circleId,
                name = trimmedName,
                installmentAmount = amount!!,
                frequency = state.frequency,
                startDateEpochDay = state.startDateEpochDay,
                members = cleanMembers.mapIndexed { index, item ->
                    MemberDraft(
                        id = item.id,
                        name = item.name,
                        phone = item.phone,
                        payoutOrder = index + 1
                    )
                }
            )
            _uiState.update { it.copy(isSaving = false, savedCircleId = savedId) }
        }
    }

    fun deleteCircle() {
        val id = _uiState.value.circleId ?: return
        viewModelScope.launch {
            repository.deleteCircle(id)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    class Factory(
        private val repository: JamiyatiRepository,
        private val circleId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CircleEditorViewModel(repository, circleId) as T
        }
    }
}
