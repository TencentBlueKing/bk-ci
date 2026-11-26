package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "PAC 分支版本有效性请求")
data class PipelineYamlBranchActionReq(
    @get:Schema(title = "分支名", required = true)
    val branchName: String,
    @get:Schema(title = "分支版本动作", required = true)
    val branchVersionAction: BranchVersionAction
)
