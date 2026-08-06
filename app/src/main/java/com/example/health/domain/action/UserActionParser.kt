package com.example.health.domain.action

import kotlin.math.roundToInt

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
        val sets = Regex("(\\d+)\\s*[组組xX×]").find(text)
            ?.groupValues?.get(1)?.toIntOrNull() ?: 1
        // 优先匹配"10次"，其次支持"3x12 / 3×12 / 3组12"写法
        val reps = Regex("(\\d+)\\s*次").find(text)
            ?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("[组組xX×]\\s*(\\d+)").find(text)
                ?.groupValues?.get(1)?.toIntOrNull()
            ?: 0

        val hasGroupInfo = sets > 1 || reps > 0
        val hasTrigger = listOf(
            "做了", "练了", "记录一下", "记一下", "记录", "训练", "练", "做"
        ).any { text.contains(it) }
        // 没有训练信号（无触发词且无组次信息）不解析，避免误伤普通对话
        if (!hasTrigger && !hasGroupInfo) return null

        val exercise = knownExercises
            .sortedByDescending { it.length }
            .firstOrNull { text.contains(it) }
            // 动作库没有的动作名：从触发词后、数字前提取
            ?: extractExerciseName(text) ?: return null

        // 重量只匹配带单位（kg/公斤/千克），避免把组数次数当重量
        val weight = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:kg|公斤|千克)")
            .find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        return UserAction.RecordTraining(
            exerciseName = exercise,
            // 只说"练了深蹲"没有组次时，写入默认值并在备注/回复中提示
            sets = if (hasGroupInfo) sets.coerceAtLeast(1) else 1,
            reps = if (hasGroupInfo) reps.coerceAtLeast(1) else 0,
            weightKg = weight
        )
    }

    /** 提取动作库外的动作名：优先取最后一个触发词后的文字，其次取句子开头到第一个数字。 */
    private fun extractExerciseName(text: String): String? {
        // 1) 最后一个触发词后的文字（"记录一下我做了深蹲"取"做了"后的"深蹲"）
        val triggerRegex = Regex(
            "(?:做了|练了|记录一下|记一下|记录|训练|练|做)\\s*([^\\d，。,.!！?？\\s]+(?:\\s[^\\d，。,.!！?？\\s]+)?)"
        )
        triggerRegex.findAll(text).lastOrNull()?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.isNotBlank() && name.length <= 12) return name
        }

        // 2) 无触发词的句式："深蹲 4组 10次 60kg" → 取开头到数字前
        Regex("^\\s*([^\\d，。,.!！?？\\s]+(?:\\s[^\\d，。,.!！?？\\s]+)?)\\s*\\d")
            .find(text)?.let { match ->
                val name = match.groupValues[1].trim()
                if (name.isNotBlank() && name.length <= 12) return name
            }
        return null
    }

    // ── 添加食物 ──

    private fun parseAddFood(text: String): UserAction.AddFood? {
        val hasTrigger = listOf("添加", "新增", "加入", "加个").any { text.contains(it) }
        if (!hasTrigger) return null

        // 名称：触发词后到第一个数字/标点前
        val name = Regex(
            "(?:添加食物|新增食物|添加|新增|加入|加个)\\s*[，,、]?\\s*([^\\d每，。,.!！?？\\n]+)"
        ).find(text)?.groupValues?.get(1)?.replace("食物", "")?.trim() ?: return null
        if (name.isBlank() || name.length > 20) return null

        // 能量：数字 + 单位（支持千焦/kJ 自动换算为 kcal，1 kcal = 4.184 kJ）
        val energy = Regex(
            "(?:每100g|每百克|每100克|每 100g|每 100克)?\\s*(\\d+(?:\\.\\d+)?)\\s*(千焦|kJ|焦|kcal|千卡|大卡|卡路里|卡)"
        ).find(text) ?: return null
        val energyValue = energy.groupValues[1].toDoubleOrNull() ?: return null
        val unit = energy.groupValues[2]
        val calories = when {
            unit in listOf("千焦", "kJ", "焦") -> (energyValue / 4.184).roundToInt()
            else -> energyValue.roundToInt()
        }
        if (name.isBlank() || calories <= 0) return null

        // 蛋白质（可选）："蛋白质75.7克" 或 "75.7克蛋白质"
        // 先匹配关键词在前的写法，避免"50克 蛋白质"把碳水的 50 误认成蛋白质
        val protein = Regex("蛋白质[^\\d]{0,8}(\\d+(?:\\.\\d+)?)\\s*克").find(text)
            ?.groupValues?.get(1)?.toDoubleOrNull()
            ?: Regex("(\\d+(?:\\.\\d+)?)\\s*克\\s*蛋白质").find(text)
                ?.groupValues?.get(1)?.toDoubleOrNull()
            ?: 0.0

        // 碳水/脂肪（可选）："碳水50克" "脂肪10克"
        val carbs = Regex("碳水(?:化合物)?[^\\d]{0,8}(\\d+(?:\\.\\d+)?)\\s*克").find(text)
            ?.groupValues?.get(1)?.toDoubleOrNull()
            ?: Regex("(\\d+(?:\\.\\d+)?)\\s*克\\s*碳水(?:化合物)?").find(text)
                ?.groupValues?.get(1)?.toDoubleOrNull()
            ?: 0.0
        val fat = Regex("脂肪[^\\d]{0,8}(\\d+(?:\\.\\d+)?)\\s*克").find(text)
            ?.groupValues?.get(1)?.toDoubleOrNull()
            ?: Regex("(\\d+(?:\\.\\d+)?)\\s*克\\s*脂肪").find(text)
                ?.groupValues?.get(1)?.toDoubleOrNull()
            ?: 0.0

        return UserAction.AddFood(name, calories, protein, carbs, fat)
    }

    // ── 修改食物热量 ──

    private fun parseUpdateFood(text: String): UserAction.UpdateFood? {
        val match = Regex(
            "(?:把|将)?\\s*([^\\d，。,.!！?？\\s]+?)\\s*(?:的)?热量(?:改成|改为|调成|设为|调整为)\\s*(\\d+(?:\\.\\d+)?)\\s*(千焦|kJ|焦|kcal|千卡|大卡|卡路里|卡)?"
        ).find(text) ?: return null

        val name = match.groupValues[1].trim()
        val value = match.groupValues[2].toDoubleOrNull() ?: return null
        val unit = match.groupValues[3]
        val calories = when {
            unit in listOf("千焦", "kJ", "焦") -> (value / 4.184).roundToInt()
            else -> value.roundToInt()
        }
        if (name.isBlank() || calories <= 0) return null
        return UserAction.UpdateFood(name, calories)
    }
}
