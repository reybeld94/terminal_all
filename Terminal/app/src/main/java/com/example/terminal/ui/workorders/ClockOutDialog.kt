package com.example.terminal.ui.workorders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.terminal.data.network.ClockOutStatus
import com.example.terminal.ui.theme.TerminalKeypadBackground
import com.example.terminal.ui.theme.TerminalKeypadButton
import com.example.terminal.ui.theme.TerminalKeypadEnter

@Composable
fun ClockOutDialog(
    modifier: Modifier = Modifier,
    quantity: String,
    selectedStatus: ClockOutStatus,
    onQuantityChange: (String) -> Unit,
    onStatusSelected: (ClockOutStatus) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val dialogShape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(TerminalKeypadButton, TerminalKeypadBackground)
                    ),
                    shape = dialogShape
                )
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Clock Out Work Order",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val crossColor = MaterialTheme.colorScheme.onSurfaceVariant
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Canvas(modifier = Modifier.size(32.dp)) {
                            val strokeWidth = 4.dp.toPx()
                            drawLine(
                                color = crossColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = crossColor,
                                start = Offset(size.width, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { input -> onQuantityChange(input.filter { it.isDigit() }) },
                        label = { Text(text = "Quantity") },
                        placeholder = { Text(text = "0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    )

                    StatusButtons(
                        selectedStatus = selectedStatus,
                        onStatusSelected = { status ->
                            onStatusSelected(status)
                            if (quantity.isNotBlank()) {
                                onConfirm()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusButtons(
    selectedStatus: ClockOutStatus,
    onStatusSelected: (ClockOutStatus) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusButton(
            modifier = Modifier.weight(1f),
            label = ClockOutStatus.COMPLETE.displayName,
            icon = Icons.Filled.CheckCircle,
            isSelected = selectedStatus == ClockOutStatus.COMPLETE,
            onClick = { onStatusSelected(ClockOutStatus.COMPLETE) }
        )

        StatusButton(
            modifier = Modifier.weight(1f),
            label = ClockOutStatus.INCOMPLETE.displayName,
            icon = Icons.Filled.Cancel,
            isSelected = selectedStatus == ClockOutStatus.INCOMPLETE,
            onClick = { onStatusSelected(ClockOutStatus.INCOMPLETE) }
        )
    }
}

@Composable
private fun StatusButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        TerminalKeypadEnter
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Button(
        modifier = modifier.height(72.dp),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}
