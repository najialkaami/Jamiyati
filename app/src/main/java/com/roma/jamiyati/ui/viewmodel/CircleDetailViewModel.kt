package com.roma.jamiyati.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.roma.jamiyati.data.local.CircleWithDetails
import com.roma.jamiyati.data.local.MemberEntity
import com.roma.jamiyati.data.local.RoundPaymentEntity
import com.roma.jamiyati.data.repository.JamiyatiRepository
import com.roma.jamiyati.util.RoundCalculator
import com.roma.jamiyati.util.RoundScheduleItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CircleDetailUiState(
    val details: CircleWithDetails? = null,
    val sortedMembers: List<MemberEntity> = emptyList(),
    val schedule: List<RoundScheduleItem> = emptyList(),
    val currentRoundNumber: Int = 1,
    val lateMembers: List<MemberEntity> = emptyList(),
    val isDrawing: Boolean = false,
    /** الترتيب معتمد نهائياً (بعد القرعة أو الاعتماد اليدوي) */
    val orderLocked: Boolean = false,
    /** جميع المشتركين لديهم اسم ورقم هاتف صحيح، فيمكن اعتماد الترتيب */
    val membersComplete: Boolean = false,
    val highlightedMemberId: Long? = null,
    val isLoading: Boolean = true
)

class CircleDetailViewModel(
    private val repository: JamiyatiRepository,
    private val circleId: Long
) : ViewModel() {

    private val isDrawingFlow = MutableStateFlow(false)
    private val highlightedMemberIdFlow = MutableStateFlow<Long?>(null)
    private val previewMembersFlow = MutableStateFlow<List<MemberEntity>?>(null)

    val uiState: StateFlow<CircleDetailUiState> = combine(
        repository.observeCircle(circleId),
        isDrawingFlow,
        highlightedMemberIdFlow,
        previewMembersFlow
    ) { details, isDrawing, highlightedId, previewMembers ->
        if (details == null) {
            CircleDetailUiState(isLoading = false)
        } else {
            val activeMembers = previewMembers ?: details.members.sortedBy { it.payoutOrder }
            val currentRound = RoundCalculator.computeCurrentRoundNumber(details)
            CircleDetailUiState(
                details = details,
                sortedMembers = activeMembers,
                schedule = RoundCalculator.buildSchedule(details),
                currentRoundNumber = currentRound,
                lateMembers = RoundCalculator.getLateMembersForRound(details, currentRound),
                isDrawing = isDrawing,
                orderLocked = details.circle.orderLocked,
                membersComplete = details.members.size >= 2 && details.members.all {
                    it.name.isNotBlank() && it.phone.filter(Char::isDigit).length >= 9
                },
                highlightedMemberId = highlightedId,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CircleDetailUiState()
    )

    fun moveMember(fromIndex: Int, toIndex: Int) {
        if (uiState.value.orderLocked) return
        val currentList = uiState.value.sortedMembers.toMutableList()
        if (fromIndex !in currentList.indices || toIndex !in currentList.indices || fromIndex == toIndex) {
            return
        }
        val moved = currentList.removeAt(fromIndex)
        currentList.add(toIndex, moved)
        previewMembersFlow.value = currentList
        viewModelScope.launch {
            repository.updateMembersPayoutOrder(currentList)
            previewMembersFlow.value = null
        }
    }

    /** اعتماد الترتيب الحالي نهائياً */
    fun confirmOrder() {
        val state = uiState.value
        if (state.orderLocked || !state.membersComplete) return
        viewModelScope.launch {
            repository.updateMembersPayoutOrder(state.sortedMembers)
            repository.lockOrder(circleId)
        }
    }

    fun persistMemberOrder(reordered: List<MemberEntity>) {
        if (uiState.value.orderLocked) return
        previewMembersFlow.value = reordered
        viewModelScope.launch {
            repository.updateMembersPayoutOrder(reordered)
            previewMembersFlow.value = null
        }
    }

    fun runRandomDraw() {
        val state = uiState.value
        val members = state.sortedMembers
        if (members.size < 2 || isDrawingFlow.value || state.orderLocked || !state.membersComplete) return

        viewModelScope.launch {
            isDrawingFlow.value = true
            repeat(12) { step ->
                val shuffledPreview = members.shuffled()
                previewMembersFlow.value = shuffledPreview
                highlightedMemberIdFlow.value = shuffledPreview[step % shuffledPreview.size].id
                delay(90L + (step * 15L))
            }
            val finalOrder = members.shuffled()
            previewMembersFlow.value = finalOrder
            highlightedMemberIdFlow.value = finalOrder.firstOrNull()?.id
            repository.updateMembersPayoutOrder(finalOrder)
            // القرعة تُجرى مرة واحدة فقط: نقفل الترتيب مباشرة
            repository.lockOrder(circleId)
            delay(450L)
            highlightedMemberIdFlow.value = null
            previewMembersFlow.value = null
            isDrawingFlow.value = false
        }
    }

    fun toggleMemberPayment(
        roundNumber: Int,
        memberId: Long,
        isPaid: Boolean,
        paidDateEpochDay: Long? = null
    ) {
        viewModelScope.launch {
            repository.setPaymentStatus(
                circleId = circleId,
                roundNumber = roundNumber,
                memberId = memberId,
                isPaid = isPaid,
                paidDateEpochDay = paidDateEpochDay
            )
        }
    }

    fun getPaymentFor(roundNumber: Int, memberId: Long): RoundPaymentEntity? {
        return uiState.value.details?.payments?.firstOrNull {
            it.roundNumber == roundNumber && it.memberId == memberId
        }
    }

    class Factory(
        private val repository: JamiyatiRepository,
        private val circleId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CircleDetailViewModel(repository, circleId) as T
        }
    }
}
