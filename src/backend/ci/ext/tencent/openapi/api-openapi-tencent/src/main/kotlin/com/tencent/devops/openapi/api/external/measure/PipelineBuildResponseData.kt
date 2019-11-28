package com.tencent.devops.openapi.api.external.measure

import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.quality.api.v2.pojo.QualityRuleIntercept
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线构建查询接口响应数据")
data class PipelineBuildResponseData(
    @ApiModelProperty("流水线对应的项目id")
    val projectId: String = "",
    @ApiModelProperty("projectCode")
    val projectCode: String = "",
    @ApiModelProperty("bgName")
    val bgName: String = "",
    @ApiModelProperty("centerName")
    val centerName: String = "",
    @ApiModelProperty("deptName")
    val deptName: String = "",
    @ApiModelProperty("流水线的id")
    val pipelineId: String = "",
    @ApiModelProperty("流水线的名称")
    val pipelineName: String = "",
    @ApiModelProperty("构建版本号")
    val buildNum: Int = 0,
    @ApiModelProperty("流水线的这次构建的id")
    val buildId: String = "",
    @ApiModelProperty("流水线的启动时间")
    val beginTime: Long = 0,
    @ApiModelProperty("流水线的结束时间")
    val endTime: Long = 0,
    @ApiModelProperty("流水线的启动方式")
    val startType: String = "",
    @ApiModelProperty("流水线的启动用户")
    val buildUser: String = "",
    @ApiModelProperty("流水线的是否并行")
    val isParallel: Boolean = false,
    @ApiModelProperty("流水线的构建结果")
    val buildResult: String = "",
    @ApiModelProperty("流水线Element结构")
    val elements: List<BuildElementData> = emptyList(),
    @ApiModelProperty("Codecc报告")
    val codeccReport: CodeccCallback? = null,
    @ApiModelProperty("质量红线数据", required = false)
    var qualityData: List<QualityRuleIntercept>? = null,
    @ApiModelProperty("是否 PCG 公共构件资源")
    val dispatchTypeContainsPcg: Boolean = false,
    @ApiModelProperty("templateId", required = false)
    val templateId: String? = "",
    @ApiModelProperty("model", required = false)
    val model: String? = "",
    @ApiModelProperty("错误类型", required = false)
    val errorType: String? = null,
    @ApiModelProperty("错误码标识", required = false)
    val errorCode: Int? = null,
    @ApiModelProperty("错误信息描述", required = false)
    val errorMsg: String? = null
)