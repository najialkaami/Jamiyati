package com.roma.jamiyati.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.roma.jamiyati.data.local.CircleWithDetails
import com.roma.jamiyati.data.repository.JamiyatiRepository
import com.roma.jamiyati.util.RoundCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CircleSummaryUi(
    val id: Long,
    val name: String,
    val installmentAmount: Double,
    val frequencyLabel: String,
    val totalMembers: Int,
    val totalPayout: Double,
    val currentRoundNumber: Int,
    val currentReceiverName: String,
    val paidInCurrentRound: Int,
    val lateCountInCurrentRound: Int
)

data class HomeUiState(
    val circles: List<CircleSummaryUi> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val repository: JamiyatiRepository
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeAllCircles(),
        searchQueryFlow
    ) { circles, query ->
        val summaries = circles
            .filter { query.isBlank() || it.circle.name.contains(query.trim(), ignoreCase = true) }
            .map { it.toSummaryUi() }

        HomeUiState(
            circles = summaries,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
    }

    fun deleteCircle(circleId: Long) {
        viewModelScope.launch {
            repository.deleteCircle(circleId)
        }
    }

    private fun CircleWithDetails.toSummaryUi(): CircleSummaryUi {
        val sortedMembers = members.sortedBy { it.payoutOrder }
        val totalMembers = sortedMembers.size
        val currentRound = RoundCalculator.computeCurrentRoundNumber(this)
        val receiver = sortedMembers.firstOrNull { it.payoutOrder == currentRound }
            ?: sortedMembers.getOrNull((currentRound - 1).coerceAtLeast(0))
        val paidCount = payments.count { it.roundNumber == currentRound && it.isPaid }

        return CircleSummaryUi(
            id = circle.id,
            name = circle.name,
            installmentAmount = circle.installmentAmount,
            frequencyLabel = circle.frequency.labelAr,
            totalMembers = totalMembers,
            totalPayout = circle.installmentAmount * totalMembers,
            currentRoundNumber = currentRound,
            currentReceiverName = receiver?.name ?: "غير محدد",
            paidInCurrentRound = paidCount,
            lateCountInCurrentRound = (totalMembers - paidCount).coerceAtLeast(0)
        )
    }

    class Factory(private val repository: JamiyatiRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
