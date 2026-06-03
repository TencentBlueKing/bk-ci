package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "代码库复制资源属性")
data class RepositoryCopyResourceProp(
    val scmCode: String
) : PipelineCopyResourceProp {
    companion object {
        const val classType = "repository"
    }
}
