package com.example.health.domain.action

/**
 * 解析模型以文本形式（DSML/XML 风格）输出的工具调用。
 *
 * 部分模型/代理会把工具调用写进回复内容而不是标准 JSON `tool_calls` 字段，格式如：
 * ```
 * <|DSML|I tool_calls>
 * <invoke name="record_training">
 *   <parameter name="sets">4</parameter>
 *   <parameter name="exercise_name">离心引体向上</parameter>
 *   <parameter name="reps">6</parameter>
 * </invoke>
 * </|DSML|I tool_calls>
 * ```
 *
 * 职责：
 * - [extractToolCalls]：提取所有 `<invoke>` 块（工具名 + 参数）；
 * - [stripToolCalls]：从内容中移除工具调用块与 DSML 包裹标签，只留用户可读文本。
 */
object TextToolCallParser {

    data class ParsedToolCall(
        val name: String,
        val arguments: Map<String, String>
    )

    private val invokeRegex = Regex(
        "<invoke\\s+name=[\"']([^\"']+)[\"']>([\\s\\S]*?)</invoke>",
        RegexOption.IGNORE_CASE
    )

    private val parameterRegex = Regex(
        "<parameter\\s+name=[\"']([^\"']+)[\"'][^>]*>([\\s\\S]*?)</parameter>",
        RegexOption.IGNORE_CASE
    )

    /** 提取内容中的所有文本工具调用。 */
    fun extractToolCalls(content: String): List<ParsedToolCall> {
        if (content.isBlank()) return emptyList()
        return invokeRegex.findAll(content).mapNotNull { match ->
            val name = match.groupValues[1].trim()
            if (name.isBlank()) return@mapNotNull null
            val body = match.groupValues[2]
            val args = parameterRegex.findAll(body).associate { param ->
                param.groupValues[1].trim() to param.groupValues[2].trim()
            }
            ParsedToolCall(name, args)
        }.toList()
    }

    /** 移除工具调用块与 DSML 包裹标签，返回可读文本。 */
    fun stripToolCalls(content: String): String {
        if (content.isBlank()) return content
        return content
            .replace(invokeRegex, "")
            // DSML 包裹标签（含开头/结尾、可能带空白）
            .replace(Regex("</?\\|?DSML[^>]*>", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    /** 内容中是否包含文本工具调用。 */
    fun containsToolCalls(content: String): Boolean = extractToolCalls(content).isNotEmpty()
}
