package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板与流水线关联实体")
data class PipelineTemplateRelatedUpdateInfo(
    @get:Schema(title = "推荐版本号", required = false)
    val buildNo: BuildNo? = null,
    @get:Schema(title = "构建参数", required = false)
    val params: List<BuildFormProperty>? = null,
    @get:Schema(title = "是否软删除", required = false)
    val deleted: Boolean? = null,
    @get:Schema(title = "实例化错误信息", required = false)
    val instanceErrorInfo: String? = null,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null,
    @get:Schema(title = "状态", required = false)
    val status: TemplatePipelineStatus? = null,
    @get:Schema(title = "合并请求URL", required = false)
    val pullRequestUrl: String? = null,
    @get:Schema(title = "合并请求ID", required = false)
    val pullRequestId: Long? = null
)
