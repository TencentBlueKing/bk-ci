package com.tencent.devops.process.trigger.artifact.pojo

/**
 * 制品事件抽取出的匹配事实
 *
 */
data class ArtifactFactParam(
    val projectId: String,
    val repoName: String,
    // 自定义仓库完整路径：文件为 fullPath，目录为归档目录 path
    val path: String? = null,
    val artifactsName: String? = null,
    // 镜像名（去掉 docker:// 前缀），仅镜像仓库有值
    val image: String? = null,
    val version: String? = null,
    val sourcePipelineId: String? = null,
    val sourceBuildId: String? = null,
    val metadata: Map<String, Any?> = emptyMap()
)
