package com.tencent.devops.process.pojo.trigger.artifact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品节点信息（bkrepo webhook payload 中的 node 对象）
 */
@Schema(title = "制品节点信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ArtifactNode(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "仓库名")
    val repoName: String,
    @get:Schema(title = "目录路径")
    val path: String,
    @get:Schema(title = "文件名")
    val name: String,
    @get:Schema(title = "完整路径")
    val fullPath: String,
    @get:Schema(title = "是否目录")
    val folder: Boolean,
    @get:Schema(title = "文件大小")
    val size: Long,
    @get:Schema(title = "sha256")
    val sha256: String,
    @get:Schema(title = "md5")
    val md5: String,
    @get:Schema(title = "节点元数据列表")
    val nodeMetadata: List<ArtifactNodeMetadata>,
    @get:Schema(title = "创建人")
    val createdBy: String,
    @get:Schema(title = "创建时间")
    val createdDate: String,
    @get:Schema(title = "最近修改人")
    val lastModifiedBy: String,
    @get:Schema(title = "最近修改时间")
    val lastModifiedDate: String
)
