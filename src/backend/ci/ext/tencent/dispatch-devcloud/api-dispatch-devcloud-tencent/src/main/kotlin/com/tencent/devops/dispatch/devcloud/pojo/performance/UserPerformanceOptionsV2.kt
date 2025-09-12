package com.tencent.devops.dispatch.devcloud.pojo.performance

import com.tencent.devops.dispatch.devcloud.pojo.devcloud.PerformanceData
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "DevCloud性能用户选项配置v2版本")
data class UserPerformanceOptionsV2(
    @get:Schema(title = "默认选项")
    val defaultUid: String,
    @get:Schema(title = "性能配置")
    val performanceList: List<PerformanceData>
)