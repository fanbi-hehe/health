package com.example.health.ui.chat

import com.example.health.data.local.entity.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ChatViewModelTest {

    @Test
    fun `estimateTokens 按字符四分之一估算`() {
        assertEquals(25, ChatViewModel.estimateTokens("a".repeat(100)))
        assertEquals(0, ChatViewModel.estimateTokens(""))
    }

    @Test
    fun `isNewDay 判断跨天`() {
        assertTrue(ChatViewModel.isNewDay("2000-01-01"))
        assertFalse(ChatViewModel.isNewDay(LocalDate.now().toString()))
    }

    @Test
    fun `shouldCompact 未达阈值不压缩`() {
        val history = listOf(
            ChatMessage(role = "user", content = "a".repeat(1000), timestamp = 0L)
        )
        assertFalse(ChatViewModel.shouldCompact("", history))
    }

    @Test
    fun `shouldCompact 超过阈值触发压缩`() {
        val history = listOf(
            ChatMessage(role = "user", content = "a".repeat(40000), timestamp = 0L)
        )
        assertTrue(ChatViewModel.shouldCompact("", history))
    }

    @Test
    fun `shouldCompact 摘要也计入 token`() {
        assertTrue(ChatViewModel.shouldCompact("b".repeat(40000), emptyList()))
    }
}
