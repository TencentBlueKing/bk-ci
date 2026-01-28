package com.tencent.devops.common.archive.pojo

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "包总览信息")
data class PackageSummary(
    @get:Schema(title = "创建者", required = true)
    val createdBy: String,
    @get:Schema(title = "创建时间", required = true)
    val createdDate: LocalDateTime,
    @get:Schema(title = "修改者", required = true)
    val lastModifiedBy: String,
    @get:Schema(title = "修改时间", required = true)
    val lastModifiedDate: LocalDateTime,
    @get:Schema(title = "所属项目id", required = true)
    val projectId: String,
    @get:Schema(title = "所属仓库名称", required = true)
    val repoName: String,
    @get:Schema(title = "包名称", required = true)
    val name: String,
    @get:Schema(title = "包唯一key", required = true)
    val key: String,
    @get:Schema(title = "包类型", required = true)
    var type: String,
    @get:Schema(title = "最新版名称", required = true)
    val latest: String,
    @get:Schema(title = "下载次数", required = true)
    val downloads: Long,
    @get:Schema(title = "版本数量", required = true)
    var versions: Long,
    @get:Schema(title = "包简要描述", required = true)
    var description: String? = null,
    @get:Schema(title = "版本标签", required = true)
    val versionTag: Map<String, String>,
    @get:Schema(title = "扩展字段", required = true)
    val extension: Map<String, Any>,
    @get:Schema(title = "历史版本", required = true)
    val historyVersion: Set<String>
)