package com.tencent.devops.process.service.template.v2.version.convert

import com.tencent.devops.process.pojo.template.v2.PipelineTemplateVersionReq
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext

/**
 * 流水线模版版本请求转换器
 */
interface PipelineTemplateVersionReqConverter {

    fun support(request: PipelineTemplateVersionReq): Boolean

    fun convert(
        userId: String,
        projectId: String,
        templateId: String?,
        version: Long?,
        request: PipelineTemplateVersionReq
    ): PipelineTemplateVersionCreateContext
}
