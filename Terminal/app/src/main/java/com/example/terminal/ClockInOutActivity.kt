package com.example.terminal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.terminal.ui.enterImmersiveMode

class ClockInOutActivity : AppCompatActivity() {

    private val inputBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clock_in_out)
        window.enterImmersiveMode()

        val inputField = findViewById<EditText>(R.id.editEmployeeInput)
        val displayText = findViewById<TextView>(R.id.textEmployeeDisplay)

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

        digitButtons.forEach { (buttonId, value) ->
            findViewById<Button>(buttonId).setOnClickListener {
                inputBuilder.append(value)
                updateDisplay(inputField, displayText)
            }
        }

        findViewById<Button>(R.id.buttonClear).setOnClickListener {
            if (inputBuilder.isNotEmpty()) {
                inputBuilder.deleteCharAt(inputBuilder.length - 1)
            }
            updateDisplay(inputField, displayText)
        }

        findViewById<Button>(R.id.buttonEnter).setOnClickListener {
            val employeeNumber = inputBuilder.toString()
            if (employeeNumber.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.toast_enter_employee_number),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val messageRes = if (LoggedEmployees.toggle(employeeNumber)) {
                R.string.toast_clock_in_success
            } else {
                R.string.toast_clock_out_success
            }

            Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()
            inputBuilder.clear()
            updateDisplay(inputField, displayText)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.enterImmersiveMode()
        }
    }

    private fun updateDisplay(inputField: EditText, displayText: TextView) {
        if (inputBuilder.isEmpty()) {
            inputField.text.clear()
            displayText.text = getString(R.string.clock_in_out_display_placeholder)
        } else {
            val currentValue = inputBuilder.toString()
            inputField.setText(currentValue)
            inputField.setSelection(currentValue.length)
            displayText.text = currentValue
        }
    }

    private object LoggedEmployees {
        private val loggedIn = mutableSetOf<String>()

        /**
         * Toggles the login state for the provided employee number.
         *
         * @return true if the employee has just clocked in, false if clocked out.
         */
        fun toggle(employeeNumber: String): Boolean {
            return if (loggedIn.contains(employeeNumber)) {
                loggedIn.remove(employeeNumber)
                false
            } else {
                loggedIn.add(employeeNumber)
                true
            }
        }
    }
}
