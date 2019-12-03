package com.tencent.devops.process.pojo.template

data class TemplateInstancePage(
    val projectId: String,
    val templateId: String,
    val instances: List<TemplatePipeline>,
    val latestVersion: TemplateVersion,
    val count: Int,
    val page: Int?,
    val pageSize: Int?
)