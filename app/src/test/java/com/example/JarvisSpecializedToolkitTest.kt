package com.example

import com.example.models.JarvisSpecializedToolkit
import org.junit.Assert.*
import org.junit.Test

class JarvisSpecializedToolkitTest {

    @Test
    fun testBasicMathOperations() {
        val addRes = JarvisSpecializedToolkit.tryEvaluateMath("calculate 25 + 75")
        assertNotNull(addRes)
        assertTrue(addRes!!.contains("100"))

        val subRes = JarvisSpecializedToolkit.tryEvaluateMath("what is 100 - 35")
        assertNotNull(subRes)
        assertTrue(subRes!!.contains("65"))

        val mulRes = JarvisSpecializedToolkit.tryEvaluateMath("what is 12 * 12")
        assertNotNull(mulRes)
        assertTrue(mulRes!!.contains("144"))

        val divRes = JarvisSpecializedToolkit.tryEvaluateMath("calculate 144 / 12")
        assertNotNull(divRes)
        assertTrue(divRes!!.contains("12"))
    }

    @Test
    fun testParenthesesAndPrecedence() {
        val res = JarvisSpecializedToolkit.tryEvaluateMath("calculate 2 + 3 * 4")
        assertNotNull(res)
        assertTrue(res!!.contains("14"))

        val parenRes = JarvisSpecializedToolkit.tryEvaluateMath("calculate (2 + 3) * 4")
        assertNotNull(parenRes)
        assertTrue(parenRes!!.contains("20"))
    }

    @Test
    fun testPercentageAndSquareRoot() {
        val pctRes = JarvisSpecializedToolkit.tryEvaluateMath("what is 20 percent of 500")
        assertNotNull(pctRes)
        assertTrue(pctRes!!.contains("100"))

        val sqrtRes = JarvisSpecializedToolkit.tryEvaluateMath("square root of 144")
        assertNotNull(sqrtRes)
        assertTrue(sqrtRes!!.contains("12"))
    }

    @Test
    fun testDivideByZeroHandling() {
        val divZero = JarvisSpecializedToolkit.tryEvaluateMath("what is 50 / 0")
        assertNotNull(divZero)
        assertTrue(divZero!!.contains("Division by zero") || divZero.contains("Mathematical error"))
    }

    @Test
    fun testNonMathReturnsNull() {
        val nonMath = JarvisSpecializedToolkit.tryEvaluateMath("tell me a funny bedtime story")
        assertNull(nonMath)
    }

    @Test
    fun testLengthConversions() {
        val res = JarvisSpecializedToolkit.tryEvaluateConversion("convert 10 km to miles")
        assertNotNull(res)
        assertTrue(res!!.contains("miles"))
    }

    @Test
    fun testTemperatureConversions() {
        val cToF = JarvisSpecializedToolkit.tryEvaluateConversion("convert 100 celsius to fahrenheit")
        assertNotNull(cToF)
        assertTrue(cToF!!.contains("212"))

        val fToC = JarvisSpecializedToolkit.tryEvaluateConversion("convert 32 fahrenheit to celsius")
        assertNotNull(fToC)
        assertTrue(fToC!!.contains("0"))
    }

    @Test
    fun testWeightConversions() {
        val res = JarvisSpecializedToolkit.tryEvaluateConversion("convert 1 kg to grams")
        assertNotNull(res)
        assertTrue(res!!.contains("1,000") || res.contains("1000"))
    }

    @Test
    fun testCurrencyConversions() {
        val res = JarvisSpecializedToolkit.tryEvaluateConversion("convert 100 usd to eur")
        assertNotNull(res)
        assertTrue(res!!.contains("EUR"))
    }

    @Test
    fun testSmartRepliesGeneration() {
        val weatherReplies = JarvisSpecializedToolkit.generateSmartReplies("Here is the weather forecast", "weather")
        assertTrue(weatherReplies.isNotEmpty())
        assertTrue(weatherReplies.any { it.contains("Forecast") || it.contains("rain") })

        val alarmReplies = JarvisSpecializedToolkit.generateSmartReplies("Alarms configured", "set alarm")
        assertTrue(alarmReplies.isNotEmpty())
        assertTrue(alarmReplies.any { it.contains("alarm") || it.contains("timer") })
    }
}
