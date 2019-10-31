package com.tencent.devops.gitci.pojo

import com.tencent.devops.process.pojo.pipeline.ModelDetail
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂构建详情模型")
data class GitCIModelDetail(
    @ApiModelProperty("工蜂Event事件", required = true)
    val gitRequestEvent: GitRequestEvent,
    @ApiModelProperty("构建详情-构建信息", required = true)
    val modelDetail: ModelDetail
)