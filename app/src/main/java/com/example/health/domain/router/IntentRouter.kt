package com.example.health.domain.router

/**
 * 本地意图路由引擎。
 *
 * 基于关键词/正则匹配，将用户自然语言输入映射为 [IntentQuery] 子类型。
 * 匹配优先级：动作进度 > 热量饮食 > 整体趋势 > 用户档案 > 闲聊。
 *
 * @param text         用户输入文本
 * @param knownExercises 用户历史训练过的动作名称列表（用于动作名匹配），可为空
 * @return 解析后的意图类型
 */
object IntentRouter {

    // ── 关键词词典 ──

    private val dietKeywords = listOf(
        "吃了", "吃", "热量", "卡路里", "kcal", "大卡", "千卡",
        "摄入", "饮食", "食谱", "餐", "食物", "吃了多少",
        "吃了什么", "今天吃", "记录"
    )

    private val progressKeywords = listOf(
        "进步", "变强", "增重", "提升", "增加", "增长",
        "进步了", "长了", "涨了", "重量变化", "以前多少",
        "之前", "过去", "历史", "记录"
    )

    private val overallKeywords = listOf(
        "怎么样", "最近", "状态", "趋势", "总结", "评估",
        "进展", "表现", "整体", "情况", "分析", "回顾",
        "最近表现", "最近状态"
    )

    private val profileKeywords = listOf(
        "目标", "体重目标", "热量目标", "档案", "我的信息",
        "训练天数", "训练经验", "身高", "体重", "设置"
    )

    /** 时间范围模式：匹配 "今天/昨日/昨天/近3天/最近三天/近一周/最近7天" 等 */
    private val timeRangePatterns = listOf(
        Regex("(今天|今日|今[日天])") to "today",
        Regex("(昨天|昨日)") to "yesterday",
        Regex("(3天|三天|最近三|这三)") to "3days",
        Regex("(7天|七天|一周|最近一|这周|本周)") to "7days"
    )

    // ── 公共方法 ──

    /**
     * 解析用户输入，返回意图类型。
     */
    fun resolve(text: String, knownExercises: List<String> = emptyList()): IntentQuery {
        val clean = text.trim()
        if (clean.isBlank()) return IntentQuery.GeneralChat

        // 1. 动作进度（最高优先级 — 动作名 + 进度/重量关键词）
        resolveExerciseProgress(clean, knownExercises)?.let { return it }

        // 2. 热量/饮食
        if (matchesAnyKeyword(clean, dietKeywords)) {
            return IntentQuery.DietCalories(timeRange = detectTimeRange(clean))
        }

        // 3. 整体趋势
        if (matchesAnyKeyword(clean, overallKeywords)) {
            return IntentQuery.OverallSummary
        }

        // 4. 用户档案
        // "体重" alone could be ambiguous; require a profile-specific keyword combo
        if (matchesAnyKeyword(clean, profileKeywords)) {
            return IntentQuery.UserProfile
        }

        // 5. 兜底：闲聊
        return IntentQuery.GeneralChat
    }

    // ── 私有方法 ──

    /**
     * 在用户输入中匹配已知动作名 + 进度关键词。
     * 返回第一个匹配的 ExerciseProgress，或 null。
     */
    private fun resolveExerciseProgress(
        text: String,
        knownExercises: List<String>
    ): IntentQuery.ExerciseProgress? {
        if (knownExercises.isEmpty()) return null

        // 按长度降序排序，优先匹配更长的动作名（避免 "卧推" 匹配到 "哑铃卧推" 之前）
        val sorted = knownExercises.sortedByDescending { it.length }

        for (exercise in sorted) {
            if (text.contains(exercise) && matchesAnyKeyword(text, progressKeywords)) {
                return IntentQuery.ExerciseProgress(exercise)
            }
        }
        return null
    }

    /** 检测文本中提到的时间范围，默认返回 "3days"。 */
    private fun detectTimeRange(text: String): String {
        for ((pattern, range) in timeRangePatterns) {
            if (pattern.containsMatchIn(text)) return range
        }
        return "3days" // 默认近3天
    }

    /** 文本是否匹配任一关键词。 */
    private fun matchesAnyKeyword(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }
}
