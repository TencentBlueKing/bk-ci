package com.tencent.devops.process.pojo

import io.swagger.annotations.ApiModel

@ApiModel("构建中跨项目引用模板信息")
data class BuildTemplateAcrossInfo(
    val templateId: String,
    val templateType: TemplateAcrossInfoType,
    val templateInstancesIds: MutableList<String>,
    val targetProjectId: String
)

enum class TemplateAcrossInfoType {
    JOB,
    STEP
}
