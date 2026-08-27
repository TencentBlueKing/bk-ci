package com.tencent.devops.process.trigger.artifact.pojo

import com.tencent.devops.process.pojo.trigger.artifact.ArtifactEvent

/**
 * 制品事件抽取出的匹配事实
 *
 */
data class ArtifactFactParam(
    val projectId: String,
    val repoName: String,
    // 仓库根路径,自定义仓库才有值
    val rootPath: String? = null,
    // 匹配路径（去掉根目录），仅自定义仓库有值；文件含文件名，目录只到归档目录、不含其中文件
    val paths: List<String>? = null,
    val artifactsName: String? = null,
    // 镜像名（去掉 docker:// 前缀），仅镜像仓库有值
    val image: String? = null,
    val version: String? = null,
    val sourcePipelineId: String? = null,
    val sourceBuildId: String? = null,
    val metadata: Map<String, Any?> = emptyMap()
)
