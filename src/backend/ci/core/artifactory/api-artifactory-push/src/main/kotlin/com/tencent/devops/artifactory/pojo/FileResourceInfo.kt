package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("待分发文件资源信息")
class FileResourceInfo(
    @ApiModelProperty("项目Id", required = true)
    val projectId: String,
    @ApiModelProperty("流水线Id", required = true)
    val pipelineId: String,
    @ApiModelProperty("构件Id", required = true)
    val buildId: String,
    @ApiModelProperty("文件名", required = true)
    val fileName: String,
    @ApiModelProperty("是否为流水线仓库", required = false)
    val isCustom: Boolean? = false
)