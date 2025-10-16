package com.example.terminal

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_issue_materials)
        window.enterImmersiveMode()

        val employeeValue = findViewById<TextView>(R.id.textIssueEmployeeValue)
        val materialValue = findViewById<TextView>(R.id.textMaterialValue)
        val activeInput = findViewById<TextView>(R.id.textIssueActiveInput)

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

        val updateDisplays = {
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

        val updateActiveIndicator = {
            activeInput.text = when (activeTarget) {
                InputTarget.EMPLOYEE -> getString(R.string.issue_materials_active_employee)
                InputTarget.MATERIAL -> getString(R.string.issue_materials_active_material)
            }

            employeeValue.alpha = if (activeTarget == InputTarget.EMPLOYEE) 1f else 0.6f
            materialValue.alpha = if (activeTarget == InputTarget.MATERIAL) 1f else 0.6f
        }

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
                val builder = if (activeTarget == InputTarget.EMPLOYEE) employeeBuilder else materialBuilder
                builder.append(value)
                updateDisplays()
            }
        }

        findViewById<Button>(R.id.buttonClear).setOnClickListener {
            val builder = if (activeTarget == InputTarget.EMPLOYEE) employeeBuilder else materialBuilder
            if (builder.isNotEmpty()) {
                builder.deleteCharAt(builder.length - 1)
                updateDisplays()
            }
        }

        findViewById<Button>(R.id.buttonEnter).setOnClickListener {
            activeTarget = when (activeTarget) {
                InputTarget.EMPLOYEE -> InputTarget.MATERIAL
                InputTarget.MATERIAL -> InputTarget.EMPLOYEE
            }
            updateActiveIndicator()
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
