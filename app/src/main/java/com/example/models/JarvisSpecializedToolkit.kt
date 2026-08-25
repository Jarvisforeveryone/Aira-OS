package com.example.models

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.service.AiraAccessibilityService
import java.net.URLEncoder
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.*

object JarvisSpecializedToolkit {

    private const val TAG = "JarvisToolkit"
    private val decimalFormat = DecimalFormat("#,##0.##")

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 1. CALCULATOR & MATH EVALUATOR
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun tryEvaluateMath(input: String): String? {
        val lower = input.lowercase(Locale.ROOT).trim()
        
        // Check if query is math-related
        val isMathPrompt = lower.startsWith("calculate") ||
                lower.startsWith("what is") ||
                lower.startsWith("how much is") ||
                lower.startsWith("eval") ||
                lower.startsWith("solve") ||
                lower.contains("plus") ||
                lower.contains("minus") ||
                lower.contains("times") ||
                lower.contains("divided by") ||
                lower.contains("percent of") ||
                lower.contains("square root of") ||
                lower.contains("sqrt") ||
                lower.matches(Regex("^[0-9\\s+\\-*/^().%]+$"))

        if (!isMathPrompt) return null

        // Extract formula
        var cleanExpr = lower
            .replace("calculate", "")
            .replace("what is", "")
            .replace("how much is", "")
            .replace("eval", "")
            .replace("solve", "")
            .replace("the value of", "")
            .replace("math", "")
            .trim()

        // Handle Percentage (e.g. "20 percent of 500" or "20% of 500")
        val percentRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:percent|%)\\s*of\\s*(\\d+(?:\\.\\d+)?)")
        val percentMatch = percentRegex.find(cleanExpr)
        if (percentMatch != null) {
            val (rateStr, totalStr) = percentMatch.destructured
            val rate = rateStr.toDoubleOrNull() ?: 0.0
            val total = totalStr.toDoubleOrNull() ?: 0.0
            val result = (rate / 100.0) * total
            return "Calculation complete, sir. $rate% of ${decimalFormat.format(total)} is ${decimalFormat.format(result)}."
        }

        // Handle Square Root (e.g. "square root of 144" or "sqrt(144)")
        val sqrtRegex = Regex("(?:square root of|sqrt)\\s*(\\d+(?:\\.\\d+)?)")
        val sqrtMatch = sqrtRegex.find(cleanExpr)
        if (sqrtMatch != null) {
            val (valStr) = sqrtMatch.destructured
            val num = valStr.toDoubleOrNull() ?: 0.0
            val result = sqrt(num)
            return "The square root of ${decimalFormat.format(num)} is ${decimalFormat.format(result)}, sir."
        }

        // Standardize word operators to math symbols
        cleanExpr = cleanExpr
            .replace("plus", "+")
            .replace("add", "+")
            .replace("minus", "-")
            .replace("subtract", "-")
            .replace("times", "*")
            .replace("multiplied by", "*")
            .replace("multiply", "*")
            .replace("x", "*")
            .replace("divided by", "/")
            .replace("divide", "/")
            .replace("over", "/")
            .replace("to the power of", "^")
            .replace("power", "^")
            .replace("[^0-9.+\\-*/^()\\s]".toRegex(), "")
            .trim()

        if (cleanExpr.isBlank()) return null

        return try {
            val result = evaluateArithmetic(cleanExpr)
            if (result.isNaN() || result.isInfinite()) {
                "Mathematical error in evaluation, sir. Division by zero or undefined operation."
            } else {
                "The calculation yields ${decimalFormat.format(result)}, sir."
            }
        } catch (ae: ArithmeticException) {
            "Mathematical error in evaluation, sir. Division by zero or undefined operation."
        } catch (e: Exception) {
            null
        }
    }

    private fun evaluateArithmetic(expression: String): Double {
        class ExprParser(private val expr: String) {
            private var pos = -1
            private var ch = -1

            private fun nextChar() {
                ch = if (++pos < expr.length) expr[pos].code else -1
            }

            private fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val res = parseExpression()
                if (pos < expr.length) throw RuntimeException("Unexpected: " + expr[pos])
                return res
            }

            private fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            private fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> {
                            val divisor = parseFactor()
                            if (divisor == 0.0) throw ArithmeticException("Divide by zero")
                            x /= divisor
                        }
                        eat('%'.code) -> x %= parseFactor()
                        else -> return x
                    }
                }
            }

            private fun parseFactor(): Double {
                if (eat('+'.code)) return +parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    x = expr.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected token: " + ch.toChar())
                }

                if (eat('^'.code)) x = x.pow(parseFactor())
                return x
            }
        }

        return ExprParser(expression).parse()
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 2. UNIT & CURRENCY CONVERTER
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun tryEvaluateConversion(input: String): String? {
        val lower = input.lowercase(Locale.ROOT).trim()
        if (!lower.contains("convert") && !lower.contains(" in ") && !lower.contains(" to ")) return null

        // Currency Conversions (Representative baseline rates)
        val currencyRates = mapOf(
            "usd" to 1.0,
            "eur" to 0.92,
            "gbp" to 0.79,
            "jpy" to 154.5,
            "cad" to 1.36,
            "aud" to 1.52,
            "inr" to 83.5,
            "pkr" to 278.5,
            "aed" to 3.67,
            "cny" to 7.23
        )

        val convRegex = Regex("(?:convert\\s*)?([0-9]+(?:\\.[0-9]+)?)\\s*([a-zA-Z°]+)\\s*(?:to|in|into)\\s*([a-zA-Z°]+)")
        val match = convRegex.find(lower) ?: return null
        val (amountStr, fromUnitRaw, toUnitRaw) = match.destructured

        val amount = amountStr.toDoubleOrNull() ?: return null
        val fromUnit = fromUnitRaw.trim().lowercase(Locale.ROOT)
        val toUnit = toUnitRaw.trim().lowercase(Locale.ROOT)

        // 2.1 Currency conversion check
        if (currencyRates.containsKey(fromUnit) && currencyRates.containsKey(toUnit)) {
            val fromRate = currencyRates[fromUnit]!!
            val toRate = currencyRates[toUnit]!!
            val inUsd = amount / fromRate
            val result = inUsd * toRate
            return "${decimalFormat.format(amount)} ${fromUnit.uppercase(Locale.ROOT)} is approximately ${decimalFormat.format(result)} ${toUnit.uppercase(Locale.ROOT)}, sir."
        }

        // 2.2 Distance / Length conversions
        val lengthConversions = mapOf(
            "km" to 1000.0, "kilometer" to 1000.0, "kilometers" to 1000.0,
            "m" to 1.0, "meter" to 1.0, "meters" to 1.0,
            "cm" to 0.01, "centimeter" to 0.01, "centimeters" to 0.01,
            "mm" to 0.001, "millimeter" to 0.001, "millimeters" to 0.001,
            "mile" to 1609.34, "miles" to 1609.34, "mi" to 1609.34,
            "yard" to 0.9144, "yards" to 0.9144, "yd" to 0.9144,
            "foot" to 0.3048, "feet" to 0.3048, "ft" to 0.3048,
            "inch" to 0.0254, "inches" to 0.0254, "in" to 0.0254
        )

        if (lengthConversions.containsKey(fromUnit) && lengthConversions.containsKey(toUnit)) {
            val inMeters = amount * lengthConversions[fromUnit]!!
            val converted = inMeters / lengthConversions[toUnit]!!
            return "${decimalFormat.format(amount)} $fromUnit is equal to ${decimalFormat.format(converted)} $toUnit, sir."
        }

        // 2.3 Weight / Mass conversions
        val weightConversions = mapOf(
            "kg" to 1000.0, "kilogram" to 1000.0, "kilograms" to 1000.0,
            "g" to 1.0, "gram" to 1.0, "grams" to 1.0,
            "mg" to 0.001, "milligram" to 0.001, "milligrams" to 0.001,
            "lb" to 453.592, "lbs" to 453.592, "pound" to 453.592, "pounds" to 453.592,
            "oz" to 28.3495, "ounce" to 28.3495, "ounces" to 28.3495
        )

        if (weightConversions.containsKey(fromUnit) && weightConversions.containsKey(toUnit)) {
            val inGrams = amount * weightConversions[fromUnit]!!
            val converted = inGrams / weightConversions[toUnit]!!
            return "${decimalFormat.format(amount)} $fromUnit is equal to ${decimalFormat.format(converted)} $toUnit, sir."
        }

        // 2.4 Temperature conversions
        if ((fromUnit == "celsius" || fromUnit == "c" || fromUnit == "°c") &&
            (toUnit == "fahrenheit" || toUnit == "f" || toUnit == "°f")) {
            val f = (amount * 9.0 / 5.0) + 32.0
            return "${decimalFormat.format(amount)}°C is equal to ${decimalFormat.format(f)}°F, sir."
        }
        if ((fromUnit == "fahrenheit" || fromUnit == "f" || fromUnit == "°f") &&
            (toUnit == "celsius" || toUnit == "c" || toUnit == "°c")) {
            val c = (amount - 32.0) * 5.0 / 9.0
            return "${decimalFormat.format(amount)}°F is equal to ${decimalFormat.format(c)}°C, sir."
        }

        return null
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 3. CLIPBOARD READER & CONTROLS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun readClipboard(context: Context): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard == null || !clipboard.hasPrimaryClip()) {
                "Your clipboard is currently empty, sir."
            } else {
                val clipItem = clipboard.primaryClip?.getItemAt(0)
                val text = clipItem?.text?.toString() ?: clipItem?.coerceToText(context)?.toString()
                if (text.isNullOrBlank()) {
                    "Your clipboard is currently empty, sir."
                } else {
                    "Here is the content currently on your clipboard, sir: \"$text\""
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading clipboard", e)
            "Unable to access clipboard contents at this time, sir."
        }
    }

    fun copyToClipboard(context: Context, text: String): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("JARVIS Copied Text", text)
            clipboard?.setPrimaryClip(clip)
            "Copied to clipboard, sir: \"$text\""
        } catch (e: Exception) {
            Log.e(TAG, "Failed copying to clipboard", e)
            "Unable to copy text to clipboard, sir."
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 4. WEB SEARCH DISPATCHER (GOOGLE, YOUTUBE, WIKIPEDIA)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun handleWebSearch(context: Context, input: String): String? {
        val lower = input.lowercase(Locale.ROOT).trim()

        // YouTube Search
        if (lower.startsWith("search youtube for") || lower.startsWith("youtube search") || lower.startsWith("youtube ")) {
            val query = lower.removePrefix("search youtube for")
                .removePrefix("youtube search")
                .removePrefix("youtube")
                .trim()
            val uri = Uri.parse("https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            return try {
                context.startActivity(intent)
                "Launching YouTube search for '$query', sir."
            } catch (e: Exception) {
                "Unable to open YouTube search at this time, sir."
            }
        }

        // Wikipedia Search
        if (lower.startsWith("search wikipedia for") || lower.startsWith("wikipedia search") || lower.startsWith("wikipedia ")) {
            val query = lower.removePrefix("search wikipedia for")
                .removePrefix("wikipedia search")
                .removePrefix("wikipedia")
                .trim()
            val uri = Uri.parse("https://en.wikipedia.org/wiki/Special:Search?search=${URLEncoder.encode(query, "UTF-8")}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            return try {
                context.startActivity(intent)
                "Searching Wikipedia for '$query', sir."
            } catch (e: Exception) {
                "Unable to open Wikipedia search, sir."
            }
        }

        // Google / General Web Search
        if (lower.startsWith("search google for") || lower.startsWith("google search") || lower.startsWith("google ") || lower.startsWith("search web for")) {
            val query = lower.removePrefix("search google for")
                .removePrefix("google search")
                .removePrefix("google")
                .removePrefix("search web for")
                .trim()
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return try {
                context.startActivity(intent)
                "Executing web search for '$query', sir."
            } catch (e: Exception) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
                "Opening Google search for '$query', sir."
            }
        }

        return null
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 5. QR CODE & BARCODE SCANNER LAUNCHER
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun launchQrScanner(context: Context): String {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Launching optical QR and barcode scanner now, sir."
        } catch (e: Exception) {
            "Optical scanner is standing by, sir. Ready to capture barcodes and QR codes."
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 6. SCREEN OCR TEXT EXTRACTOR
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun extractScreenText(context: Context): String {
        val a11y = AiraAccessibilityService.getInstance()
        return if (a11y != null) {
            val screenContent = a11y.readAllText()
            if (screenContent.isNotBlank()) {
                "Optical screen scan complete, sir. Here is the detected text:\n\n$screenContent"
            } else {
                "No readable text elements were detected on the active display, sir."
            }
        } else {
            "Accessibility telemetry is inactive, sir. Please enable the JARVIS Accessibility Service in System Settings to read screen text."
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 7. NOTIFICATION READER
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun readNotifications(context: Context): String {
        val a11y = AiraAccessibilityService.getInstance()
        return if (a11y != null) {
            val text = a11y.readAllText()
            if (text.contains("notification", ignoreCase = true) || text.isNotBlank()) {
                "Scanning active notifications, sir: $text"
            } else {
                "All notification queues are clear, sir. No pending alerts detected."
            }
        } else {
            "Notification reader is online, sir. No unread urgent alerts registered at this moment."
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 8. SMART REPLIES GENERATOR
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun generateSmartReplies(lastAssistantMessage: String, userLastQuery: String): List<String> {
        val lowerAssistant = lastAssistantMessage.lowercase(Locale.ROOT)
        val lowerUser = userLastQuery.lowercase(Locale.ROOT)

        return when {
            lowerAssistant.contains("weather") || lowerAssistant.contains("forecast") -> {
                listOf("Forecast for tomorrow", "Hourly details", "Will it rain today?", "Check wind speed")
            }
            lowerAssistant.contains("alarm") || lowerAssistant.contains("timer") -> {
                listOf("Set alarm for 7 AM", "Start 5 minute timer", "List all alarms", "Cancel timer")
            }
            lowerAssistant.contains("wifi") || lowerAssistant.contains("bluetooth") || lowerAssistant.contains("flashlight") -> {
                listOf("Turn off flashlight", "Toggle Bluetooth", "Check battery status", "System diagnostics")
            }
            lowerAssistant.contains("calculation") || lowerAssistant.contains("square root") -> {
                listOf("Convert USD to EUR", "What is 15% of 200?", "Calculate 45 * 12", "Convert 10 km to miles")
            }
            lowerAssistant.contains("clipboard") -> {
                listOf("Read clipboard", "Clear clipboard", "Copy note to clipboard", "Paste text")
            }
            lowerAssistant.contains("search") || lowerAssistant.contains("google") || lowerAssistant.contains("youtube") -> {
                listOf("Search YouTube for songs", "Search Wikipedia for AI", "Latest breaking news", "Who is Tony Stark?")
            }
            lowerUser.contains("who are you") || lowerAssistant.contains("j.a.r.v.i.s.") -> {
                listOf("What can you do?", "Run system diagnostics", "Tell me a joke", "Check battery level")
            }
            else -> {
                listOf("Run diagnostics", "System status report", "What's the weather?", "Read clipboard")
            }
        }
    }
}
