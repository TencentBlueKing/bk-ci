package com.tencent.devops.quality.api.v2.pojo

class ControlPointPosition(
    val name: String
) {
    val cnName: String = POSITION_NAME_MAP[name] ?: ""
    companion object {
        val BEFORE_POSITION = "BEFORE"
        val AFTER_POSITION = "AFTER"
        val POSITION_NAME_MAP = mapOf(
                BEFORE_POSITION to "准入-满足条件才能执行控制点",
                AFTER_POSITION to "准出-满足条件才能执行后续插件"
        )
    }
}