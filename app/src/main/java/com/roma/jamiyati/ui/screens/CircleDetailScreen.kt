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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
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
                    orderLocked = uiState.orderLocked,
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
                    orderLocked = uiState.orderLocked,
                    membersComplete = uiState.membersComplete,
                    highlightedMemberId = uiState.highlightedMemberId,
                    onMoveMember = viewModel::moveMember,
                    onRandomDraw = viewModel::runRandomDraw,
                    onConfirmOrder = viewModel::confirmOrder
                )
            }
        }
    }
}

@Composable
private fun RoundsScheduleTab(
    schedule: List<RoundScheduleItem>,
    orderLocked: Boolean,
    onOpenRound: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!orderLocked) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠ ترتيب القبض لم يُعتمد بعد. افتح تبويب «ترتيب القبض» واعتمده أو أجرِ القرعة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        itemsIndexed(schedule, key = { _, item -> item.roundNumber }) { _, round ->
            val receiverName = round.receiver?.name ?: "غير محدد"
            val date = DateFormatterAr.formatEpochDay(round.dateEpochDay)
            val faded = round.status == RoundStatus.DONE || round.status == RoundStatus.UPCOMING
            val canOpen = round.status != RoundStatus.UPCOMING
            val containerColor = when (round.status) {
                RoundStatus.CURRENT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                RoundStatus.DONE -> MaterialTheme.colorScheme.surfaceVariant
                RoundStatus.INCOMPLETE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                RoundStatus.UPCOMING -> MaterialTheme.colorScheme.surface
            }
            Card(
                onClick = { onOpenRound(round.roundNumber) },
                enabled = canOpen,
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    disabledContainerColor = containerColor
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (faded) 0.5f else 1f)
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
                            text = when (round.status) {
                                RoundStatus.DONE -> "الجولة ${round.roundNumber} · منتهية ✓"
                                RoundStatus.UPCOMING -> "الجولة ${round.roundNumber} · قادمة 🔒"
                                else -> "الجولة ${round.roundNumber} · ${round.status.labelAr}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (round.status == RoundStatus.INCOMPLETE) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (round.status) {
                                RoundStatus.DONE -> "تم استلام المبلغ: $receiverName"
                                RoundStatus.CURRENT -> "المستحق الآن: $receiverName"
                                else -> "المستحق: $receiverName"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (round.status == RoundStatus.CURRENT) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = if (round.status == RoundStatus.UPCOMING) "تُفتح بتاريخ: $date" else "التاريخ: $date",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (round.status != RoundStatus.UPCOMING) {
                            Text(
                                text = "تم السداد: ${round.paidCount} من ${round.totalMembers}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (round.status == RoundStatus.INCOMPLETE) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.formatLyd(round.totalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val action = when (round.status) {
                            RoundStatus.CURRENT -> "تسجيل الدفعات"
                            RoundStatus.INCOMPLETE -> "إكمال الدفعات"
                            RoundStatus.DONE -> "عرض"
                            RoundStatus.UPCOMING -> null
                        }
                        if (action != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = action,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (round.status == RoundStatus.INCOMPLETE) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.secondary
                            )
                        }
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
            val displayPhone = LibyanPhoneFormatter.formatForDisplay(member.phone)
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
                            text = displayPhone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "المستحق: ${CurrencyFormatter.formatLyd(installmentAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
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
                        Text("تذكير")
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
    orderLocked: Boolean,
    membersComplete: Boolean,
    highlightedMemberId: Long?,
    onMoveMember: (Int, Int) -> Unit,
    onRandomDraw: () -> Unit,
    onConfirmOrder: () -> Unit
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var confirmDraw by remember { mutableStateOf(false) }
    var confirmManual by remember { mutableStateOf(false) }
    val editable = !orderLocked && !isDrawing

    Column(modifier = Modifier.fillMaxSize()) {
        if (orderLocked && !isDrawing) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        text = "🔒 الترتيب معتمد نهائياً",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "لا يمكن تغيير ترتيب القبض أو إعادة القرعة، حفاظاً على حقوق جميع المشتركين.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "رتّب المشتركين بالأسهم ثم اضغط «اعتماد الترتيب»، أو أجرِ قرعة عشوائية.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "تنبيه: الاعتماد أو القرعة يتم مرة واحدة فقط، ولا يمكن تغيير الترتيب بعدها.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                if (!membersComplete) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "أكمل أسماء وأرقام هواتف جميع المشتركين أولاً (زر التعديل ✎ في الأعلى).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { confirmDraw = true },
                        enabled = editable && membersComplete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Casino, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isDrawing) "جاري السحب..." else "قرعة عشوائية")
                    }
                    OutlinedButton(
                        onClick = { confirmManual = true },
                        enabled = editable && membersComplete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("اعتماد الترتيب")
                    }
                }
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

                val dragModifier = if (editable) {
                    Modifier.pointerInput(members.size, index) {
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
                } else Modifier

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
                        .then(dragModifier)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editable) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "اسحب للترتيب",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
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
                                text = LibyanPhoneFormatter.formatForDisplay(member.phone),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        if (editable) {
                            IconButton(
                                onClick = { onMoveMember(index, index - 1) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "تقديم")
                            }
                            IconButton(
                                onClick = { onMoveMember(index, index + 1) },
                                enabled = index < members.lastIndex
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "تأخير")
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDraw) {
        AlertDialog(
            onDismissRequest = { confirmDraw = false },
            title = { Text("إجراء القرعة") },
            text = {
                Text(
                    "القرعة تُجرى مرة واحدة فقط، ونتيجتها نهائية ولا يمكن تغييرها أو إعادتها.\n" +
                        "تأكد أن جميع المشتركين (${members.size}) مضافون بأسمائهم وأرقامهم الصحيحة."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDraw = false
                    onRandomDraw()
                }) { Text("إجراء القرعة") }
            },
            dismissButton = { TextButton(onClick = { confirmDraw = false }) { Text("إلغاء") } }
        )
    }

    if (confirmManual) {
        AlertDialog(
            onDismissRequest = { confirmManual = false },
            title = { Text("اعتماد الترتيب") },
            text = {
                Text(
                    "سيتم اعتماد الترتيب الظاهر نهائياً، ولا يمكن تغييره أو إجراء قرعة بعده.\n" +
                        "الأول في القائمة يقبض في الجولة الأولى، وهكذا."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmManual = false
                    onConfirmOrder()
                }) { Text("اعتماد") }
            },
            dismissButton = { TextButton(onClick = { confirmManual = false }) { Text("إلغاء") } }
        )
    }
}
