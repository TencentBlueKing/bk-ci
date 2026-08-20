package com.tencent.devops.artifactory.pojo.artifact

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 产出物元数据上报请求
 * 供插件 SDK 调用
 */
@Schema(title = "产出物元数据上报请求")
data class ArtifactMetadataRequest(
    @get:Schema(title = "流水线名称")
    val pipelineName: String? = null,
    @get:Schema(title = "构建号", required = true)
    val buildNum: Int,
    @get:Schema(title = "阶段ID", required = true)
    val stageId: String,
    @get:Schema(title = "构建容器ID", required = true)
    val containerId: String,
    @get:Schema(title = "任务ID", required = true)
    val taskId: String,
    @get:Schema(title = "执行次数", required = true)
    val executeCount: Int,
    @get:Schema(title = "产出物类型：FILE/IMAGE/REPORT/PACKAGE等", required = true)
    val artifactType: String,
    @get:Schema(title = "产出物名称，如文件名、镜像名", required = true)
    val artifactName: String,
    @get:Schema(title = "产出物版本，如镜像Tag、包版本")
    val artifactVersion: String? = null,
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
    @get:Schema(title = "代码提交ID", required = true)
    val commitId: String,
    @get:Schema(title = "扩展元数据，JSON格式")
    val extraInfo: String? = null
)
