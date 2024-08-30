package com.tencent.devops.common.event.pojo.measure

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class ProjectUserOperateMetricsData(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "统计日期")
    val theDate: LocalDate,
    @get:Schema(title = "操作")
    val operate: String
) {
    private fun getProjectUserOperateMetricsKey(): String {
        return "key:$projectId:$userId:$operate:$theDate"
    }

    override fun equals(other: Any?): Boolean {
        if (other is ProjectUserOperateMetricsData) {
            return other.projectId == projectId && other.userId == userId &&
                other.theDate == theDate && other.operate == operate
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return getProjectUserOperateMetricsKey().hashCode()
    }
}
