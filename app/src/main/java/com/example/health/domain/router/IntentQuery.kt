package com.example.health.domain.router

/**
 * AI 对话意图分类 — 本地意图路由引擎的查询类型。
 *
 * 基于用户输入的关键词/正则匹配，识别 4 大类意图：
 * - 热量类："今天/最近吃了多少热量" → 查 DietRecord
 * - 动作类："深蹲重量进步了吗" → 查 TrainingRecord
 * - 整体趋势："最近怎么样" → 统计摘要
 * - 用户档案/闲聊 → 档案信息或不带数据
 */
sealed class IntentQuery {

    /**
     * 热量/饮食查询。
     * @param timeRange "today" / "yesterday" / "3days" / "7days"
     */
    data class DietCalories(val timeRange: String) : IntentQuery()

    /**
     * 指定动作的训练进度查询。
     * @param exerciseName 匹配到的动作名称
     */
    data class ExerciseProgress(val exerciseName: String) : IntentQuery()

    /** 整体趋势/最近状态查询（近3天摘要 + 30天统计）。 */
    object OverallSummary : IntentQuery()

    /** 运动消耗/步数查询（消耗、步数、跑了/走了多少）。 */
    object ActivitySummary : IntentQuery()

    /** 用户档案/目标查询（体重目标、每日热量目标等）。 */
    object UserProfile : IntentQuery()

    /** 闲聊或未命中 — 不带额外数据直接回复。 */
    object GeneralChat : IntentQuery()
}
