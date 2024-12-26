package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "移除/移交用户组成员导致的无效授权")
data class InvalidAuthorizationsDTO(
    @get:Schema(title = "引起代持人权限失效的用户组")
    val invalidGroupIds: List<Int>,
    @get:Schema(title = "引起代持人权限失效的流水线")
    val invalidPipelineIds: List<String>,
    @get:Schema(title = "引起oauth失效的代码库")
    val invalidRepertoryIds: List<String> = emptyList(),
    @get:Schema(title = "失效的CMDB环境节点")
    val invalidEnvNodeIds: List<String> = emptyList()
) {
    fun isHasInvalidAuthorizations(): Boolean {
        return invalidRepertoryIds.isNotEmpty() || invalidPipelineIds.isNotEmpty() || invalidEnvNodeIds.isNotEmpty()
    }
}
