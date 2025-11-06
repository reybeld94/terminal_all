package com.example.terminal.ui.workorders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.terminal.data.local.UserPrefs
import com.example.terminal.data.network.ClockOutStatus
import com.example.terminal.data.repository.ActiveWorkOrder
import com.example.terminal.data.repository.UserStatus
import com.example.terminal.data.repository.WorkOrderDetails
import com.example.terminal.data.repository.WorkOrdersRepository
import com.example.terminal.di.AppContainer
import java.util.Locale
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
    val showClockOutForm: Boolean = false,
    val isEmployeeValidated: Boolean = false,
    val employeeValidationError: String? = null,
    val userStatus: UserStatus? = null,
    val clockOutQuantity: String = "",
    val clockOutStatus: ClockOutStatus = ClockOutStatus.COMPLETE,
    val scannedWorkOrder: WorkOrderDetails? = null,
    val isAssemblyLoading: Boolean = false
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
    private var workOrderDetailsJob: Job? = null

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

    fun onBarcodeScanned(rawCode: String) {
        if (_uiState.value.isLoading) {
            return
        }

        val trimmed = rawCode.trim()
        if (trimmed.isEmpty()) {
            return
        }

        val uppercase = trimmed.uppercase(Locale.getDefault())
        val handled = when {
            uppercase.length > 1 && uppercase.startsWith('A') -> {
                val digits = uppercase.drop(1)
                if (digits.isNotEmpty() && digits.all(Char::isDigit)) {
                    handleAssemblyScan(digits)
                    true
                } else {
                    false
                }
            }

            trimmed.all(Char::isDigit) -> {
                handleEmployeeScan(trimmed)
                true
            }

            else -> false
        }

        if (!handled) {
            showMessage("Unrecognized barcode")
        }
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
            showMessage("Validate the employee before continuing")
            return
        }

        if (employee.isEmpty() || workOrder.isEmpty()) {
            showMessage("Employee ID and assembly number are required")
            return
        }

        if (!employee.isDigitsOnly() || !workOrder.isDigitsOnly()) {
            showMessage("Enter valid numeric values")
            return
        }

        if (userId == null) {
            showMessage("Unable to fetch the user. Please validate again.")
            return
        }

        val workOrderId = workOrder.toInt()

        setLoading(true)
        val scannedDetails = _uiState.value.scannedWorkOrder

        viewModelScope.launch {
            val result = repository.clockIn(workOrderId, userId)
            result.fold(
                onSuccess = {
                    val refreshed = refreshUserStatus(
                        employee = employee,
                        fallbackWorkOrder = scannedDetails
                    )
                    if (refreshed) {
                        showMessage("Clock in recorded successfully")
                    } else {
                        showMessage("Clock in recorded successfully, but the information could not be refreshed")
                    }
                },
                onFailure = { error ->
                    showMessage(error.message ?: "Failed to register clock in")
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
            showMessage("Validate the employee before continuing")
            return
        }
        if (employee.isEmpty()) {
            showMessage("Employee ID and assembly number are required")
            return
        }
        if (activeWorkOrderId == null) {
            if (workOrder.isEmpty()) {
                showMessage("Employee ID and assembly number are required")
                return
            }
            if (!workOrder.isDigitsOnly()) {
                showMessage("Enter valid numeric values")
                return
            }
        }
        cancelWorkOrderTimeout()
        _uiState.update {
            it.copy(
                showClockOutForm = true,
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
            showMessage("Enter a valid quantity greater than 0")
            return
        }

        if (!employee.isDigitsOnly()) {
            showMessage("Enter valid numeric values")
            return
        }

        val workOrderId = activeWorkOrderId ?: workOrder.toIntOrNull()
        if (workOrderId == null) {
            showMessage("Employee ID and assembly number are required")
            return
        }

        val status = state.clockOutStatus
        setLoading(true)
        _uiState.update {
            it.copy(
                showClockOutForm = false,
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
                    showMessage("Clock out recorded successfully")
                },
                onFailure = { error ->
                    showMessage(error.message ?: "Failed to register clock out")
                }
            )
            setLoading(false)
            startWorkOrderTimeout()
        }
    }

    fun hideClockOutForm() {
        _uiState.update {
            it.copy(
                showClockOutForm = false,
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

    private fun handleEmployeeScan(employeeCode: String) {
        updateEmployeeId(employeeCode)
        validateEmployee()
    }

    private fun handleAssemblyScan(workOrderCode: String) {
        cancelWorkOrderTimeout()
        updateWorkOrderId(workOrderCode)
        loadWorkOrderDetails(workOrderCode)
        val isEmployeeValidated = _uiState.value.isEmployeeValidated
        _uiState.update { current ->
            val nextField = if (isEmployeeValidated) {
                WorkOrderInputField.WORK_ORDER
            } else {
                WorkOrderInputField.EMPLOYEE
            }
            current.copy(activeField = nextField)
        }
        if (!isEmployeeValidated) {
            startWorkOrderTimeout()
        } else {
            attemptAutoClockIn()
        }
    }

    private fun loadWorkOrderDetails(workOrderCode: String) {
        if (workOrderCode.isBlank()) {
            _uiState.update {
                it.copy(
                    scannedWorkOrder = null,
                    isAssemblyLoading = false
                )
            }
            workOrderDetailsJob?.cancel()
            workOrderDetailsJob = null
            return
        }

        workOrderDetailsJob?.cancel()
        _uiState.update {
            it.copy(
                scannedWorkOrder = null,
                isAssemblyLoading = true
            )
        }

        workOrderDetailsJob = viewModelScope.launch {
            val result = repository.fetchWorkOrderDetails(workOrderCode)
            result.fold(
                onSuccess = { details ->
                    _uiState.update { current ->
                        current.copy(
                            workOrderId = details.workOrderAssemblyId.toString(),
                            scannedWorkOrder = details,
                            isAssemblyLoading = false
                        )
                    }
                    attemptAutoClockIn()
                },
                onFailure = { error ->
                    _uiState.update { current ->
                        current.copy(
                            scannedWorkOrder = null,
                            isAssemblyLoading = false
                        )
                    }
                    showMessage(error.message ?: "Unable to retrieve assembly information")
                }
            )
        }
    }

    private fun attemptAutoClockIn() {
        val state = _uiState.value
        val assemblyReady = state.scannedWorkOrder?.let { details ->
            !details.isWorkOrderClosed && details.isReleased && !details.isAssemblyClosed
        } ?: false
        val canClockIn = state.isEmployeeValidated &&
            !state.isLoading &&
            !state.isAssemblyLoading &&
            assemblyReady &&
            state.userStatus?.activeWorkOrder == null &&
            state.workOrderId.isDigitsOnly()

        if (canClockIn) {
            onClockIn()
        }
    }

    private fun updateEmployeeId(value: String) {
        cancelWorkOrderTimeout()
        _uiState.update {
            it.copy(
                employeeId = value,
                isEmployeeValidated = false,
                employeeValidationError = null,
                userStatus = null,
                showClockOutForm = false,
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
        _uiState.update { current ->
            val matchingDetails = current.scannedWorkOrder?.takeIf { details ->
                details.workOrderAssemblyId.toString() == value ||
                    details.workOrderAssemblyNumber == value
            }
            current.copy(
                workOrderId = value,
                scannedWorkOrder = if (value.isBlank()) null else matchingDetails,
                isAssemblyLoading = if (value.isBlank()) false else current.isAssemblyLoading
            )
        }
        if (value.isBlank()) {
            workOrderDetailsJob?.cancel()
            workOrderDetailsJob = null
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
            showMessage("Enter the employee ID")
            return
        }

        if (!employee.isDigitsOnly()) {
            showMessage("Enter a valid employee ID")
            return
        }

        setLoading(true)
        viewModelScope.launch {
            var shouldAttemptClockIn = false
            val result = repository.fetchUserStatus(employee)
            result.fold(
                onSuccess = { status ->
                    val activeWorkOrder = status.activeWorkOrder
                    val shouldPromptForWorkOrder = activeWorkOrder == null &&
                        _uiState.value.workOrderId.isBlank()
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
                            },
                            isAssemblyLoading = if (activeWorkOrder != null) {
                                false
                            } else {
                                it.isAssemblyLoading
                            }
                        )
                    }
                    startWorkOrderTimeout()
                    if (shouldPromptForWorkOrder) {
                        showMessage("Scan the assembly number")
                    }
                    shouldAttemptClockIn = true
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
            if (shouldAttemptClockIn) {
                attemptAutoClockIn()
            }
        }
    }

    private suspend fun refreshUserStatus(
        employee: String,
        fallbackWorkOrder: WorkOrderDetails? = null
    ): Boolean {
        val result = repository.fetchUserStatus(employee)
        return result.fold(
            onSuccess = { status ->
                _uiState.update { current ->
                    val mergedStatus = mergeActiveWorkOrderDetails(
                        status = status,
                        details = fallbackWorkOrder ?: current.scannedWorkOrder
                    )
                    current.copy(
                        isEmployeeValidated = true,
                        employeeValidationError = null,
                        userStatus = mergedStatus,
                        workOrderId = status.activeWorkOrder?.workOrderCollectionId?.toString()
                            ?: current.workOrderId,
                        activeField = if (status.activeWorkOrder == null) {
                            WorkOrderInputField.WORK_ORDER
                        } else {
                            WorkOrderInputField.EMPLOYEE
                        },
                        scannedWorkOrder = null,
                        isAssemblyLoading = false
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
                if (state.isLoading || state.showClockOutForm) {
                    false
                } else if (!state.isEmployeeValidated) {
                    state.workOrderId.isNotBlank() ||
                        state.scannedWorkOrder != null ||
                        state.isAssemblyLoading
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
        workOrderDetailsJob?.cancel()
        workOrderDetailsJob = null
        _uiState.update {
            it.copy(
                employeeId = "",
                workOrderId = "",
                isEmployeeValidated = false,
                employeeValidationError = null,
                userStatus = null,
                activeField = WorkOrderInputField.EMPLOYEE,
                clockOutQuantity = "",
                clockOutStatus = ClockOutStatus.COMPLETE,
                scannedWorkOrder = null,
                isAssemblyLoading = false
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

    private fun mergeActiveWorkOrderDetails(
        status: UserStatus,
        details: WorkOrderDetails?
    ): UserStatus {
        val active = status.activeWorkOrder ?: return status
        val workOrderDetails = details ?: return status
        if (!matchesWorkOrder(workOrderDetails, active)) {
            return status
        }

        return status.copy(
            activeWorkOrder = active.copy(
                partNumber = workOrderDetails.partNumber ?: active.partNumber,
                operationCode = workOrderDetails.operationCode ?: active.operationCode,
                operationName = workOrderDetails.operationName ?: active.operationName
            )
        )
    }

    private fun matchesWorkOrder(
        details: WorkOrderDetails,
        active: ActiveWorkOrder
    ): Boolean {
        val collectionMatches = active.workOrderCollectionId != null &&
            active.workOrderCollectionId == details.workOrderAssemblyId
        val numberMatches = !details.workOrderNumber.isNullOrBlank() &&
            details.workOrderNumber == active.workOrderNumber
        val assemblyMatches = !details.workOrderAssemblyNumber.isNullOrBlank() &&
            details.workOrderAssemblyNumber == active.workOrderAssemblyNumber
        return collectionMatches || numberMatches || assemblyMatches
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
