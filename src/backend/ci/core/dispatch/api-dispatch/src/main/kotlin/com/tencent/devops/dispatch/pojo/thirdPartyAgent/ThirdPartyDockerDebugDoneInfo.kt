package com.tencent.devops.dispatch.pojo.thirdPartyAgent

import com.tencent.devops.common.api.pojo.Error
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "第三方构建Docker登录调试完成信息")
data class ThirdPartyDockerDebugDoneInfo(
    @Schema(description = "项目id")
    val projectId: String,
    @Schema(description = "debugId")
    val debugId: Long,
    @Schema(description = "流水线id")
    val pipelineId: String,
    @Schema(description = "debug链接")
    val debugUrl: String,
    @Schema(description = "是否成功")
    val success: Boolean,
    @Schema(description = "错误信息")
    val error: Error?
)
