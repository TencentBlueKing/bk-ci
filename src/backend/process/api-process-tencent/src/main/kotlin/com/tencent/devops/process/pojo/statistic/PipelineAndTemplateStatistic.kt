package com.tencent.devops.process.pojo.statistic

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/9/14
 * @Version 1.0
 */
@ApiModel("流水线与模板统计数据")
data class PipelineAndTemplateStatistic(
    @ApiModelProperty("流水线总数", required = true)
    val pipelineNum: Int,
    @ApiModelProperty("实例化流水线总数", required = true)
    val instancedPipelineNum: Int,
    @ApiModelProperty("模板总数", required = true)
    val templateNum: Int,
    @ApiModelProperty("实例化模板总数", required = true)
    val instancedTemplateNum: Int,
    @ApiModelProperty("原始模板总数", required = true)
    val srcTemplateNum: Int,
    @ApiModelProperty("实例化原始模板总数", required = true)
    val instancedSrcTemplateNum: Int
)