package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量续期/删除/交接/组成员检查")
data class BatchOperateGroupMemberCheckVo(
    @get:Schema(title = "总数")
    val totalCount: Int,
    @get:Schema(title = "可操作的数量")
    val operableCount: Int? = null,
    @get:Schema(title = "无法操作的数量")
    val inoperableCount: Int? = null,
    @get:Schema(title = "唯一管理员组的数量")
    val uniqueManagerCount: Int? = null,
    @get:Schema(title = "无效的流水线授权数量")
    val invalidPipelineAuthorizationCount: Int? = null
)
