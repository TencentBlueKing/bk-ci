package com.tencent.devops.dispatch.pojo.thirdPartyAgent

import com.tencent.devops.common.api.pojo.Error
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "第三方构建Docker登录调试完成信息")
data class ThirdPartyDockerDebugDoneInfo(
    @Schema(name = "项目id")
    val projectId: String,
    @Schema(name = "debugId")
    val debugId: Long,
    @Schema(name = "流水线id")
    val pipelineId: String,
    @Schema(name = "debug链接")
    val debugUrl: String,
    @Schema(name = "是否成功")
    val success: Boolean,
    @Schema(name = "错误信息")
    val error: Error?
)
