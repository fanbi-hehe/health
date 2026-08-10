package com.example.health.domain.action

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextToolCallParserTest {

    private val sample = """
        <|DSML|I tool_calls>
        <invoke name="record_training">
        <parameter name="sets">4</parameter>
        <parameter name="exercise_name">离心引体向上</parameter>
        <parameter name="reps">6</parameter>
        </invoke>
        <invoke name="record_training">
        <parameter name="sets" string="false">3</parameter>
        <parameter name="exercise_name">反手引体向上</parameter>
        <parameter name="reps">力竭</parameter>
        </invoke>
        </|DSML|I tool_calls>
    """.trimIndent()

    @Test
    fun `提取多个工具调用`() {
        val calls = TextToolCallParser.extractToolCalls(sample)
        assertEquals(2, calls.size)
        assertEquals("record_training", calls[0].name)
        assertEquals("离心引体向上", calls[0].arguments["exercise_name"])
        assertEquals("4", calls[0].arguments["sets"])
        assertEquals("6", calls[0].arguments["reps"])
        assertEquals("反手引体向上", calls[1].arguments["exercise_name"])
        assertEquals("力竭", calls[1].arguments["reps"])
    }

    @Test
    fun `参数带类型属性也能解析`() {
        val calls = TextToolCallParser.extractToolCalls(sample)
        assertEquals("3", calls[1].arguments["sets"])
    }

    @Test
    fun `剥离工具调用与包裹标签`() {
        val stripped = TextToolCallParser.stripToolCalls(sample)
        assertFalse(stripped.contains("invoke"))
        assertFalse(stripped.contains("DSML"))
        assertFalse(stripped.contains("parameter"))
    }

    @Test
    fun `内容检测`() {
        assertTrue(TextToolCallParser.containsToolCalls(sample))
        assertFalse(TextToolCallParser.containsToolCalls("今天状态怎么样？"))
    }

    @Test
    fun `混合文本只保留可读部分`() {
        val mixed = "我先看一下你的数据。\n<invoke name=\"record_training\"><parameter name=\"sets\">1</parameter></invoke>\n好了，已完成。"
        val stripped = TextToolCallParser.stripToolCalls(mixed)
        assertTrue(stripped.contains("我先看一下"))
        assertTrue(stripped.contains("好了，已完成"))
        assertFalse(stripped.contains("invoke"))
    }
}
