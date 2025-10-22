/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.pipeline.type.docker

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import io.swagger.v3.oas.annotations.media.Schema

data class DockerDispatchType(
    @JsonProperty("value")
    @get:Schema(title = "docker构建版本", required = false)
    override var dockerBuildVersion: String?,
    @get:Schema(title = "镜像类型", required = false)
    override var imageType: ImageType? = ImageType.BKDEVOPS,
    @get:Schema(title = "凭证id", required = false)
    override var credentialId: String? = "",
    @get:Schema(title = "凭证项目id", required = false)
    override var credentialProject: String? = "",
    @get:Schema(title = "商店镜像代码", required = false)
    // 商店镜像代码
    override var imageCode: String? = "",
    @get:Schema(title = "商店镜像版本", required = false)
    // 商店镜像版本
    override var imageVersion: String? = "",
    @get:Schema(title = "商店镜像名称", required = false)
    // 商店镜像名称
    override var imageName: String? = "",
    @get:Schema(title = "docker资源配置ID", required = false)
    // docker资源配置ID
    var performanceConfigId: Int = 0,
    @get:Schema(title = "镜像仓库用户名", required = false)
    var imageRepositoryUserName: String = "",
    @get:Schema(title = "镜像仓库密码", required = false)
    var imageRepositoryPassword: String = ""
) : StoreDispatchType(dockerBuildVersion = if (dockerBuildVersion.isNullOrBlank()) {
    imageCode
} else {
    dockerBuildVersion
},
    routeKeySuffix = DispatchRouteKeySuffix.DOCKER_VM,
    imageType = imageType,
    credentialId = credentialId,
    credentialProject = credentialProject,
    imageCode = imageCode,
    imageVersion = imageVersion,
    imageName = imageName) {

    override fun cleanDataBeforeSave() {
        this.dockerBuildVersion = this.dockerBuildVersion?.trim()
        this.credentialId = this.credentialId?.trim()
        this.credentialProject = this.credentialProject?.trim()
        this.imageCode = this.imageCode?.trim()
        this.imageVersion = this.imageVersion?.trim()
        this.imageName = this.imageName?.trim()
    }

    override fun buildType(): BuildType {
        return BuildType.valueOf(BuildType.DOCKER.name)
    }

    override fun replaceField(variables: Map<String, String>) {
        dockerBuildVersion = EnvUtils.parseEnv(dockerBuildVersion!!, variables)
        credentialId = EnvUtils.parseEnv(credentialId, variables)
    }
}
