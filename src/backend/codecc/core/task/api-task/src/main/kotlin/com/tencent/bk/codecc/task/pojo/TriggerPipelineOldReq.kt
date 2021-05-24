package com.tencent.bk.codecc.task.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线触发请求体")
data class TriggerPipelineOldReq(
    @ApiModelProperty("仓库路径")
    val gitUrl: String?,
    @ApiModelProperty("分支")
    val branch: String?,
    @ApiModelProperty("是否显示告警")
    val defectDisplay: Boolean,
    @ApiModelProperty("触发来源")
    val triggerSource: String
)