package com.tencent.devops.remotedev.pojo.image

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
data class ListVmImagesResp(
    val result: Boolean,
    val code: Int,
    val message: String?,
    val data: List<StandardVmImage>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StandardVmImage(
    val updateAt: String?,
    val cosFile: String?,
    val sourceType: String?,
    val isStandard: Boolean,
    val size: String? = null
)

@Schema(title = "镜像查询参数")
data class ListImagesData(
    @get:Schema(title = "架构，x86_64 aarch64", required = false)
    val architecture: String?,
    @get:Schema(title = "envId", required = false)
    val envId: String?,
    @get:Schema(title = "搜索镜像名称", required = false)
    val imageName: String?,
    @get:Schema(title = "镜像类型，官方镜像(official),自定义镜像(custom)", required = false)
    val imageType: String?,
    @get:Schema(title = "机型", required = false)
    val machineType: String?,
    @get:Schema(title = "系统， linux windows", required = false)
    val platform: String?,
    @get:Schema(title = "项目，查询自定义镜像时需要", required = true)
    val projectId: String,
    @get:Schema(
        title = "镜像提供商(IEG_BKCI/TEST_IEG_BKCI/CSIG/TEST_CSIG/TEG_DEVCLOUD/TEST_TEG_DEVCLOUD)",
        required = true
    )
    val provider: String,
    @get:Schema(title = "地域，如果是官方镜像，不需要传", required = false)
    val zoneId: String?
)

@Schema(title = "镜像查询返回")
data class ListImagesResp(
    @get:Schema(title = "镜像总数")
    val count: Long,
    @get:Schema(title = "镜像列表")
    val items: List<ListImagesRespItem>?
)

data class ListImagesRespItem(
    val architecture: String?,
    val createdAt: String?,
    val creator: String?,
    val fileSize: String?,
    val image: String?,
    @JsonProperty("imageID")
    val imageId: String?,
    val imageName: String?,
    val imageType: String?,
    val machineType: String?,
    val platform: String?,
    val size: String?,
    val status: String?,
    @JsonProperty("zoneID")
    val zoneId: String?
)
