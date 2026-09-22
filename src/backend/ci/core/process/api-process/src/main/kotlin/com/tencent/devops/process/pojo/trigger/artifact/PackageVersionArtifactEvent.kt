package com.tencent.devops.process.pojo.trigger.artifact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 包版本事件（容器镜像 / 包制品）
 *
 * bkrepo webhook payload 结构：顶层仅含 `user` / `packageVersion` / `eventType`，
 * 其中 `packageVersion` 为版本详情对象（[ArtifactPackageVersion]）。
 */
@Schema(title = "包版本事件（容器镜像 / 包制品）")
@JsonIgnoreProperties(ignoreUnknown = true)
data class PackageVersionArtifactEvent(
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
