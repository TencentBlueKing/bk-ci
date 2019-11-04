package com.tencent.devops.process.pojo.measure

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线构建度量数据")
data class PipelineBuildData(
    @ApiModelProperty("流水线对应的项目id")
    val projectId: String,
    @ApiModelProperty("流水线的id")
    val pipelineId: String,
    @ApiModelProperty("模板的id")
    val templateId: String,
    @ApiModelProperty("流水线的这次构建的id")
    val buildId: String,
    @ApiModelProperty("流水线的启动时间")
    val beginTime: Long,
    @ApiModelProperty("流水线的结束时间")
    val endTime: Long,
    @ApiModelProperty("流水线的启动方式")
    val startType: StartType,
    @ApiModelProperty("流水线的启动用户")
    val buildUser: String,
    @ApiModelProperty("流水线的是否并行")
    val isParallel: Boolean,
    @ApiModelProperty("流水线的构建结果")
    val buildResult: BuildStatus,
    @ApiModelProperty("流水线Element结构")
    val pipeline: String,
    @ApiModelProperty("构建版本号")
    val buildNum: Int,
    @ApiModelProperty("元数据")
    val metaInfo: Map<String, Any>,
    @ApiModelProperty("错误类型", required = false)
    var errorType: String? = null,
    @ApiModelProperty("错误码标识", required = false)
    var errorCode: Int? = null,
    @ApiModelProperty("错误描述", required = false)
    var errorMsg: String? = null
)