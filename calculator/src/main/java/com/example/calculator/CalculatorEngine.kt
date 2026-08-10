package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale

enum class CalcOperator(val symbol: String) {
    ADD("+"),
    SUBTRACT("−"),
    MULTIPLY("×"),
    DIVIDE("÷"),
}

data class HistoryItem(
    val expression: String,
    val result: String,
    val timestamp: Long,
)

/**
 * 经典即时计算引擎：按按键顺序逐步计算，支持连续运算、负数开头、退格与错误状态。
 */
class CalculatorEngine {

    data class Snapshot(
        val expression: String,
        val display: String,
        val isError: Boolean,
    )

    private val tokens = mutableListOf<String>()
    private var displayText = "0"
    private var pendingOperator: CalcOperator? = null
    private var leftOperand: Double? = null
    private var newNumber = true
    private var error = false
    private var finishedExpression = ""

    fun snapshot(): Snapshot {
        val expression = if (finishedExpression.isNotEmpty()) {
            finishedExpression
        } else {
            buildString {
                append(tokens.joinToString(" "))
                if (pendingOperator != null && !newNumber) {
                    if (isNotEmpty()) append(' ')
                    append(displayText)
                }
            }
        }
        val display = when {
            error -> "错误"
            pendingOperator != null && !newNumber -> formatNumber(computePending())
            else -> displayText
        }
        return Snapshot(expression, display, error)
    }

    fun inputDigit(digit: Int) {
        if (error) return
        finishedExpression = ""
        if (newNumber) {
            if (pendingOperator == null && tokens.size == 1) tokens.clear()
            displayText = when {
                displayText == "0" -> digit.toString()
                displayText == "-0" && digit == 0 -> "-0"
                displayText == "-0" -> "-$digit"
                else -> digit.toString()
            }
            newNumber = false
        } else {
            when {
                displayText == "0" -> displayText = digit.toString()
                displayText == "-0" -> displayText = "-$digit"
                displayText == "-" -> displayText = "-$digit"
                displayText.length < MAX_INPUT_LENGTH -> displayText += digit
            }
        }
    }

    fun inputDot() {
        if (error) return
        finishedExpression = ""
        if (newNumber) {
            if (pendingOperator == null && tokens.size == 1) tokens.clear()
            displayText = if (displayText.startsWith("-")) "-0." else "0."
            newNumber = false
        } else if (!displayText.contains(".")) {
            displayText += "."
        }
    }

    fun inputOperator(op: CalcOperator) {
        if (error) return
        finishedExpression = ""
        val current = displayText.toDoubleOrNull() ?: 0.0

        if (pendingOperator == null) {
            // 空状态按“−”表示负数开头
            if (tokens.isEmpty() && newNumber && displayText == "0" && op == CalcOperator.SUBTRACT) {
                displayText = "-0"
                newNumber = false
                return
            }
            leftOperand = current
            if (tokens.isEmpty()) tokens.add(displayText)
        } else if (!newNumber) {
            val result = apply(pendingOperator!!, leftOperand ?: current, current)
            if (result.isNaN()) {
                setError()
                return
            }
            leftOperand = result
            tokens.clear()
            tokens.add(formatNumber(result))
        } else {
            // 连续按运算符：替换上一个运算符
            if (tokens.isNotEmpty()) tokens.removeAt(tokens.lastIndex)
        }

        tokens.add(op.symbol)
        pendingOperator = op
        displayText = formatNumber(leftOperand ?: current)
        newNumber = true
    }

    fun equals(): HistoryItem? {
        if (error) return null
        val current = displayText.toDoubleOrNull() ?: 0.0
        if (pendingOperator == null) {
            if (tokens.isEmpty()) return null
            if (newNumber && tokens.size == 1) return null
            displayText = formatNumber(current)
            tokens.clear()
            tokens.add(displayText)
            newNumber = true
            return null
        }

        val op = pendingOperator!!
        val result = apply(op, leftOperand ?: current, current)
        if (result.isNaN()) {
            setError()
            return null
        }

        val expression = tokens.joinToString(" ") + " $displayText ="
        val resultText = formatNumber(result)
        finishedExpression = expression
        tokens.clear()
        tokens.add(resultText)
        displayText = resultText
        pendingOperator = null
        leftOperand = null
        newNumber = true
        return HistoryItem(expression, resultText, System.currentTimeMillis())
    }

    fun backspace() {
        if (error) return
        finishedExpression = ""
        if (newNumber && pendingOperator != null) {
            // 退格删除刚按下的运算符
            if (tokens.isNotEmpty()) tokens.removeAt(tokens.lastIndex)
            pendingOperator = null
            leftOperand = null
            displayText = if (tokens.isNotEmpty()) tokens.last() else "0"
            newNumber = false
            return
        }
        if (newNumber && pendingOperator == null && tokens.size == 1) {
            displayText = tokens.last()
            tokens.clear()
            newNumber = false
        }
        if (displayText.length > 1) {
            displayText = displayText.dropLast(1)
            if (displayText == "-" || displayText == "-0") displayText = "0"
        } else {
            displayText = "0"
        }
    }

    fun clear() {
        error = false
        tokens.clear()
        displayText = "0"
        pendingOperator = null
        leftOperand = null
        newNumber = true
        finishedExpression = ""
    }

    fun loadResult(result: String) {
        error = false
        tokens.clear()
        tokens.add(result)
        displayText = result
        pendingOperator = null
        leftOperand = null
        newNumber = true
        finishedExpression = ""
    }

    private fun computePending(): Double {
        val right = displayText.toDoubleOrNull() ?: 0.0
        return apply(pendingOperator!!, leftOperand ?: right, right)
    }

    private fun setError() {
        error = true
        displayText = "错误"
        tokens.clear()
        pendingOperator = null
        leftOperand = null
        newNumber = true
        finishedExpression = ""
    }

    private fun apply(op: CalcOperator, a: Double, b: Double): Double = when (op) {
        CalcOperator.ADD -> a + b
        CalcOperator.SUBTRACT -> a - b
        CalcOperator.MULTIPLY -> a * b
        CalcOperator.DIVIDE -> if (b == 0.0) Double.NaN else a / b
    }

    private fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "错误"
        if (value == 0.0) return "0"
        val plain = BigDecimal(value).round(MathContext(12)).stripTrailingZeros().toPlainString()
        if (plain.length <= 12) return plain
        val scientific = String.format(Locale.US, "%.6e", value)
        val mantissa = scientific.substringBefore('e').trimEnd('0').trimEnd('.')
        val exponent = scientific.substringAfter('e')
        return "$mantissa${if (exponent.startsWith("+")) "e${exponent.drop(1)}" else "e$exponent"}"
    }

    private companion object {
        const val MAX_INPUT_LENGTH = 18
    }
}
