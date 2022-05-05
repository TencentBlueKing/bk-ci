package com.tencent.devops.common.pipeline.type.bcs

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType

data class PublicBcsDispatchType(
    @JsonProperty("value") var image: String?,
    var performanceConfigId: String?,
    override var imageType: ImageType? = ImageType.BKDEVOPS,
    override var credentialId: String? = "",
    override var credentialProject: String? = "",
    // 商店镜像代码
    override var imageCode: String? = "",
    // 商店镜像版本
    override var imageVersion: String? = "",
    // 商店镜像名称
    override var imageName: String? = ""
) : StoreDispatchType(if (image.isNullOrBlank())
    imageCode else image, DispatchRouteKeySuffix.BCS, imageType, credentialId, credentialProject, imageCode,
    imageVersion, imageName) {
    override fun cleanDataBeforeSave() {
        this.image = this.image?.trim()
        this.credentialId = this.credentialId?.trim()
        this.credentialProject = this.credentialProject?.trim()
        this.imageCode = this.imageCode?.trim()
        this.imageVersion = this.imageVersion?.trim()
        this.imageName = this.imageName?.trim()
    }

    override fun replaceField(variables: Map<String, String>) {
        image = EnvUtils.parseEnv(image!!, variables)
        credentialId = EnvUtils.parseEnv(credentialId, variables)
    }

    override fun buildType() = BuildType.valueOf(BuildType.PUBLIC_BCS.name)
}
