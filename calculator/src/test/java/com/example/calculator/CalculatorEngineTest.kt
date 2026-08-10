package com.example.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun basicAddition() {
        val engine = CalculatorEngine()
        engine.inputDigit(1)
        engine.inputOperator(CalcOperator.ADD)
        engine.inputDigit(2)
        assertEquals("1 + 2", engine.snapshot().expression)
        assertEquals("3", engine.snapshot().display)
        val history = engine.equals()
        assertEquals("1 + 2 =", history?.expression)
        assertEquals("3", history?.result)
        assertEquals("3", engine.snapshot().display)
    }

    @Test
    fun chainedCalculationUsesImmediateResult() {
        val engine = CalculatorEngine()
        engine.inputDigit(2)
        engine.inputOperator(CalcOperator.ADD)
        engine.inputDigit(3)
        engine.equals()
        engine.inputOperator(CalcOperator.MULTIPLY)
        engine.inputDigit(4)
        val history = engine.equals()
        assertEquals("5 × 4 =", history?.expression)
        assertEquals("20", history?.result)
    }

    @Test
    fun divisionByZeroShowsError() {
        val engine = CalculatorEngine()
        engine.inputDigit(5)
        engine.inputOperator(CalcOperator.DIVIDE)
        engine.inputDigit(0)
        engine.equals()
        assertTrue(engine.snapshot().isError)
        assertEquals("错误", engine.snapshot().display)
        engine.clear()
        assertEquals("0", engine.snapshot().display)
    }

    @Test
    fun negativeNumberStartsWithMinus() {
        val engine = CalculatorEngine()
        engine.inputOperator(CalcOperator.SUBTRACT)
        engine.inputDigit(5)
        engine.inputOperator(CalcOperator.ADD)
        engine.inputDigit(3)
        val history = engine.equals()
        assertEquals("-5 + 3 =", history?.expression)
        assertEquals("-2", history?.result)
    }

    @Test
    fun decimalPrecisionIsCleaned() {
        val engine = CalculatorEngine()
        engine.inputDigit(0)
        engine.inputDot()
        engine.inputDigit(1)
        engine.inputOperator(CalcOperator.ADD)
        engine.inputDigit(0)
        engine.inputDot()
        engine.inputDigit(2)
        val history = engine.equals()
        assertEquals("0.3", history?.result)
    }

    @Test
    fun backspaceRemovesOperatorThenDigit() {
        val engine = CalculatorEngine()
        engine.inputDigit(1)
        engine.inputDigit(2)
        engine.inputOperator(CalcOperator.ADD)
        engine.backspace()
        assertEquals("12", engine.snapshot().display)
        engine.backspace()
        assertEquals("1", engine.snapshot().display)
        engine.backspace()
        assertEquals("0", engine.snapshot().display)
    }

    @Test
    fun equalsWithoutOperatorKeepsValue() {
        val engine = CalculatorEngine()
        engine.inputDigit(7)
        assertNull(engine.equals())
        assertEquals("7", engine.snapshot().display)
    }

    @Test
    fun loadResultStartsNewCalculation() {
        val engine = CalculatorEngine()
        engine.loadResult("42")
        engine.inputOperator(CalcOperator.ADD)
        engine.inputDigit(8)
        val history = engine.equals()
        assertEquals("42 + 8 =", history?.expression)
        assertEquals("50", history?.result)
    }
}
