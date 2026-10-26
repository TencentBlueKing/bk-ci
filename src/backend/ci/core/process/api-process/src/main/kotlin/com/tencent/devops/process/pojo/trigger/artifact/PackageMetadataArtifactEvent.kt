package com.tencent.devops.process.pojo.trigger.artifact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 包版本元数据事件（容器镜像 / 包制品）
 *
 * 镜像上传时制品与元数据分开写入，版本创建事件到达时元数据可能尚未落库，
 * 因此监听 PACKAGE_METADATA_SAVED，此时 `packageVersion.packageMetadata` 已包含元数据。
 *
 * bkrepo webhook payload 结构：顶层含 `user` / `projectId` / `repoName` / `packageKey` /
 * `packageVersion` / `eventType`，其中 `packageVersion` 为版本详情对象（[ArtifactPackageVersion]）。
 */
@Schema(title = "包版本元数据事件（容器镜像 / 包制品）")
@JsonIgnoreProperties(ignoreUnknown = true)
data class PackageMetadataArtifactEvent(
    override val eventType: String,
    override val user: ArtifactEventUser,
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "仓库名")
    val repoName: String,
    @get:Schema(title = "包Key")
    val packageKey: String,
    @get:Schema(title = "包版本详情")
    val packageVersion: ArtifactPackageVersion
) : ArtifactEvent() {
    override fun getAssociationId(): String = "$projectId:$repoName"
}
