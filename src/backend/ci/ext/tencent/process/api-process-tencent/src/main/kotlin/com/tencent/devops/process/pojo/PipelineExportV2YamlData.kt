package com.tencent.devops.process.pojo

import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement

/**
 * 目前仅为导出Yaml函数使用
 * 封装原本的model对象，在Yaml中为其添加相应位置的注释
 */
data class PipelineExportV2YamlData(
    val yamlStr: String,
    val conflictMap: Map<String, List<List<PipelineExportV2YamlConflictMapItem>>>
)

data class PipelineExportV2YamlConflictMapItem(
    val stage: PipelineExportV2YamlConflictMapBaseItem? = null,
    var job: JobPipelineExportV2YamlConflictMapBaseItem? = null,
    var step: PipelineExportV2YamlConflictMapBaseItem? = null
)

data class PipelineExportV2YamlConflictMapBaseItem(
    val id: String?,
    val name: String?
)

data class JobPipelineExportV2YamlConflictMapBaseItem(
    val id: String?,
    val name: String?,
    val jobId: String?
)

data class MarketBuildAtomElementWithLocation(
    val stageLocation: PipelineExportV2YamlConflictMapBaseItem? = null,
    val jobLocation: JobPipelineExportV2YamlConflictMapBaseItem? = null,
    val stepAtom: MarketBuildAtomElement? = null
)
