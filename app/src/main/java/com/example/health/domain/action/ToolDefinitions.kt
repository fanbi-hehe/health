package com.example.health.domain.action

import com.example.health.data.remote.dto.FunctionSpec
import com.example.health.data.remote.dto.Tool

/**
 * AI 教练可调用的工具定义（OpenAI 兼容 function calling）。
 *
 * 安全原则：只有"写入/修改"类操作，**没有删除工具**。
 */
object ToolDefinitions {

    val coachTools: List<Tool> = listOf(
        Tool(
            function = FunctionSpec(
                name = "record_training",
                description = "记录用户今天完成的训练动作（如：深蹲 4组 10次 60kg）。如果今天已有同名动作则不会重复写入。",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "exercise_name" to mapOf("type" to "string", "description" to "动作名称，如：深蹲、卧推"),
                        "sets" to mapOf("type" to "integer", "description" to "组数（1-50）"),
                        "reps" to mapOf("type" to "integer", "description" to "每组次数（0-200）"),
                        "weight_kg" to mapOf("type" to "number", "description" to "负重公斤，无负重填 0")
                    ),
                    "required" to listOf("exercise_name", "sets", "reps")
                )
            )
        ),
        Tool(
            function = FunctionSpec(
                name = "add_food",
                description = "把食物添加进自定义食物库。热量必须是每 100 克的值并换算为 kcal（1 kcal = 4.184 kJ），可附带每 100 克的蛋白质/碳水/脂肪克数。",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "name" to mapOf("type" to "string", "description" to "食物名称"),
                        "calories_per_100g" to mapOf("type" to "integer", "description" to "每 100 克热量（kcal，1-2000）"),
                        "protein_per_100g" to mapOf("type" to "number", "description" to "每 100 克蛋白质克数（可选）"),
                        "carbs_per_100g" to mapOf("type" to "number", "description" to "每 100 克碳水化合物克数（可选）"),
                        "fat_per_100g" to mapOf("type" to "number", "description" to "每 100 克脂肪克数（可选）")
                    ),
                    "required" to listOf("name", "calories_per_100g")
                )
            )
        ),
        Tool(
            function = FunctionSpec(
                name = "update_food",
                description = "修改食物库中已有食物的每 100 克热量（kcal）。",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "name" to mapOf("type" to "string", "description" to "食物名称（模糊匹配）"),
                        "calories_per_100g" to mapOf("type" to "integer", "description" to "新的每 100 克热量（kcal，1-2000）")
                    ),
                    "required" to listOf("name", "calories_per_100g")
                )
            )
        )
    )
}
