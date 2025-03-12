package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.scm.api.pojo.Commit
import com.tencent.devops.scm.api.pojo.Tree
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线yaml文件同步请求")
data class PipelineYamlFileSyncReq(
    @get:Schema(title = "开启pac的代码库", required = true)
    val repository: Repository,
    @get:Schema(title = "文件列表", required = true)
    val fileTrees: List<Tree>,
    @get:Schema(title = "默认分支", required = true)
    val defaultBranch: String,
    @get:Schema(title = "提交信息", required = true)
    val commit: Commit
)
