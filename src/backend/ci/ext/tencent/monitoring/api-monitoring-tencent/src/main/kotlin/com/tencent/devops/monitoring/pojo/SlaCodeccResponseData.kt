package com.tencent.devops.monitoring.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("SLA--CodeCC--统计数据")
data class SlaCodeccResponseData(
    @ApiModelProperty("执行次数")
    val count: Int,
    @ApiModelProperty("耗时")
    val costTime: Long,
    @ApiModelProperty("成功率")
    val successRate: Double,
    @ApiModelProperty("错误码分布")
    val errorPie: List<ErrorPie>
) {
    companion object {
        val EMPTY = SlaCodeccResponseData(
            0,
            0,
            0.0,
            listOf()
        )
    }
}

@ApiModel("SLA--CodeCC--错误码分布")
data class ErrorPie(
    @ApiModelProperty("错误码")
    val code: String?,
    @ApiModelProperty("错误信息")
    val message: String?,
    @ApiModelProperty("次数")
    val count: Int
)
