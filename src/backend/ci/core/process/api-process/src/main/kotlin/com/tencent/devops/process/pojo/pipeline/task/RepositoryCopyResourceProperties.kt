package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "代码库复制资源属性")
data class RepositoryCopyResourceProperties(
    @get:Schema(description = "授权方式")
    val authType: String?,
    @get:Schema(description = "授权信息")
    val authInfo: String?,
    @get:Schema(description = "代码库协议")
    val repositoryType: String?,
    @get:Schema(description = "代码库URL")
    val repositoryUrl: String?
) : PipelineCopyResourceProperties {
    companion object {
        const val classType = "repository"
    }
}
