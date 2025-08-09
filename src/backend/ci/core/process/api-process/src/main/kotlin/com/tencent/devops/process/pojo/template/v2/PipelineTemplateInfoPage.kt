package com.tencent.devops.process.pojo.template.v2

data class PipelineTemplateInfoPage(
    val count: Int,
    val countOfCustom: Int,
    val countOfMarket: Int,
    val records: List<PipelineTemplateInfoV2>
)
