package com.shadow.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class MainActivity : AppCompatActivity() {
    private lateinit var expressionText: TextView
    private lateinit var resultText: TextView
    private var expression = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        expressionText = findViewById(R.id.expressionText)
        resultText = findViewById(R.id.resultText)

        bindInputButtons()
        bindActionButtons()
        refreshDisplay()
    }

    private fun bindInputButtons() {
        val inputs = mapOf(
            R.id.btn0 to "0",
            R.id.btn1 to "1",
            R.id.btn2 to "2",
            R.id.btn3 to "3",
            R.id.btn4 to "4",
            R.id.btn5 to "5",
            R.id.btn6 to "6",
            R.id.btn7 to "7",
            R.id.btn8 to "8",
            R.id.btn9 to "9",
            R.id.btnDot to ".",
            R.id.btnPlus to "+",
            R.id.btnMinus to "-",
            R.id.btnMultiply to "×",
            R.id.btnDivide to "÷",
            R.id.btnPercent to "%",
            R.id.btnOpen to "(",
            R.id.btnClose to ")",
            R.id.btnPi to "π",
            R.id.btnE to "e",
            R.id.btnPower to "^",
            R.id.btnExp to "*10^",
            R.id.btnFact to "!",
            R.id.btnSquare to "^2",
            R.id.btnSin to "sin(",
            R.id.btnCos to "cos(",
            R.id.btnTan to "tan(",
            R.id.btnAsin to "asin(",
            R.id.btnAcos to "acos(",
            R.id.btnAtan to "atan(",
            R.id.btnLn to "ln(",
            R.id.btnLog to "log(",
            R.id.btnSqrt to "sqrt("
        )

        inputs.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener {
                expression += value
                refreshDisplay()
            }
        }
    }

    private fun bindActionButtons() {
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            expression = ""
            refreshDisplay()
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            if (expression.isNotEmpty()) {
                expression = expression.dropLast(1)
                refreshDisplay()
            }
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            val value = evaluateExpression(expression)
            if (value != null) {
                expression = formatNumber(value)
                refreshDisplay(showPreview = false)
            } else {
                resultText.text = getString(android.R.string.unknownName)
            }
        }
    }

    private fun refreshDisplay(showPreview: Boolean = true) {
        expressionText.text = expression
        if (expression.isEmpty()) {
            resultText.text = "0"
            return
        }

        if (!showPreview) {
            resultText.text = expression
            return
        }

        val preview = evaluateExpression(expression)
        resultText.text = preview?.let(::formatNumber) ?: "…"
    }

    private fun evaluateExpression(raw: String): Double? {
        return try {
            val normalized = raw
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", "pi")
            ExpressionParser(normalized).parse()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            "%s".format(java.util.Locale.US, "%.10f", value).trimEnd('0').trimEnd('.')
        }
    }
}

private class ExpressionParser(source: String) {
    private val input = addImplicitMultiplication(source.replace(" ", ""))
    private var index = 0

    fun parse(): Double {
        val value = parseExpression()
        if (index != input.length) {
            throw IllegalArgumentException("Unexpected token")
        }
        return value
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            value = when {
                match('+') -> value + parseTerm()
                match('-') -> value - parseTerm()
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parsePower()
        while (true) {
            value = when {
                match('*') -> value * parsePower()
                match('/') -> value / parsePower()
                match('%') -> value % parsePower()
                else -> return value
            }
        }
    }

    private fun parsePower(): Double {
        var value = parseUnary()
        if (match('^')) {
            value = value.pow(parsePower())
        }
        return value
    }

    private fun parseUnary(): Double {
        return when {
            match('+') -> parseUnary()
            match('-') -> -parseUnary()
            else -> parsePostfix()
        }
    }

    private fun parsePostfix(): Double {
        var value = parsePrimary()
        while (match('!')) {
            value = factorial(value)
        }
        return value
    }

    private fun parsePrimary(): Double {
        if (match('(')) {
            val value = parseExpression()
            require(match(')')) { "Missing closing parenthesis" }
            return value
        }

        if (current().isLetter()) {
            val name = parseName()
            if (name == "pi") return Math.PI
            if (name == "e") return Math.E

            val argument = if (match('(')) {
                val v = parseExpression()
                require(match(')')) { "Missing closing parenthesis" }
                v
            } else {
                parsePrimary()
            }
            return applyFunction(name, argument)
        }

        return parseNumber()
    }

    private fun parseNumber(): Double {
        val start = index
        var hasDot = false
        while (index < input.length) {
            val c = input[index]
            if (c == '.') {
                if (hasDot) break
                hasDot = true
                index++
            } else if (c.isDigit()) {
                index++
            } else {
                break
            }
        }
        require(start != index) { "Number expected" }
        return input.substring(start, index).toDouble()
    }

    private fun parseName(): String {
        val start = index
        while (index < input.length && input[index].isLetter()) {
            index++
        }
        return input.substring(start, index)
    }

    private fun applyFunction(name: String, value: Double): Double {
        return when (name) {
            "sin" -> sin(Math.toRadians(value))
            "cos" -> cos(Math.toRadians(value))
            "tan" -> tan(Math.toRadians(value))
            "asin" -> Math.toDegrees(asin(value))
            "acos" -> Math.toDegrees(acos(value))
            "atan" -> Math.toDegrees(atan(value))
            "ln" -> ln(value)
            "log" -> log10(value)
            "sqrt" -> sqrt(value)
            else -> throw IllegalArgumentException("Unknown function: $name")
        }
    }

    private fun factorial(value: Double): Double {
        require(value >= 0 && value % 1.0 == 0.0) { "Factorial is defined for non-negative integers only" }
        var result = 1.0
        var i = 2
        while (i <= value.toInt()) {
            result *= i
            i++
        }
        return result
    }

    private fun current(): Char = if (index < input.length) input[index] else '\u0000'

    private fun match(expected: Char): Boolean {
        if (index < input.length && input[index] == expected) {
            index++
            return true
        }
        return false
    }

    private fun addImplicitMultiplication(raw: String): String {
        if (raw.isEmpty()) return raw

        val out = StringBuilder()
        for (i in raw.indices) {
            val c = raw[i]
            out.append(c)
            if (i == raw.lastIndex) continue

            val next = raw[i + 1]
            val leftTokenEndsValue = c.isDigit() || c == '.' || c == ')' || c == '!' || c == 'e' || c == 'i'
            val rightTokenStartsValue = next.isDigit() || next == '(' || next.isLetter()
            if (leftTokenEndsValue && rightTokenStartsValue && !(c == 'e' && next.isDigit())) {
                out.append('*')
            }
        }
        return out.toString()
    }
}
