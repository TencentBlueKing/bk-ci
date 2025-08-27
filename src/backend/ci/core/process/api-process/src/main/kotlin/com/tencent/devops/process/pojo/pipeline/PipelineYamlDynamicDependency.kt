package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "yaml文件动态依赖")
data class PipelineYamlDynamicDependency(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "代码库ID")
    val repoHashId: String,
    @get:Schema(title = "文件路径")
    val filePath: String,
    @get:Schema(title = "yaml文件类型")
    val fileType: YamlFileType,
    @get:Schema(title = "文件blob_id")
    val blobId: String,
    @get:Schema(title = "文件commitId")
    val commitId: String,
    @get:Schema(title = "文件提交时间")
    val commitTime: LocalDateTime,
    @get:Schema(title = "文件来源分支/tag")
    val ref: String,
    @get:Schema(title = "依赖的文件路径")
    val dependentFilePath: String,
    @get:Schema(title = "依赖的文件类型")
    val dependentFileType: YamlFileType,
    @get:Schema(title = "依赖的分支,默认为*,表示跟随分支")
    val dependentRef: String,
    @get:Schema(title = "依赖的文件blobId")
    val dependentBlobId: String,
    @get:Schema(title = "依赖的文件commitId")
    val dependentCommitId: String,
    @get:Schema(title = "依赖的文件提交时间")
    val dependentCommitTime: LocalDateTime
)
