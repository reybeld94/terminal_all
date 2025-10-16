package com.example.terminal.ui.workorders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.terminal.data.network.ClockOutStatus
import com.example.terminal.ui.theme.TerminalKeypadBackground
import com.example.terminal.ui.theme.TerminalKeypadButton

@Composable
fun ClockOutDialog(
    onDismiss: () -> Unit,
    onConfirm: (qty: String, status: ClockOutStatus) -> Unit
) {
    var qty by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf(ClockOutStatus.COMPLETE) }

    Dialog(onDismissRequest = onDismiss) {
        val dialogShape = RoundedCornerShape(28.dp)
        Surface(
            modifier = Modifier,
            shape = dialogShape,
            tonalElevation = 12.dp,
            shadowElevation = 12.dp,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(TerminalKeypadButton, TerminalKeypadBackground)
                        )
                    )
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Clock OUT WO",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { input -> qty = input.filter { it.isDigit() } },
                            label = { Text(text = "Cantidad") },
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
                            onStatusSelected = { status -> selectedStatus = status }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(text = "Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        TextButton(
                            onClick = { onConfirm(qty, selectedStatus) },
                            enabled = qty.isNotBlank()
                        ) {
                            Text(text = "Confirm")
                        }
                    }
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
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Button(
        modifier = modifier,
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
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
