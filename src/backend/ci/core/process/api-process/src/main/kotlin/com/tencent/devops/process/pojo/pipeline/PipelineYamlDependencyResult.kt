package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "yaml文件动态依赖")
data class PipelineYamlDependencyResult(
    @get:Schema(title = "项目ID")
   val projectId: String,
    @get:Schema(title = "流水线ID")
   val pipelineId: String,
    @get:Schema(title = "流水线版本")
   val pipelineVersion: Int,
    @get:Schema(title = "流水线版本状态")
    val pipelineVersionStatus: VersionStatus? = VersionStatus.RELEASED,
    @get:Schema(title = "分支版本状态", required = false)
    val branchAction: BranchVersionAction? = null,
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
