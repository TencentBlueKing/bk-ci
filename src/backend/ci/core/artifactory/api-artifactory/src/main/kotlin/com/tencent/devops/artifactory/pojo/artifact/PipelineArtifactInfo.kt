package com.tencent.devops.artifactory.pojo.artifact

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 流水线产出物元数据信息
 */
@Schema(title = "流水线产出物元数据信息")
data class PipelineArtifactInfo(
    @get:Schema(title = "主键ID", required = true)
    val id: Long,
    @get:Schema(title = "蓝盾项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线名称")
    val pipelineName: String? = null,
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "构建号")
    val buildNum: Int,
    @get:Schema(title = "阶段ID")
    val stageId: String = "",
    @get:Schema(title = "构建容器ID")
    val containerId: String = "",
    @get:Schema(title = "任务ID")
    val taskId: String = "",
    @get:Schema(title = "执行次数")
    val executeCount: Int,
    @get:Schema(title = "产出物类型：FILE/IMAGE/REPORT/PACKAGE等", required = true)
    val artifactType: String,
    @get:Schema(title = "产出物名称，如文件名、镜像名", required = true)
    val artifactName: String,
    @get:Schema(title = "产出物版本，如镜像Tag、包版本")
    val artifactVersion: String,
    @get:Schema(title = "产出物唯一资源标识，如文件路径、镜像完整地址")
    val artifactUri: String? = null,
    @get:Schema(title = "产出物仓库地址，如制品库地址、镜像Registry")
    val artifactRepoUrl: String? = null,
    @get:Schema(title = "产出物摘要，如sha256、镜像digest")
    val artifactDigest: String? = null,
    @get:Schema(title = "产出物大小，单位字节")
    val artifactSize: Long? = null,
    @get:Schema(title = "代码库地址")
    val codeRepoUrl: String? = null,
    @get:Schema(title = "代码提交ID")
    val commitId: String = "",
    @get:Schema(title = "扩展元数据，JSON格式")
    val extraInfo: String? = null,
    @get:Schema(title = "创建人")
    val creator: String = "system",
    @get:Schema(title = "修改人")
    val modifier: String = "system",
    @get:Schema(title = "创建时间", required = true)
    val createTime: LocalDateTime,
    @get:Schema(title = "更新时间", required = true)
    val updateTime: LocalDateTime
)
