package com.example.terminal.ui.workorders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.terminal.data.local.UserPrefs
import com.example.terminal.data.network.ClockOutStatus
import com.example.terminal.data.repository.UserStatus
import com.example.terminal.data.repository.WorkOrdersRepository
import com.example.terminal.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkOrderInputField {
    EMPLOYEE,
    WORK_ORDER,
    CLOCK_OUT_QTY
}

data class WorkOrdersUiState(
    val employeeId: String = "",
    val workOrderId: String = "",
    val activeField: WorkOrderInputField = WorkOrderInputField.EMPLOYEE,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null,
    val showClockOutDialog: Boolean = false,
    val isEmployeeValidated: Boolean = false,
    val employeeValidationError: String? = null,
    val userStatus: UserStatus? = null,
    val clockOutQuantity: String = "",
    val clockOutStatus: ClockOutStatus = ClockOutStatus.COMPLETE
)

private const val WORK_ORDER_TIMEOUT_MS = 20_000L

class WorkOrdersViewModel(
    private val repository: WorkOrdersRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkOrdersUiState())
    val uiState: StateFlow<WorkOrdersUiState> = _uiState.asStateFlow()

    private var saveEmployeeJob: Job? = null
    private var workOrderTimeoutJob: Job? = null

    init {
        viewModelScope.launch {
            userPrefs.lastEmployeeId.collectLatest { storedEmployee ->
                if (!storedEmployee.isNullOrBlank()) {
                    _uiState.update { current ->
                        if (current.employeeId.isBlank()) {
                            current.copy(employeeId = storedEmployee)
                        } else {
                            current
                        }
                    }
                }
            }
        }
    }

    fun onEmployeeFieldSelected() {
        _uiState.update { it.copy(activeField = WorkOrderInputField.EMPLOYEE) }
    }

    fun onWorkOrderFieldSelected() {
        if (_uiState.value.isEmployeeValidated) {
            _uiState.update { it.copy(activeField = WorkOrderInputField.WORK_ORDER) }
        }
    }

    fun onEmployeeCardDismissed() {
        resetToEmployeeStep()
    }

    fun setDigit(digit: String) {
        require(digit.length == 1 && digit[0].isDigit())
        when (_uiState.value.activeField) {
            WorkOrderInputField.EMPLOYEE -> {
                cancelWorkOrderTimeout()
                updateEmployeeId(_uiState.value.employeeId + digit)
            }

            WorkOrderInputField.WORK_ORDER -> {
                cancelWorkOrderTimeout()
                updateWorkOrderId(_uiState.value.workOrderId + digit)
            }

            WorkOrderInputField.CLOCK_OUT_QTY -> {
                updateClockOutQuantity(_uiState.value.clockOutQuantity + digit)
            }
        }
    }

    fun clear() {
        when (_uiState.value.activeField) {
            WorkOrderInputField.EMPLOYEE -> {
                val updated = _uiState.value.employeeId.dropLast(1)
                updateEmployeeId(updated)
            }

            WorkOrderInputField.WORK_ORDER -> {
                val updated = _uiState.value.workOrderId.dropLast(1)
                updateWorkOrderId(updated)
            }

            WorkOrderInputField.CLOCK_OUT_QTY -> {
                val updated = _uiState.value.clockOutQuantity.dropLast(1)
                updateClockOutQuantity(updated)
            }
        }
    }

    fun enter() {
        when (_uiState.value.activeField) {
            WorkOrderInputField.EMPLOYEE -> {
                if (_uiState.value.isEmployeeValidated) {
                    _uiState.update { it.copy(activeField = WorkOrderInputField.WORK_ORDER) }
                } else {
                    validateEmployee()
                }
            }

            WorkOrderInputField.WORK_ORDER -> {
                _uiState.update { it.copy(activeField = WorkOrderInputField.EMPLOYEE) }
            }

            WorkOrderInputField.CLOCK_OUT_QTY -> {
                onClockOutConfirm()
            }
        }
    }

    fun onClockIn() {
        val employee = _uiState.value.employeeId.trim()
        val workOrder = _uiState.value.workOrderId.trim()
        val userId = _uiState.value.userStatus?.userId

        if (!_uiState.value.isEmployeeValidated) {
            showMessage("Valide el empleado antes de continuar")
            return
        }

        if (employee.isEmpty() || workOrder.isEmpty()) {
            showMessage("Employee # y Work Order # son requeridos")
            return
        }

        if (!employee.isDigitsOnly() || !workOrder.isDigitsOnly()) {
            showMessage("Ingrese valores numéricos válidos")
            return
        }

        if (userId == null) {
            showMessage("No se pudo obtener el usuario. Valide nuevamente.")
            return
        }

        val workOrderId = workOrder.toInt()

        setLoading(true)
        viewModelScope.launch {
            val result = repository.clockIn(workOrderId, userId)
            result.fold(
                onSuccess = {
                    val refreshed = refreshUserStatus(employee)
                    if (refreshed) {
                        showMessage("Clock In registrado correctamente")
                    } else {
                        showMessage("Clock In registrado correctamente, pero no se pudo actualizar la información")
                    }
                },
                onFailure = { error ->
                    showMessage(error.message ?: "Error al registrar Clock In")
                }
            )
            setLoading(false)
        }
    }

    fun onClockOutClick() {
        val state = _uiState.value
        val employee = state.employeeId.trim()
        val workOrder = state.workOrderId.trim()
        val activeWorkOrderId = state.userStatus?.activeWorkOrder?.workOrderCollectionId

        if (!state.isEmployeeValidated) {
            showMessage("Valide el empleado antes de continuar")
            return
        }
        if (employee.isEmpty()) {
            showMessage("Employee # y Work Order # son requeridos")
            return
        }
        if (activeWorkOrderId == null) {
            if (workOrder.isEmpty()) {
                showMessage("Employee # y Work Order # son requeridos")
                return
            }
            if (!workOrder.isDigitsOnly()) {
                showMessage("Ingrese valores numéricos válidos")
                return
            }
        }
        cancelWorkOrderTimeout()
        _uiState.update {
            it.copy(
                showClockOutDialog = true,
                activeField = WorkOrderInputField.CLOCK_OUT_QTY,
                clockOutQuantity = "",
                clockOutStatus = ClockOutStatus.COMPLETE
            )
        }
    }

    fun onClockOutQuantityChange(value: String) {
        updateClockOutQuantity(value)
    }

    fun onClockOutStatusSelected(status: ClockOutStatus) {
        _uiState.update { it.copy(clockOutStatus = status) }
    }

    fun onClockOutConfirm() {
        val state = _uiState.value
        val employee = state.employeeId.trim()
        val workOrder = state.workOrderId.trim()
        val activeWorkOrderId = state.userStatus?.activeWorkOrder?.workOrderCollectionId

        val quantityText = state.clockOutQuantity
        val quantity = quantityText.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            showMessage("Ingrese una cantidad válida mayor a 0")
            return
        }

        if (!employee.isDigitsOnly()) {
            showMessage("Ingrese valores numéricos válidos")
            return
        }

        val workOrderId = activeWorkOrderId ?: workOrder.toIntOrNull()
        if (workOrderId == null) {
            showMessage("Employee # y Work Order # son requeridos")
            return
        }

        val status = state.clockOutStatus
        setLoading(true)
        _uiState.update {
            it.copy(
                showClockOutDialog = false,
                clockOutQuantity = "",
                clockOutStatus = ClockOutStatus.COMPLETE,
                activeField = nextActiveFieldAfterClockOut(it)
            )
        }
        viewModelScope.launch {
            val result = repository.clockOut(
                workOrderCollectionId = workOrderId,
                quantity = quantity,
                complete = status.isComplete
            )
            result.fold(
                onSuccess = {
                    _uiState.update { current ->
                        val updatedStatus = current.userStatus?.copy(activeWorkOrder = null)
                        current.copy(
                            userStatus = updatedStatus,
                            workOrderId = "",
                            activeField = WorkOrderInputField.WORK_ORDER,
                            clockOutQuantity = "",
                            clockOutStatus = ClockOutStatus.COMPLETE
                        )
                    }
                    showMessage("Clock Out registrado correctamente")
                },
                onFailure = { error ->
                    showMessage(error.message ?: "Error al registrar Clock Out")
                }
            )
            setLoading(false)
            startWorkOrderTimeout()
        }
    }

    fun dismissClockOutDialog() {
        _uiState.update {
            it.copy(
                showClockOutDialog = false,
                clockOutQuantity = "",
                clockOutStatus = ClockOutStatus.COMPLETE,
                activeField = nextActiveFieldAfterClockOut(it)
            )
        }
        startWorkOrderTimeout()
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun updateEmployeeId(value: String) {
        cancelWorkOrderTimeout()
        _uiState.update {
            it.copy(
                employeeId = value,
                isEmployeeValidated = false,
                employeeValidationError = null,
                userStatus = null,
                showClockOutDialog = false,
                clockOutQuantity = "",
                clockOutStatus = ClockOutStatus.COMPLETE,
                activeField = WorkOrderInputField.EMPLOYEE
            )
        }
        saveEmployeeJob?.cancel()
        if (value.isNotBlank()) {
            saveEmployeeJob = viewModelScope.launch {
                userPrefs.saveLastEmployeeId(value)
            }
        }
    }

    private fun updateWorkOrderId(value: String) {
        _uiState.update { it.copy(workOrderId = value) }
        if (value.isBlank()) {
            if (_uiState.value.isEmployeeValidated) {
                startWorkOrderTimeout()
            }
        } else {
            cancelWorkOrderTimeout()
        }
    }

    private fun updateClockOutQuantity(value: String) {
        val filtered = value.filter(Char::isDigit)
        _uiState.update {
            it.copy(
                clockOutQuantity = filtered,
                activeField = WorkOrderInputField.CLOCK_OUT_QTY
            )
        }
    }

    private fun validateEmployee() {
        val employee = _uiState.value.employeeId.trim()
        if (employee.isEmpty()) {
            showMessage("Ingrese el número de empleado")
            return
        }

        if (!employee.isDigitsOnly()) {
            showMessage("Ingrese un número de empleado válido")
            return
        }

        setLoading(true)
        viewModelScope.launch {
            val result = repository.fetchUserStatus(employee)
            result.fold(
                onSuccess = { status ->
                    val activeWorkOrder = status.activeWorkOrder
                    _uiState.update {
                        it.copy(
                            isEmployeeValidated = true,
                            employeeValidationError = null,
                            userStatus = status,
                            workOrderId = activeWorkOrder?.workOrderCollectionId?.toString()
                                ?: it.workOrderId,
                            activeField = if (activeWorkOrder == null) {
                                WorkOrderInputField.WORK_ORDER
                            } else {
                                WorkOrderInputField.EMPLOYEE
                            }
                        )
                    }
                    startWorkOrderTimeout()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isEmployeeValidated = false,
                            employeeValidationError = error.message ?: "Wrong user",
                            userStatus = null
                        )
                    }
                }
            )
            setLoading(false)
        }
    }

    private suspend fun refreshUserStatus(employee: String): Boolean {
        val result = repository.fetchUserStatus(employee)
        return result.fold(
            onSuccess = { status ->
                _uiState.update { current ->
                    current.copy(
                        isEmployeeValidated = true,
                        employeeValidationError = null,
                        userStatus = status,
                        workOrderId = status.activeWorkOrder?.workOrderCollectionId?.toString()
                            ?: current.workOrderId,
                        activeField = if (status.activeWorkOrder == null) {
                            WorkOrderInputField.WORK_ORDER
                        } else {
                            WorkOrderInputField.EMPLOYEE
                        }
                    )
                }
                startWorkOrderTimeout()
                true
            },
            onFailure = {
                false
            }
        )
    }

    private fun startWorkOrderTimeout() {
        cancelWorkOrderTimeout()
        workOrderTimeoutJob = viewModelScope.launch {
            delay(WORK_ORDER_TIMEOUT_MS)
            val shouldReset = _uiState.value.let { state ->
                if (!state.isEmployeeValidated || state.isLoading || state.showClockOutDialog) {
                    false
                } else {
                    val hasActiveWorkOrder = state.userStatus?.activeWorkOrder != null
                    if (hasActiveWorkOrder) {
                        true
                    } else {
                        state.workOrderId.isBlank()
                    }
                }
            }
            if (shouldReset) {
                resetToEmployeeStep()
            }
        }
    }

    private fun cancelWorkOrderTimeout() {
        workOrderTimeoutJob?.cancel()
        workOrderTimeoutJob = null
    }

    private fun resetToEmployeeStep() {
        cancelWorkOrderTimeout()
        saveEmployeeJob?.cancel()
        _uiState.update {
            it.copy(
                employeeId = "",
                workOrderId = "",
                isEmployeeValidated = false,
                employeeValidationError = null,
                userStatus = null,
                activeField = WorkOrderInputField.EMPLOYEE,
                clockOutQuantity = "",
                clockOutStatus = ClockOutStatus.COMPLETE
            )
        }
    }

    private fun nextActiveFieldAfterClockOut(state: WorkOrdersUiState): WorkOrderInputField {
        return if (state.userStatus?.activeWorkOrder != null) {
            WorkOrderInputField.EMPLOYEE
        } else {
            WorkOrderInputField.WORK_ORDER
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    private fun setLoading(loading: Boolean) {
        _uiState.update { it.copy(isLoading = loading) }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val userPrefs = UserPrefs.create(appContext)
                    val repository = AppContainer.workOrdersRepository(appContext, userPrefs)
                    return WorkOrdersViewModel(repository, userPrefs) as T
                }
            }
        }
    }
}

private fun String.isDigitsOnly(): Boolean = isNotEmpty() && all(Char::isDigit)
