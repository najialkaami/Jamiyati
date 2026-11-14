package com.roma.jamiyati.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roma.jamiyati.ui.viewmodel.CircleDetailViewModel
import com.roma.jamiyati.util.CurrencyFormatter
import com.roma.jamiyati.util.DateFormatterAr
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundPaymentsScreen(
    viewModel: CircleDetailViewModel,
    roundNumber: Int,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dateEditMemberId by remember { mutableStateOf<Long?>(null) }

    val roundItem = uiState.schedule.firstOrNull { it.roundNumber == roundNumber }
    val installment = uiState.details?.circle?.installmentAmount ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دفعات الجولة $roundNumber") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            roundItem?.let { item ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "المستلم: ${item.receiver?.name ?: "غير محدد"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تاريخ الجولة: ${DateFormatterAr.formatEpochDay(item.dateEpochDay)} · الإجمالي: ${CurrencyFormatter.formatLyd(item.totalAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "تم الدفع: ${item.paidCount} من ${item.totalMembers} (القسط: ${CurrencyFormatter.formatLyd(installment)})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.sortedMembers, key = { it.id }) { member ->
                    val payment = viewModel.getPaymentFor(roundNumber, member.id)
                    val isPaid = payment?.isPaid == true
                    val paidDate = payment?.paidDateEpochDay

                    Card(
                        onClick = {
                            viewModel.toggleMemberPayment(
                                roundNumber = roundNumber,
                                memberId = member.id,
                                isPaid = !isPaid
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPaid) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isPaid,
                                onCheckedChange = { checked ->
                                    viewModel.toggleMemberPayment(
                                        roundNumber = roundNumber,
                                        memberId = member.id,
                                        isPaid = checked
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (isPaid && paidDate != null) {
                                        "مدفوع بتاريخ ${DateFormatterAr.formatEpochDay(paidDate)}"
                                    } else {
                                        "غير مدفوع"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isPaid) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                            if (isPaid) {
                                IconButton(onClick = { dateEditMemberId = member.id }) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarToday,
                                        contentDescription = "تغيير تاريخ السداد"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    dateEditMemberId?.let { memberId ->
        val existingDate = viewModel.getPaymentFor(roundNumber, memberId)?.paidDateEpochDay
            ?: LocalDate.now().toEpochDay()
        val initialMillis = LocalDate.ofEpochDay(existingDate)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { dateEditMemberId = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            viewModel.toggleMemberPayment(
                                roundNumber = roundNumber,
                                memberId = memberId,
                                isPaid = true,
                                paidDateEpochDay = picked.toEpochDay()
                            )
                        }
                        dateEditMemberId = null
                    }
                ) { Text("حفظ التاريخ") }
            },
            dismissButton = {
                TextButton(onClick = { dateEditMemberId = null }) { Text("إلغاء") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
