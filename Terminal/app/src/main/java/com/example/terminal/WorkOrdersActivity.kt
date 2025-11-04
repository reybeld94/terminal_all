package com.example.terminal

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.terminal.ui.enterImmersiveMode

class WorkOrdersActivity : AppCompatActivity() {

    private enum class InputTarget { EMPLOYEE, WORK_ORDER }

    private val employeeBuilder = StringBuilder()
    private val workOrderBuilder = StringBuilder()
    private var activeTarget = InputTarget.EMPLOYEE

    private lateinit var employeeValue: TextView
    private lateinit var workOrderValue: TextView
    private lateinit var activeInput: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_orders)
        window.enterImmersiveMode()

        employeeValue = findViewById(R.id.textEmployeeValue)
        workOrderValue = findViewById(R.id.textWorkOrderValue)
        activeInput = findViewById(R.id.textActiveInput)

        val digitButtons = mapOf(
            R.id.buttonKey0 to "0",
            R.id.buttonKey1 to "1",
            R.id.buttonKey2 to "2",
            R.id.buttonKey3 to "3",
            R.id.buttonKey4 to "4",
            R.id.buttonKey5 to "5",
            R.id.buttonKey6 to "6",
            R.id.buttonKey7 to "7",
            R.id.buttonKey8 to "8",
            R.id.buttonKey9 to "9"
        )

        employeeValue.setOnClickListener {
            activeTarget = InputTarget.EMPLOYEE
            updateActiveIndicator()
        }

        workOrderValue.setOnClickListener {
            activeTarget = InputTarget.WORK_ORDER
            updateActiveIndicator()
        }

        digitButtons.forEach { (buttonId, value) ->
            findViewById<Button>(buttonId).setOnClickListener {
                appendCharacters(value)
            }
        }

        findViewById<Button>(R.id.buttonClear).setOnClickListener {
            removeLastCharacter()
        }

        findViewById<Button>(R.id.buttonEnter).setOnClickListener {
            toggleActiveTarget()
        }

        findViewById<Button>(R.id.buttonClockInWo).setOnClickListener {
            handleClockAction(clockIn = true)
        }

        findViewById<Button>(R.id.buttonClockOutWo).setOnClickListener {
            handleClockAction(clockIn = false)
        }

        updateDisplays()
        updateActiveIndicator()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.enterImmersiveMode()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_MULTIPLE && event.keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            val characters = event.characters
            if (!characters.isNullOrEmpty()) {
                val digitsOnly = characters.filter { it.isDigit() }
                if (digitsOnly.isNotEmpty()) {
                    appendCharacters(digitsOnly)
                    return true
                }
            }
        }

        if (event.action == KeyEvent.ACTION_UP) {
            when (event.keyCode) {
                in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9,
                in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> {
                    val unicodeChar = event.unicodeChar
                    if (unicodeChar != 0 && unicodeChar.toChar().isDigit()) {
                        appendCharacters(unicodeChar.toChar().toString())
                        return true
                    }
                }
                KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL -> {
                    removeLastCharacter()
                    return true
                }
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    toggleActiveTarget()
                    return true
                }
            }
        }

        return super.dispatchKeyEvent(event)
    }

    private fun currentBuilder(): StringBuilder {
        return if (activeTarget == InputTarget.EMPLOYEE) employeeBuilder else workOrderBuilder
    }

    private fun appendCharacters(value: CharSequence) {
        if (value.isEmpty()) return
        currentBuilder().append(value)
        updateDisplays()
    }

    private fun removeLastCharacter() {
        val builder = currentBuilder()
        if (builder.isNotEmpty()) {
            builder.deleteCharAt(builder.length - 1)
            updateDisplays()
        }
    }

    private fun toggleActiveTarget() {
        activeTarget = when (activeTarget) {
            InputTarget.EMPLOYEE -> InputTarget.WORK_ORDER
            InputTarget.WORK_ORDER -> InputTarget.EMPLOYEE
        }
        updateActiveIndicator()
    }

    private fun updateDisplays() {
        employeeValue.text = if (employeeBuilder.isEmpty()) {
            getString(R.string.default_input_placeholder)
        } else {
            employeeBuilder.toString()
        }

        workOrderValue.text = if (workOrderBuilder.isEmpty()) {
            getString(R.string.default_input_placeholder)
        } else {
            workOrderBuilder.toString()
        }
    }

    private fun updateActiveIndicator() {
        activeInput.text = when (activeTarget) {
            InputTarget.EMPLOYEE -> getString(R.string.work_orders_active_employee)
            InputTarget.WORK_ORDER -> getString(R.string.work_orders_active_work_order)
        }

        employeeValue.alpha = if (activeTarget == InputTarget.EMPLOYEE) 1f else 0.6f
        workOrderValue.alpha = if (activeTarget == InputTarget.WORK_ORDER) 1f else 0.6f
    }

    private fun handleClockAction(clockIn: Boolean) {
        val employeeNumber = employeeBuilder.toString()
        val workOrderNumber = workOrderBuilder.toString()

        if (employeeNumber.isBlank() || workOrderNumber.isBlank()) {
            Toast.makeText(
                this,
                getString(R.string.toast_enter_employee_and_work_order),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val message = if (clockIn) {
            val workOrders = workAssignments.getOrPut(employeeNumber) { mutableSetOf() }
            workOrders.add(workOrderNumber)
            getString(R.string.toast_clock_in_wo, employeeNumber, workOrderNumber)
        } else {
            val workOrders = workAssignments[employeeNumber]
            workOrders?.remove(workOrderNumber)
            if (workOrders != null && workOrders.isEmpty()) {
                workAssignments.remove(employeeNumber)
            }
            getString(R.string.toast_clock_out_wo, employeeNumber, workOrderNumber)
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val workAssignments = mutableMapOf<String, MutableSet<String>>()
    }
}
