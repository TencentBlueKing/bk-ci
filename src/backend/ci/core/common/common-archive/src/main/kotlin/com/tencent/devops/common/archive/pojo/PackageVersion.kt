package com.tencent.devops.common.archive.pojo

import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "包信息")
data class PackageVersion(
    @get:Schema(title = "创建者", required = true)
    val createdBy: String,
    @get:Schema(title = "创建时间", required = true)
    val createdDate: LocalDateTime,
    @get:Schema(title = "修改者", required = true)
    val lastModifiedBy: String,
    @get:Schema(title = "修改时间", required = true)
    val lastModifiedDate: LocalDateTime,
    @get:Schema(title = "包版本", required = true)
    val name: String,
    @get:Schema(title = "包大小", required = true)
    val size: Long,
    @get:Schema(title = "下载次数", required = true)
    var downloads: Long,
    @get:Schema(title = "制品晋级阶段", required = true)
    val stageTag: List<String>,
    @get:Schema(title = "元数据", required = true)
    val metadata: Map<String, Any>,
    @get:Schema(title = "元数据", required = true)
    val packageMetadata: List<MetadataModel>,
    @get:Schema(title = "标签", required = true)
    val tags: List<String>,
    @get:Schema(title = "扩展字段", required = true)
    val extension: Map<String, Any>,
    @get:Schema(title = "包内容文件路径", required = false)
    val contentPath: String? = null,
    @get:Schema(title = "清单文件路径", required = false)
    val manifestPath: String? = null
)