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
                description = "把食物添加进自定义食物库。热量必须是每 100 克的值并换算为 kcal（1 kcal = 4.184 kJ），可附带每 100 克的蛋白质/碳水/脂肪克数；如果用户同时说了本次吃了多少克（如'吃了60克'），用 amount_g 参数一并记录今日饮食。",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "name" to mapOf("type" to "string", "description" to "食物名称"),
                        "calories_per_100g" to mapOf("type" to "integer", "description" to "每 100 克热量（kcal，1-2000）"),
                        "protein_per_100g" to mapOf("type" to "number", "description" to "每 100 克蛋白质克数（可选）"),
                        "carbs_per_100g" to mapOf("type" to "number", "description" to "每 100 克碳水化合物克数（可选）"),
                        "fat_per_100g" to mapOf("type" to "number", "description" to "每 100 克脂肪克数（可选）"),
                        "amount_g" to mapOf("type" to "number", "description" to "本次食用的克数（可选，提供则同时记录今日饮食）")
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
        ),
        Tool(
            function = FunctionSpec(
                name = "generate_training_plan",
                description = "根据用户档案、训练历史和自定义要求，生成一份 7 天个性化训练计划并保存（用户可在训练页查看）。用户说'设计/制定/生成/换一个健身计划'、'帮我安排训练'等时调用。",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "custom_prompt" to mapOf(
                            "type" to "string",
                            "description" to "用户附加的自定义要求（可选），如：侧重腿部、居家无器械、增肌为主"
                        )
                    )
                )
            )
        ),
        Tool(
            function = FunctionSpec(
                name = "record_activity_calories",
                description = "手动记录用户今日的运动消耗热量（如用户说'今天跑步消耗了300大卡'）。多条记录会逐条累加。",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "type" to mapOf("type" to "string", "description" to "运动类型，如：跑步/骑行/步行/力量训练/其他"),
                        "calories_kcal" to mapOf("type" to "integer", "description" to "本次运动消耗热量（kcal，1-5000）"),
                        "duration_minutes" to mapOf("type" to "integer", "description" to "时长分钟（可选）"),
                        "note" to mapOf("type" to "string", "description" to "备注（可选）")
                    ),
                    "required" to listOf("type", "calories_kcal")
                )
            )
        ),
        Tool(
            function = FunctionSpec(
                name = "web_search",
                description = "联网搜索权威信息（营养、运动、健康类问题需要查证时调用）。返回搜索结果摘要与链接。",
                parameters = mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "query" to mapOf("type" to "string", "description" to "搜索关键词")
                    ),
                    "required" to listOf("query")
                )
            )
        )
    )
}
