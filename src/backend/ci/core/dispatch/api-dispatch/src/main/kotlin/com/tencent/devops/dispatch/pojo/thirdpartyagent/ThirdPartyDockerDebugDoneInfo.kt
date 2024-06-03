package com.tencent.devops.dispatch.pojo.thirdpartyagent

import com.tencent.devops.common.api.pojo.Error
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "第三方构建Docker登录调试完成信息")
data class ThirdPartyDockerDebugDoneInfo(
    @get:Schema(title = "项目id")
    val projectId: String,
    @get:Schema(title = "debugId")
    val debugId: Long,
    @get:Schema(title = "流水线id")
    val pipelineId: String,
    @get:Schema(title = "debug链接")
    val debugUrl: String,
    @get:Schema(title = "是否成功")
    val success: Boolean,
    @get:Schema(title = "错误信息")
    val error: Error?
)
