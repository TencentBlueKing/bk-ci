package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import com.tencent.devops.process.pojo.pipeline.enums.YamlRefValueType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "yaml文件动态依赖")
data class PipelineYamlDependency(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "代码库ID")
    val repoHashId: String,
    @get:Schema(title = "文件路径")
    val filePath: String,
    @get:Schema(title = "yaml文件类型")
    val fileType: YamlFileType,
    @get:Schema(title = "文件来源分支/tag")
    val ref: String,
    @get:Schema(title = "ref值的类型")
    val refValueType: YamlRefValueType,
    @get:Schema(title = "依赖的文件路径")
    val dependentFilePath: String,
    @get:Schema(title = "依赖的文件类型")
    val dependentFileType: YamlFileType,
    @get:Schema(title = "依赖的分支,默认为*,表示跟随分支")
    val dependentRef: String
)
