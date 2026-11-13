package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.repository.pojo.Repository
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线PAC关闭请求")
data class PipelineYamlPacDisableReq(
    @get:Schema(title = "关闭pac的代码库", required = true)
    val repository: Repository,
    @get:Schema(title = "默认分支,为空时回退到yaml流水线记录中的默认分支", required = false)
    val defaultBranch: String? = null
)
