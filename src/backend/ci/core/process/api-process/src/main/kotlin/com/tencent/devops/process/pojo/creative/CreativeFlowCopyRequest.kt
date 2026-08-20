package com.tencent.devops.process.pojo.creative

import com.tencent.devops.process.enums.CreativeFlowCopyConflictPolicy
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流跨空间复制请求")
data class CreativeFlowCopyRequest(
    @get:Schema(title = "分享ID", required = true)
    val shareId: String,
    @get:Schema(title = "创作流条目ID", required = true)
    val flowId: String,
    @get:Schema(title = "目标创作流名称，缺省用授权中的源名称")
    val targetPipelineName: String? = null,
    @get:Schema(title = "目标环境HashId，创作流必填", required = true)
    val targetEnvHashId: String,
    @get:Schema(title = "变量覆盖，key为变量名")
    val variableOverrides: Map<String, String>? = null,
    @get:Schema(title = "冲突策略：SKIP / OVERWRITE / FAIL，默认 SKIP")
    val conflictPolicy: CreativeFlowCopyConflictPolicy = CreativeFlowCopyConflictPolicy.SKIP,
    @get:Schema(title = "OVERWRITE时必填，且必须是本授权已登记的副本")
    val targetPipelineId: String? = null,
    @get:Schema(title = "是否迁移依赖资源，本迭代仅接受false")
    val copyDependencies: Boolean = false
)
