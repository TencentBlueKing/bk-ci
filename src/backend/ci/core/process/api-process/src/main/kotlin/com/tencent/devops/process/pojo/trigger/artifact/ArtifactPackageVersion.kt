package com.tencent.devops.process.pojo.trigger.artifact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品包版本信息（bkrepo webhook payload 中的 packageVersion 对象）
 *
 * 对应 bkrepo PackageVersion，容器镜像 / 包制品版本创建（VERSION_CREATED）时上报。
 */
@Schema(title = "制品包版本信息")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ArtifactPackageVersion(
    @get:Schema(title = "创建人")
    val createdBy: String,
    @get:Schema(title = "创建时间")
    val createdDate: String,
    @get:Schema(title = "最近修改人")
    val lastModifiedBy: String,
    @get:Schema(title = "最近修改时间")
    val lastModifiedDate: String,
    @get:Schema(title = "版本名（tag）")
    val name: String,
    @get:Schema(title = "版本大小")
    val size: Long,
    @get:Schema(title = "下载次数")
    val downloads: Long,
    @get:Schema(title = "晋级状态标签")
    val stageTag: List<String>,
    @get:Schema(title = "元数据列表")
    val packageMetadata: List<ArtifactNodeMetadata>,
    @get:Schema(title = "版本标签")
    val tags: List<String>,
    @get:Schema(title = "扩展字段")
    val extension: Map<String, Any?>,
    @get:Schema(title = "内容存储路径")
    val contentPath: String
)
