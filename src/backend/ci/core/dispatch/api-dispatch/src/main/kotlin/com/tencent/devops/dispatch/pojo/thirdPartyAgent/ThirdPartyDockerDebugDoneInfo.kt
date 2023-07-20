package com.tencent.devops.dispatch.pojo.thirdPartyAgent

import com.tencent.devops.common.api.pojo.Error
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("第三方构建Docker登录调试完成信息")
data class ThirdPartyDockerDebugDoneInfo(
    @ApiModelProperty("项目id")
    val projectId: String,
    @ApiModelProperty("debugId")
    val debugId: Long,
    @ApiModelProperty("流水线id")
    val pipelineId: String,
    @ApiModelProperty("debug链接")
    val debugUrl: String,
    @ApiModelProperty("是否成功")
    val success: Boolean,
    @ApiModelProperty("错误信息")
    val error: Error?
)
