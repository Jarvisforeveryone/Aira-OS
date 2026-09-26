package com.example

import android.view.accessibility.AccessibilityNodeInfo
import com.example.service.AiraAccessibilityService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccessibilityPrivacyTest {

    @Test
    fun testSensitivePasswordDetection() {
        val service = AiraAccessibilityService()
        val node = AccessibilityNodeInfo.obtain()
        node.isPassword = true

        val isSensitive = service.isSensitiveNode(node)
        assertTrue(isSensitive)
    }

    @Test
    fun testSensitivePinDetection() {
        val service = AiraAccessibilityService()
        val node = AccessibilityNodeInfo.obtain()
        node.viewIdResourceName = "com.bank.app:id/enter_user_pin"

        val isSensitive = service.isSensitiveNode(node)
        assertTrue(isSensitive)
    }

    @Test
    fun testSensitiveCvvDetection() {
        val service = AiraAccessibilityService()
        val node = AccessibilityNodeInfo.obtain()
        node.viewIdResourceName = "com.shop.app:id/credit_card_cvv"

        val isSensitive = service.isSensitiveNode(node)
        assertTrue(isSensitive)
    }

    @Test
    fun testNonSensitiveNormalTextDetection() {
        val service = AiraAccessibilityService()
        val node = AccessibilityNodeInfo.obtain()
        node.isPassword = false
        node.viewIdResourceName = "com.example:id/chat_message_text"
        node.text = "Hello Aira, what is today's schedule?"

        val isSensitive = service.isSensitiveNode(node)
        assertFalse(isSensitive)
    }
}
