package com.tencent.devops.metrics.pojo.po

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "第三方平台数据对象")
data class ThirdPlatformDatePO(
    @Schema(description = "主键ID")
    val id: Long,
    @Schema(description = "项目ID")
    val projectId: String,
    @Schema(description = "codecc检查代码库平均分")
    val repoCodeccAvgScore: BigDecimal?,
    @Schema(description = "已解决缺陷数")
    val resolvedDefectNum: Int?,
    @Schema(description = "使用质量红线的流水线执行被拦截次数")
    val qualityPipelineInterceptionNum: Int?,
    @Schema(description = "使用质量红线的流水线执行总次数")
    val qualityPipelineExecuteNum: Int?,
    @Schema(description = "编译加速节省时间，单位：秒")
    val turboSaveTime: BigDecimal?,
    @Schema(description = "统计时间")
    val statisticsTime: LocalDateTime,
    @Schema(description = "创建者")
    val creator: String? = null,
    @Schema(description = "修改者")
    val modifier: String? = null,
    @Schema(description = "修改时间")
    val updateTime: LocalDateTime,
    @Schema(description = "创建时间")
    val createTime: LocalDateTime
)
