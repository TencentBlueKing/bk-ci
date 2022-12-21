package com.tencent.devops.metrics.pojo.po

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.math.BigDecimal
import java.time.LocalDateTime

@ApiModel("第三方平台数据对象")
data class ThirdPlatformDatePO(
    @ApiModelProperty("主键ID")
    val id: Long,
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("codecc检查代码库平均分")
    val repoCodeccAvgScore: BigDecimal?,
    @ApiModelProperty("已解决缺陷数")
    val resolvedDefectNum: Int?,
    @ApiModelProperty("使用质量红线的流水线执行被拦截次数")
    val qualityPipelineInterceptionNum: Int?,
    @ApiModelProperty("使用质量红线的流水线执行总次数")
    val qualityPipelineExecuteNum: Int?,
    @ApiModelProperty("编译加速节省时间，单位：秒")
    val turboSaveTime: BigDecimal?,
    @ApiModelProperty("统计时间")
    val statisticsTime: LocalDateTime,
    @ApiModelProperty("创建者")
    val creator: String? = null,
    @ApiModelProperty("修改者")
    val modifier: String? = null,
    @ApiModelProperty("修改时间")
    val updateTime: LocalDateTime,
    @ApiModelProperty("创建时间")
    val createTime: LocalDateTime
)
