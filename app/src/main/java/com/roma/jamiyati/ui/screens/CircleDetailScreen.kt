package com.roma.jamiyati.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roma.jamiyati.data.local.MemberEntity
import com.roma.jamiyati.ui.viewmodel.CircleDetailViewModel
import com.roma.jamiyati.util.CurrencyFormatter
import com.roma.jamiyati.util.DateFormatterAr
import com.roma.jamiyati.util.LibyanPhoneFormatter
import com.roma.jamiyati.util.PdfReportExporter
import com.roma.jamiyati.util.RoundScheduleItem
import com.roma.jamiyati.util.RoundStatus
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleDetailScreen(
    viewModel: CircleDetailViewModel,
    onBack: () -> Unit,
    onEditCircle: () -> Unit,
    onOpenRound: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("جدول الجولات", "المتأخرون", "ترتيب القبض")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.details?.circle?.name ?: "تفاصيل الجمعية") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    uiState.details?.let { details ->
                        IconButton(
                            onClick = {
                                PdfReportExporter.exportAndShareCircleReport(context, details)
                                    .onFailure {
                                        Toast.makeText(context, "تعذر إنشاء التقرير", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = "تصدير PDF")
                        }
                    }
                    IconButton(onClick = onEditCircle) {
                        Icon(Icons.Rounded.Edit, contentDescription = "تعديل الجمعية")
                    }
                }
            )
        }
    ) { innerPadding ->
        val details = uiState.details
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (details == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("الجمعية غير موجودة")
            }
            return@Scaffold
        }

        val totalPayout = details.circle.installmentAmount * uiState.sortedMembers.size

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "إجمالي القبض",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = CurrencyFormatter.formatLyd(totalPayout),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "القسط: ${CurrencyFormatter.formatLyd(details.circle.installmentAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "الجولة الحالية: ${uiState.currentRoundNumber} من ${uiState.sortedMembers.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    val label = if (index == 1 && uiState.lateMembers.isNotEmpty()) {
                        "$title (${uiState.lateMembers.size})"
                    } else title
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) }
                    )
                }
            }

            when (selectedTab) {
                0 -> RoundsScheduleTab(
                    schedule = uiState.schedule,
                    onOpenRound = onOpenRound
                )
                1 -> LateMembersTab(
                    circleName = details.circle.name,
                    roundNumber = uiState.currentRoundNumber,
                    installmentAmount = details.circle.installmentAmount,
                    lateMembers = uiState.lateMembers
                )
                2 -> PayoutOrderTab(
                    members = uiState.sortedMembers,
                    isDrawing = uiState.isDrawing,
                    highlightedMemberId = uiState.highlightedMemberId,
                    onMoveMember = viewModel::moveMember,
                    onRandomDraw = viewModel::runRandomDraw
                )
            }
        }
    }
}

@Composable
private fun RoundsScheduleTab(
    schedule: List<RoundScheduleItem>,
    onOpenRound: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(schedule, key = { _, item -> item.roundNumber }) { _, round ->
            val containerColor = when (round.status) {
                RoundStatus.CURRENT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                RoundStatus.DONE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                RoundStatus.UPCOMING -> MaterialTheme.colorScheme.surface
            }
            Card(
                onClick = { onOpenRound(round.roundNumber) },
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الجولة ${round.roundNumber} · ${round.status.labelAr}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "المستلم: ${round.receiver?.name ?: "غير محدد"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "التاريخ: ${DateFormatterAr.formatEpochDay(round.dateEpochDay)} · تم السداد: ${round.paidCount}/${round.totalMembers}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.formatLyd(round.totalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تسجيل الدفعات",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LateMembersTab(
    circleName: String,
    roundNumber: Int,
    installmentAmount: Double,
    lateMembers: List<MemberEntity>
) {
    val context = LocalContext.current

    if (lateMembers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "جميع المشتركين سددوا قسط الجولة الحالية",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "المتأخرون في الجولة رقم $roundNumber",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        itemsIndexed(lateMembers, key = { _, m -> m.id }) { _, member ->
            val intlPhone = LibyanPhoneFormatter.formatToLibyanInternational(member.phone)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "$intlPhone · ${CurrencyFormatter.formatLyd(installmentAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            val opened = LibyanPhoneFormatter.openWhatsAppReminder(
                                context = context,
                                rawPhone = member.phone,
                                memberName = member.name,
                                circleName = circleName,
                                roundNumber = roundNumber,
                                installmentAmount = installmentAmount
                            )
                            if (!opened) {
                                Toast.makeText(
                                    context,
                                    "تعذر فتح واتساب، تأكد من الرقم الليبي",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تذكير عبر واتساب")
                    }
                }
            }
        }
    }
}

@Composable
private fun PayoutOrderTab(
    members: List<MemberEntity>,
    isDrawing: Boolean,
    highlightedMemberId: Long?,
    onMoveMember: (Int, Int) -> Unit,
    onRandomDraw: () -> Unit
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "اسحب مطولاً لإعادة الترتيب أو أجرِ قرعة",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRandomDraw,
                enabled = !isDrawing && members.size >= 2
            ) {
                Icon(Icons.Rounded.Casino, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isDrawing) "جاري السحب..." else "قرعة عشوائية")
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(members, key = { _, m -> m.id }) { index, member ->
                val isHighlighted = highlightedMemberId == member.id
                val isDragging = draggingIndex == index
                val scale by animateFloatAsState(
                    targetValue = if (isHighlighted || isDragging) 1.03f else 1f,
                    label = "drawScale"
                )
                val bgColor by animateColorAsState(
                    targetValue = when {
                        isHighlighted -> MaterialTheme.colorScheme.secondaryContainer
                        isDragging -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                    label = "drawColor"
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .offset {
                            IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0)
                        }
                        .scale(scale)
                        .pointerInput(members.size, index) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val thresholdPx = 110f
                                    if (dragOffsetY > thresholdPx && index < members.lastIndex) {
                                        onMoveMember(index, index + 1)
                                        draggingIndex = index + 1
                                        dragOffsetY = 0f
                                    } else if (dragOffsetY < -thresholdPx && index > 0) {
                                        onMoveMember(index, index - 1)
                                        draggingIndex = index - 1
                                        dragOffsetY = 0f
                                    }
                                },
                                onDragEnd = {
                                    draggingIndex = null
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffsetY = 0f
                                }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = "اسحب للترتيب",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${index + 1}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = LibyanPhoneFormatter.formatToLibyanInternational(member.phone),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onMoveMember(index, index - 1) },
                            enabled = index > 0 && !isDrawing
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "تقديم")
                        }
                        IconButton(
                            onClick = { onMoveMember(index, index + 1) },
                            enabled = index < members.lastIndex && !isDrawing
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "تأخير")
                        }
                    }
                }
            }
        }
    }
}
