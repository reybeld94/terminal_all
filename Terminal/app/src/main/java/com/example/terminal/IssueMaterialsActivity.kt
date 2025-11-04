package com.example.terminal

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.terminal.ui.enterImmersiveMode

class IssueMaterialsActivity : AppCompatActivity() {

    private enum class InputTarget { EMPLOYEE, MATERIAL }

    private val employeeBuilder = StringBuilder()
    private val materialBuilder = StringBuilder()
    private var activeTarget = InputTarget.EMPLOYEE

    private lateinit var employeeValue: TextView
    private lateinit var materialValue: TextView
    private lateinit var activeInput: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_issue_materials)
        window.enterImmersiveMode()

        employeeValue = findViewById(R.id.textIssueEmployeeValue)
        materialValue = findViewById(R.id.textMaterialValue)
        activeInput = findViewById(R.id.textIssueActiveInput)

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

        materialValue.setOnClickListener {
            activeTarget = InputTarget.MATERIAL
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

        findViewById<Button>(R.id.buttonIssueMaterial).setOnClickListener {
            handleIssueMaterial()
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
        return if (activeTarget == InputTarget.EMPLOYEE) employeeBuilder else materialBuilder
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
            InputTarget.EMPLOYEE -> InputTarget.MATERIAL
            InputTarget.MATERIAL -> InputTarget.EMPLOYEE
        }
        updateActiveIndicator()
    }

    private fun updateDisplays() {
        employeeValue.text = if (employeeBuilder.isEmpty()) {
            getString(R.string.default_input_placeholder)
        } else {
            employeeBuilder.toString()
        }

        materialValue.text = if (materialBuilder.isEmpty()) {
            getString(R.string.default_input_placeholder)
        } else {
            materialBuilder.toString()
        }
    }

    private fun updateActiveIndicator() {
        activeInput.text = when (activeTarget) {
            InputTarget.EMPLOYEE -> getString(R.string.issue_materials_active_employee)
            InputTarget.MATERIAL -> getString(R.string.issue_materials_active_material)
        }

        employeeValue.alpha = if (activeTarget == InputTarget.EMPLOYEE) 1f else 0.6f
        materialValue.alpha = if (activeTarget == InputTarget.MATERIAL) 1f else 0.6f
    }

    private fun handleIssueMaterial() {
        val employeeNumber = employeeBuilder.toString()
        val materialCode = materialBuilder.toString()

        if (employeeNumber.isBlank() || materialCode.isBlank()) {
            Toast.makeText(
                this,
                getString(R.string.toast_enter_employee_and_material),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val materials = issuedMaterials.getOrPut(employeeNumber) { mutableListOf() }
        materials.add(materialCode)

        val message = getString(R.string.toast_issue_material, employeeNumber, materialCode)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val issuedMaterials = mutableMapOf<String, MutableList<String>>()
    }
}
