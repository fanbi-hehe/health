package com.example.health.domain.action

/**
 * 从用户对话中解析 AI 教练可执行的本地操作。
 *
 * 支持：
 * - 训练记录："我做了深蹲 4组 10次 60kg"
 * - 添加食物："添加食物 红烧肉 300kcal"
 * - 修改食物："把红烧肉的热量改成 350"
 * - 删除意图：一律拦截，不执行
 */
object UserActionParser {

    private val deleteKeywords = listOf("删除", "删掉", "移除", "去掉", "清除", "删了")
    private val dataKeywords = listOf(
        "食物", "记录", "动作", "训练", "模板", "条目", "热量", "库"
    )

    fun parse(text: String, knownExercises: List<String>): UserAction {
        val clean = text.trim()
        if (clean.isBlank()) return UserAction.None

        // 删除意图优先拦截（含数据关键词、已知动作名、或短句如"删除红烧肉"）
        if (deleteKeywords.any { clean.contains(it) } &&
            (dataKeywords.any { clean.contains(it) } ||
                knownExercises.any { clean.contains(it) } ||
                clean.length <= 12)
        ) {
            return UserAction.DeleteRequested
        }

        parseRecordTraining(clean, knownExercises)?.let { return it }
        parseAddFood(clean)?.let { return it }
        parseUpdateFood(clean)?.let { return it }
        return UserAction.None
    }

    // ── 训练记录 ──

    private fun parseRecordTraining(
        text: String,
        knownExercises: List<String>
    ): UserAction.RecordTraining? {
        val hasTrigger = listOf("做了", "练了", "记录一下", "记录", "练", "做").any { text.contains(it) }
        if (!hasTrigger) return null

        val exercise = knownExercises
            .sortedByDescending { it.length }
            .firstOrNull { text.contains(it) } ?: return null

        val sets = Regex("(\\d+)\\s*[组組xX×]").find(text)
            ?.groupValues?.get(1)?.toIntOrNull() ?: 1
        // 优先匹配"10次"，其次支持"3x12 / 3×12 / 3组12"写法
        val reps = Regex("(\\d+)\\s*次").find(text)
            ?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("[组組xX×]\\s*(\\d+)").find(text)
                ?.groupValues?.get(1)?.toIntOrNull()
            ?: 0
        // 重量只匹配带单位（kg/公斤/千克），避免把组数次数当重量
        val weight = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:kg|公斤|千克)")
            .find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        if (reps <= 0 && sets <= 1) return null
        return UserAction.RecordTraining(
            exerciseName = exercise,
            sets = sets.coerceAtLeast(1),
            reps = reps.coerceAtLeast(1),
            weightKg = weight
        )
    }

    // ── 添加食物 ──

    private fun parseAddFood(text: String): UserAction.AddFood? {
        val hasTrigger = listOf("添加", "新增", "加入", "加个").any { text.contains(it) }
        if (!hasTrigger) return null

        val match = Regex(
            "(?:添加|新增|加入|加个)\\s*([^\\d，。,.!！?？]+?)\\s*(\\d+)\\s*(?:kcal|千卡|大卡|卡路里|卡)"
        ).find(text) ?: return null

        val name = match.groupValues[1].replace("食物", "").trim()
        val calories = match.groupValues[2].toIntOrNull() ?: return null
        if (name.isBlank() || calories <= 0) return null
        return UserAction.AddFood(name, calories)
    }

    // ── 修改食物热量 ──

    private fun parseUpdateFood(text: String): UserAction.UpdateFood? {
        val match = Regex(
            "(?:把|将)?\\s*([^\\d，。,.!！?？\\s]+?)\\s*(?:的)?热量(?:改成|改为|调成|设为|调整为)\\s*(\\d+)\\s*(?:kcal|千卡|大卡|卡路里|卡)?"
        ).find(text) ?: return null

        val name = match.groupValues[1].trim()
        val calories = match.groupValues[2].toIntOrNull() ?: return null
        if (name.isBlank() || calories <= 0) return null
        return UserAction.UpdateFood(name, calories)
    }
}
