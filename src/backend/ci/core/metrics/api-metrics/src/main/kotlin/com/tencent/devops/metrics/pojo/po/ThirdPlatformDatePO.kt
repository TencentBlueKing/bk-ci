package com.tencent.devops.metrics.pojo.po

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(name = "第三方平台数据对象")
data class ThirdPlatformDatePO(
    @Schema(name = "主键ID")
    val id: Long,
    @Schema(name = "项目ID")
    val projectId: String,
    @Schema(name = "codecc检查代码库平均分")
    val repoCodeccAvgScore: BigDecimal?,
    @Schema(name = "已解决缺陷数")
    val resolvedDefectNum: Int?,
    @Schema(name = "使用质量红线的流水线执行被拦截次数")
    val qualityPipelineInterceptionNum: Int?,
    @Schema(name = "使用质量红线的流水线执行总次数")
    val qualityPipelineExecuteNum: Int?,
    @Schema(name = "编译加速节省时间，单位：秒")
    val turboSaveTime: BigDecimal?,
    @Schema(name = "统计时间")
    val statisticsTime: LocalDateTime,
    @Schema(name = "创建者")
    val creator: String? = null,
    @Schema(name = "修改者")
    val modifier: String? = null,
    @Schema(name = "修改时间")
    val updateTime: LocalDateTime,
    @Schema(name = "创建时间")
    val createTime: LocalDateTime
)
