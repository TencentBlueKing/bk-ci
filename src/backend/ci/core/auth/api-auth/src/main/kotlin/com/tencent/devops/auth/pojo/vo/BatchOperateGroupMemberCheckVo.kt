package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量续期/删除/交接/组成员检查")
data class BatchOperateGroupMemberCheckVo(
    @get:Schema(title = "总数")
    val totalCount: Int,
    @get:Schema(title = "可操作的数量")
    val operableCount: Int? = 0,
    @get:Schema(title = "无法操作的数量")
    val inoperableCount: Int? = 0,
    @get:Schema(title = "唯一管理员组的数量")
    val uniqueManagerCount: Int? = 0,
    @get:Schema(title = "导致代持人失效的用户组")
    val invalidGroupCount: Int? = 0,
    @get:Schema(title = "无效的流水线授权数量")
    val invalidPipelineAuthorizationCount: Int? = 0,
    @get:Schema(title = "无效的代码库授权数量")
    val invalidRepositoryAuthorizationCount: Int? = 0,
    @get:Schema(title = "无效的环境节点授权数量")
    val invalidEnvNodeAuthorizationCount: Int? = 0,
    @get:Schema(title = "可交接的组数量")
    val canHandoverCount: Int? = 0,
    @get:Schema(title = "是否需要交接")
    val needToHandover: Boolean? = null
)
