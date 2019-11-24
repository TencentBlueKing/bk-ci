package com.tencent.devops.common.pipeline.type

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.pipeline.type.docker.ImageType

/**
 * @Description
 * @Date 2019/11/12
 * @Version 1.0
 */
abstract class StoreDispatchType(
    @JsonProperty("value") open var dockerBuildVersion: String?,
    routeKeySuffix: DispatchRouteKeySuffix? = null,
    open var imageType: ImageType? = ImageType.BKDEVOPS,
    open var credentialId: String? = "",
    open var credentialProject: String? = "",
    // 商店镜像代码
    open var imageCode: String? = "",
    // 商店镜像版本
    open var imageVersion: String? = "",
    // 商店镜像名称
    open var imageName: String? = ""
) : DispatchType((if (dockerBuildVersion.isNullOrBlank()) imageCode else dockerBuildVersion)
    ?: "StoreDispatchType empty image", routeKeySuffix)