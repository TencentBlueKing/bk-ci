package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "yaml文件依赖")
data class PipelineYamlDependency(
    val projectId: String,
    @get:Schema(title = "代码库ID")
    val repoHashId: String,
    @get:Schema(title = "yaml文件路径")
    val filePath: String,
    @get:Schema(title = "yaml文件路径MD5,因为filePath太长，作为主键索引会超,所以用md5")
    val filePathMd5: String,
    @get:Schema(title = "ci文件blob_id")
    val blobId: String,
    @get:Schema(title = "依赖的文件路径")
    val dependFilePath: String,
    @get:Schema(title = "依赖的文件路径MD5")
    val dependFilePathMd5: String,
    @get:Schema(title = "依赖的文件路径类型")
    val dependFileType: YamlFileType,
    @get:Schema(title = "依赖的文件分支")
    val dependRef: String
)
