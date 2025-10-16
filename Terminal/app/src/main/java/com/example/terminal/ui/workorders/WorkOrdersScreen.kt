package com.example.terminal.ui.workorders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.terminal.data.repository.UserStatus
import com.example.terminal.ui.theme.TerminalBackgroundBottom
import com.example.terminal.ui.theme.TerminalBackgroundTop
import com.example.terminal.ui.theme.TerminalHelperText
import com.example.terminal.ui.theme.TerminalKeypadBackground
import com.example.terminal.ui.theme.TerminalKeypadButton
import com.example.terminal.ui.theme.TerminalKeypadClear
import com.example.terminal.ui.theme.TerminalKeypadEnter
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun WorkOrdersScreen(
    viewModel: WorkOrdersViewModel = viewModel(
        factory = WorkOrdersViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(TerminalBackgroundTop, TerminalBackgroundBottom)
                    )
                )
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp, top = 12.dp, end = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(36.dp)
            ) {
                WorkOrdersForm(
                    modifier = Modifier.weight(0.6f),
                    uiState = uiState,
                    onEmployeeClick = viewModel::onEmployeeFieldSelected,
                    onWorkOrderClick = viewModel::onWorkOrderFieldSelected,
                    onEmployeeCardClose = viewModel::onEmployeeCardDismissed,
                    onClockIn = viewModel::onClockIn,
                    onClockOut = viewModel::onClockOutClick
                )

                WorkOrdersKeypad(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight(),
                    onNumberClick = viewModel::setDigit,
                    onClear = viewModel::clear,
                    onEnter = viewModel::enter
                )
            }

            if (uiState.showClockOutDialog) {
                ClockOutDialog(
                    onDismiss = viewModel::dismissClockOutDialog,
                    onConfirm = { qty, status -> viewModel.onClockOut(qty, status) }
                )
            }

            if (uiState.isLoading) {
                LoadingOverlay()
            }
        }
    }
}

@Composable
private fun WorkOrdersForm(
    modifier: Modifier = Modifier,
    uiState: WorkOrdersUiState,
    onEmployeeClick: () -> Unit,
    onWorkOrderClick: () -> Unit,
    onEmployeeCardClose: () -> Unit,
    onClockIn: () -> Unit,
    onClockOut: () -> Unit
) {
    val hasActiveWorkOrder = uiState.userStatus?.activeWorkOrder != null
    val isClockInEnabled = uiState.isEmployeeValidated &&
        uiState.employeeId.isNotBlank() &&
        uiState.workOrderId.isNotBlank() &&
        !uiState.isLoading &&
        !hasActiveWorkOrder
    val isClockOutEnabled = uiState.isEmployeeValidated && hasActiveWorkOrder && !uiState.isLoading
    val employeeInstruction = "Enter your Employee ID and press Enter on the keypad to validate."
    val workOrderInstruction = "Use the keypad to enter or scan the assembly number and press Enter to continue."
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!uiState.isEmployeeValidated) {
            StepHeading(
                title = "Please enter or scan your user ID",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            SelectableField(
                label = "Employee #",
                value = uiState.employeeId,
                isActive = uiState.activeField == WorkOrderInputField.EMPLOYEE,
                onClick = onEmployeeClick,
                enabled = !uiState.isLoading,
                isError = uiState.employeeValidationError != null
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = employeeInstruction,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = TerminalHelperText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.employeeValidationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.employeeValidationError,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                uiState.userStatus?.let { status ->
                    EmployeeStatusCard(
                        status = status,
                        modifier = Modifier.fillMaxWidth(),
                        onClose = onEmployeeCardClose
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (!hasActiveWorkOrder) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StepHeading(
                                title = "Please enter or scan your assembly number",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            SelectableField(
                                label = "Assembly #",
                                value = uiState.workOrderId,
                                isActive = uiState.activeField == WorkOrderInputField.WORK_ORDER,
                                onClick = onWorkOrderClick,
                                enabled = uiState.isEmployeeValidated && !uiState.isLoading
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = workOrderInstruction,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = TerminalHelperText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Text(
                            text = "You are already clocked in on a work order. Please clock out before starting another.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = TerminalHelperText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(34.dp)
        ) {
            Button(
                onClick = onClockIn,
                enabled = isClockInEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = "Clock IN WO",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onClockOut,
                enabled = isClockOutEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isClockOutEnabled) {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                    }
                )
            ) {
                Text(
                    text = "Clock OUT WO",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StepHeading(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = TerminalHelperText,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SelectableField(
    label: String,
    value: String,
    isActive: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isError: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val displayValue = if (value.isBlank()) "" else value
    val shape = RoundedCornerShape(14.dp)
    val inactiveColor = MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.6f else 0.4f)
    val focusedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    val borderColor = when {
        !enabled -> inactiveColor
        isError -> MaterialTheme.colorScheme.error
        isActive || isFocused -> focusedColor
        else -> inactiveColor
    }
    val borderWidth = if (isActive || isFocused) 2.dp else 1.5.dp

    OutlinedTextField(
        value = displayValue,
        onValueChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (enabled) {
                    it.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    it
                }
            }
            .border(BorderStroke(borderWidth, borderColor), shape),
        readOnly = true,
        enabled = enabled,
        isError = isError,
        label = { Text(text = label, fontWeight = FontWeight.Medium) },
        placeholder = { Text(text = "--", fontWeight = FontWeight.Medium, fontSize = 17.sp) },
        singleLine = true,
        shape = shape,
        interactionSource = interactionSource,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            errorBorderColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            errorContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            errorLabelColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = focusedColor,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    )
}

@Composable
private fun EmployeeStatusCard(
    status: UserStatus,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(TerminalKeypadButton, TerminalKeypadBackground)
                    )
                )
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Employee",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                        Text(
                            text = listOfNotNull(status.firstName, status.lastName)
                                .joinToString(separator = " ")
                                .ifBlank { "--" },
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar tarjeta de empleado",
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }

                val workOrder = status.activeWorkOrder
                if (workOrder != null) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        WorkOrderNumberHeading(
                            workOrderNumber = workOrder.workOrderNumber,
                            assemblyNumber = workOrder.workOrderAssemblyNumber
                        )

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        ClockInInfo(clockInTime = workOrder.clockInTime)

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                        WorkOrderDetailsGrid(
                            partNumber = workOrder.partNumber,
                            operationName = workOrder.operationName,
                            operationCode = workOrder.operationCode
                        )
                    }
                } else {
                    Text(
                        text = "No active work order",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkOrderNumberHeading(
    workOrderNumber: String?,
    assemblyNumber: String?
) {
    val displayNumber = remember(workOrderNumber, assemblyNumber) {
        when {
            !workOrderNumber.isNullOrBlank() -> workOrderNumber
            !assemblyNumber.isNullOrBlank() -> assemblyNumber
            else -> null
        }
    } ?: "--"

    Text(
        text = "Work Order # $displayNumber",
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        ),
        color = MaterialTheme.colorScheme.onTertiary
    )
}

@Composable
private fun ClockInInfo(clockInTime: String?) {
    val locale = Locale.getDefault()
    val clockInInstant = remember(clockInTime) { parseClockInInstant(clockInTime) }
    val formattedClockIn = remember(clockInInstant, clockInTime, locale) {
        clockInInstant?.let { formatClockInDisplay(it, locale) }
            ?: clockInTime?.takeIf { it.isNotBlank() }
    }

    var elapsed by remember(clockInInstant) {
        mutableStateOf(clockInInstant?.let { formatElapsed(it) })
    }

    if (clockInInstant != null) {
        LaunchedEffect(clockInInstant) {
            while (true) {
                elapsed = formatElapsed(clockInInstant)
                delay(1_000L)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "CLOCK IN",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            text = formattedClockIn ?: "--",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        val elapsedDisplay = elapsed ?: "--:--:--"
        Text(
            text = "⏱ $elapsedDisplay (since clock-in)",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkOrderDetailsGrid(
    partNumber: String?,
    operationName: String?,
    operationCode: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WorkOrderGridItem(
            label = "Part Number",
            value = partNumber?.takeIf { it.isNotBlank() } ?: "--"
        )

        val operationValue = when {
            !operationName.isNullOrBlank() -> operationName
            !operationCode.isNullOrBlank() -> operationCode
            else -> "--"
        }

        WorkOrderGridItem(
            label = "Operation",
            value = operationValue
        )
    }
}

@Composable
private fun RowScope.WorkOrderGridItem(
    label: String,
    value: String
) {
    val locale = Locale.getDefault()
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label.uppercase(locale),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun parseClockInInstant(clockInTime: String?): Instant? {
    if (clockInTime.isNullOrBlank()) {
        return null
    }

    return runCatching { OffsetDateTime.parse(clockInTime).toInstant() }
        .recoverCatching { Instant.parse(clockInTime) }
        .recoverCatching {
            LocalDateTime.parse(clockInTime)
                .atZone(ZoneId.systemDefault())
                .toInstant()
        }
        .getOrNull()
}

private fun formatClockInDisplay(
    clockInInstant: Instant,
    locale: Locale
): String {
    val zonedDateTime = clockInInstant.atZone(ZoneId.systemDefault())
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    return "${dateFormatter.format(zonedDateTime)} • ${timeFormatter.format(zonedDateTime)}"
}

private fun formatElapsed(clockInInstant: Instant): String {
    val duration = Duration.between(clockInInstant, Instant.now())
    val safeDuration = if (duration.isNegative) Duration.ZERO else duration
    val totalSeconds = safeDuration.seconds
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
private fun WorkOrdersKeypad(
    modifier: Modifier = Modifier,
    onNumberClick: (String) -> Unit,
    onClear: () -> Unit,
    onEnter: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = TerminalKeypadBackground,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            val keypadItems = buildList {
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9").forEach { digit ->
                    add(
                        KeypadItem(
                            label = digit,
                            onClick = { onNumberClick(digit) },
                            backgroundColor = TerminalKeypadButton,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                    )
                }
                add(
                    KeypadItem(
                        label = "Clear",
                        onClick = onClear,
                        backgroundColor = TerminalKeypadClear,
                        contentColor = TerminalHelperText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                        letterSpacing = 0.5.sp
                    )
                )
                add(
                    KeypadItem(
                        label = "0",
                        onClick = { onNumberClick("0") },
                        backgroundColor = TerminalKeypadButton,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                )
                add(
                    KeypadItem(
                        label = "Enter",
                        onClick = onEnter,
                        backgroundColor = TerminalKeypadEnter,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        borderColor = Color.Transparent,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(keypadItems, key = { it.label }) { item ->
                    KeypadButton(item)
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    item: KeypadItem
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(10.dp),
        color = item.backgroundColor,
        contentColor = item.contentColor,
        shadowElevation = 5.dp,
        tonalElevation = 1.5.dp,
        border = item.borderColor?.let { BorderStroke(1.dp, it) },
        onClick = item.onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.label,
                fontSize = item.fontSize,
                textAlign = TextAlign.Center,
                color = item.contentColor,
                fontWeight = item.fontWeight,
                letterSpacing = item.letterSpacing
            )
        }
    }
}

private data class KeypadItem(
    val label: String,
    val onClick: () -> Unit,
    val backgroundColor: Color,
    val contentColor: Color,
    val fontSize: TextUnit,
    val fontWeight: FontWeight,
    val borderColor: Color?,
    val letterSpacing: TextUnit
)

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

