package com.example.terminal

import android.media.AudioAttributes
import android.media.SoundPool
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import com.example.terminal.data.local.UserPrefs
import com.example.terminal.data.network.ApiClient
import com.example.terminal.ui.theme.TerminalTheme
import com.example.terminal.ui.workorders.WorkOrdersScreen
import java.util.ArrayList
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TerminalApp() {
    val context = LocalContext.current
    val tabs = TerminalTab.values()
    var selectedTab by rememberSaveable { mutableStateOf(TerminalTab.CLOCK) }
    val loginStates = rememberSaveable(saver = LoginStateSaver) { mutableStateMapOf<Int, Boolean>() }
    val materialsStates = rememberSaveable(saver = MaterialsStateSaver) {
        mutableStateMapOf<Int, MutableList<String>>()
    }
    val userPrefs = remember { UserPrefs.create(context) }
    val serverAddress by userPrefs.serverAddress.collectAsState(initial = ApiClient.DEFAULT_BASE_URL)
    val beepVolume by userPrefs.beepVolume.collectAsState(initial = 1f)
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(serverAddress) {
        ApiClient.updateBaseUrl(serverAddress)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val selectedIndex = tabs.indexOf(selectedTab)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabRow(
                    selectedTabIndex = selectedIndex,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    indicator = { tabPositions ->
                        if (tabPositions.isNotEmpty()) {
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = index == selectedIndex
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.title.uppercase(),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    }
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.onSurface,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.offset(y = (-4).dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Configurar servidor",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    TerminalTab.CLOCK -> ClockTabContent(
                        loginStates = loginStates,
                        beepVolume = beepVolume,
                        modifier = Modifier.fillMaxSize()
                    )

                    TerminalTab.WORK_ORDERS -> WorkOrdersScreen()

                    TerminalTab.ISSUE_MATERIALS -> IssueMaterialsTabContent(
                        materialsStates = materialsStates,
                        beepVolume = beepVolume,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (showSettingsDialog) {
            ServerSettingsDialog(
                initialAddress = serverAddress,
                initialVolume = beepVolume,
                onDismiss = { showSettingsDialog = false },
                onSave = { newAddress, newVolume ->
                    showSettingsDialog = false
                    coroutineScope.launch {
                        userPrefs.saveServerAddress(newAddress)
                        userPrefs.saveBeepVolume(newVolume)
                    }
                }
            )
        }
    }
}

private enum class TerminalTab(val title: String) {
    CLOCK("Clock In/Out"),
    WORK_ORDERS("Work Orders"),
    ISSUE_MATERIALS("Issue Materials")
}

@Composable
private fun ClockTabContent(
    loginStates: SnapshotStateMap<Int, Boolean>,
    beepVolume: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputValue by rememberSaveable { mutableStateOf("") }
    var lastEmployee by rememberSaveable { mutableStateOf<Int?>(null) }

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Clock In/Out",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            DisplayValue(
                label = "Empleado",
                value = inputValue.ifEmpty { lastEmployee?.toString().orEmpty() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            val statusText = lastEmployee?.let { employee ->
                if (loginStates[employee] == true) "Clocked In" else "Clocked Out"
            } ?: "--"
            DisplayValue(label = "Estado", value = statusText)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ingrese el número de empleado y presione Enter para clock in/out.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        NumericKeypad(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(24.dp),
            beepVolume = beepVolume,
            onNumberClick = { digit -> inputValue += digit },
            onClear = { inputValue = "" },
            onEnter = {
                val employeeNumber = inputValue.toIntOrNull()
                if (employeeNumber == null) {
                    Toast.makeText(context, "Ingrese un número de empleado", Toast.LENGTH_SHORT).show()
                } else {
                    val isLoggedIn = loginStates[employeeNumber] == true
                    if (isLoggedIn) {
                        Toast.makeText(context, "Clock Out Successful", Toast.LENGTH_SHORT).show()
                        loginStates.remove(employeeNumber)
                    } else {
                        Toast.makeText(context, "Clock In Successful", Toast.LENGTH_SHORT).show()
                        loginStates[employeeNumber] = true
                    }
                    lastEmployee = employeeNumber
                }
                inputValue = ""
            }
        )
    }
}

@Composable
private fun ServerSettingsDialog(
    initialAddress: String,
    initialVolume: Float,
    onDismiss: () -> Unit,
    onSave: (String, Float) -> Unit
) {
    var address by rememberSaveable(initialAddress) { mutableStateOf(initialAddress) }
    var volume by rememberSaveable(initialVolume) { mutableStateOf(initialVolume) }
    val isValid = address.trim().isNotEmpty()
    val volumePercentage = (volume * 100).roundToInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Configuración del servidor") },
        text = {
            Column {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Configura la dirección del servidor utilizada para las peticiones.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(text = "Dirección del servidor") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Volumen del beep: $volumePercentage%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            valueRange = 0f..1f
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(address.trim(), volume) },
                enabled = isValid
            ) {
                Text(text = "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}

@Composable
private fun IssueMaterialsTabContent(
    materialsStates: SnapshotStateMap<Int, MutableList<String>>,
    beepVolume: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputValue by rememberSaveable { mutableStateOf("") }
    var employeeNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var materialCode by rememberSaveable { mutableStateOf<String?>(null) }
    var currentField by rememberSaveable { mutableStateOf(MaterialInputField.EMPLOYEE) }

    val instructionText = when (currentField) {
        MaterialInputField.EMPLOYEE -> "Ingrese o escanee el número de empleado y presione Enter."
        MaterialInputField.MATERIAL -> "Ingrese o escanee el código de material y presione Enter."
    }

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Issue Materials",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            DisplayValue(label = "Employee", value = employeeNumber?.toString().orEmpty())
            Spacer(modifier = Modifier.height(8.dp))
            DisplayValue(label = "Material", value = materialCode.orEmpty())
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = instructionText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val employee = employeeNumber
                    val material = materialCode
                    if (employee == null) {
                        Toast.makeText(context, "Ingrese un número de empleado", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (material.isNullOrEmpty()) {
                        Toast.makeText(context, "Ingrese un código de material", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val updatedList = materialsStates[employee]?.toMutableList() ?: mutableListOf()
                    updatedList.add(material)
                    materialsStates[employee] = updatedList

                    Toast.makeText(
                        context,
                        "Employee $employee issued Material $material",
                        Toast.LENGTH_SHORT
                    ).show()

                    materialCode = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Issue Material",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        NumericKeypad(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(24.dp),
            beepVolume = beepVolume,
            onNumberClick = { digit -> inputValue += digit },
            onClear = { inputValue = "" },
            onEnter = {
                when (currentField) {
                    MaterialInputField.EMPLOYEE -> {
                        val employee = inputValue.toIntOrNull()
                        if (employee == null) {
                            Toast.makeText(
                                context,
                                "Ingrese un número de empleado",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            employeeNumber = employee
                            currentField = MaterialInputField.MATERIAL
                            inputValue = ""
                        }
                    }

                    MaterialInputField.MATERIAL -> {
                        if (inputValue.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Ingrese un código de material",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            materialCode = inputValue
                            currentField = MaterialInputField.EMPLOYEE
                            inputValue = ""
                        }
                    }
                }
            }
        )
    }
}

private enum class MaterialInputField {
    EMPLOYEE,
    MATERIAL
}

@Composable
private fun DisplayValue(label: String, value: String) {
    val displayValue = value.ifEmpty { "--" }
    val isEmployeeLabel = label.equals("Empleado", ignoreCase = true) || label.equals("Employee", ignoreCase = true)

    if (isEmployeeLabel) {
        val avatarText = displayValue
            .takeIf { it.isNotBlank() && it != "--" }
            ?.trim()
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.take(2)
            ?.joinToString(separator = "") { it.first().toString() }
            ?.uppercase()
            ?: "--"

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayValue,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    } else {
        Text(
            text = "$label: $displayValue",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NumericKeypad(
    modifier: Modifier = Modifier,
    beepVolume: Float = 1f,
    onNumberClick: (String) -> Unit,
    onClear: () -> Unit,
    onEnter: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val soundPool = remember {
            SoundPool.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setMaxStreams(1)
                .build()
        }
        var beepSoundId by remember { mutableStateOf(0) }

        DisposableEffect(soundPool, context) {
            val loadId = soundPool.load(context, R.raw.beep, 1)
            val listener = SoundPool.OnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && sampleId == loadId) {
                    beepSoundId = sampleId
                }
            }
            soundPool.setOnLoadCompleteListener(listener)

            onDispose {
                soundPool.setOnLoadCompleteListener(null)
                if (loadId != 0) {
                    soundPool.unload(loadId)
                }
                soundPool.release()
            }
        }

        fun playBeep() {
            if (beepSoundId != 0) {
                soundPool.play(beepSoundId, beepVolume, beepVolume, 1, 0, 1f)
            }
        }

        val keypadItems = listOf(
            "1" to {
                playBeep()
                onNumberClick("1")
            },
            "2" to {
                playBeep()
                onNumberClick("2")
            },
            "3" to {
                playBeep()
                onNumberClick("3")
            },
            "4" to {
                playBeep()
                onNumberClick("4")
            },
            "5" to {
                playBeep()
                onNumberClick("5")
            },
            "6" to {
                playBeep()
                onNumberClick("6")
            },
            "7" to {
                playBeep()
                onNumberClick("7")
            },
            "8" to {
                playBeep()
                onNumberClick("8")
            },
            "9" to {
                playBeep()
                onNumberClick("9")
            },
            "Clear" to {
                playBeep()
                onClear()
            },
            "0" to {
                playBeep()
                onNumberClick("0")
            },
            "Enter" to {
                playBeep()
                onEnter()
            }
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            userScrollEnabled = false
        ) {
            items(keypadItems, key = { item -> item.first }) { item ->
                val (label, action) = item
                KeypadButton(
                    label = label,
                    onClick = action
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxSize(fraction = 0.9f),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 4.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

private val LoginStateSaver:
    Saver<SnapshotStateMap<Int, Boolean>, ArrayList<Pair<Int, Boolean>>> = Saver(
        save = { stateMap ->
            ArrayList(stateMap.map { it.key to it.value })
        },
        restore = { restoredList ->
            mutableStateMapOf<Int, Boolean>().apply {
                restoredList.forEach { (employee, isLoggedIn) ->
                    this[employee] = isLoggedIn
                }
            }
        }
    )

private val MaterialsStateSaver:
    Saver<SnapshotStateMap<Int, MutableList<String>>, ArrayList<Pair<Int, ArrayList<String>>>> = Saver(
        save = { stateMap ->
            ArrayList(stateMap.map { (employee, materials) ->
                employee to ArrayList(materials)
            })
        },
        restore = { restoredList ->
            mutableStateMapOf<Int, MutableList<String>>().apply {
                restoredList.forEach { (employee, materials) ->
                    this[employee] = materials.toMutableList()
                }
            }
        }
    )

@Preview(showBackground = true, widthDp = 900, heightDp = 450)
@Composable
private fun TerminalAppPreview() {
    TerminalTheme {
        TerminalApp()
    }
}
