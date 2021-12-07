/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.common.pipeline.type.kubernetes

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType

data class KubernetesDispatchType(
    @JsonProperty("value")
    var kubernetesBuildVersion: String?,
    override var imageType: ImageType? = ImageType.BKDEVOPS,
    override var credentialId: String? = "",
    override var credentialProject: String? = "",
    // 商店镜像代码
    override var imageCode: String? = "",
    // 商店镜像版本
    override var imageVersion: String? = "",
    // 商店镜像名称
    override var imageName: String? = "",
    // docker资源配置ID
    var performanceConfigId: Int = 0
) : StoreDispatchType(
    dockerBuildVersion = if (kubernetesBuildVersion.isNullOrBlank()) {
        imageCode
    } else {
        kubernetesBuildVersion
    },
    routeKeySuffix = DispatchRouteKeySuffix.KUBERNETES,
    imageType = imageType,
    credentialId = credentialId,
    credentialProject = credentialProject,
    imageCode = imageCode,
    imageVersion = imageVersion,
    imageName = imageName
) {

    override fun cleanDataBeforeSave() {
        this.kubernetesBuildVersion = this.kubernetesBuildVersion?.trim()
        this.credentialId = this.credentialId?.trim()
        this.credentialProject = this.credentialProject?.trim()
        this.imageCode = this.imageCode?.trim()
        this.imageVersion = this.imageVersion?.trim()
        this.imageName = this.imageName?.trim()
    }

    override fun buildType(): BuildType {
        return BuildType.valueOf(BuildType.KUBERNETES.name)
    }

    override fun replaceField(variables: Map<String, String>) {
        kubernetesBuildVersion = EnvUtils.parseEnv(kubernetesBuildVersion!!, variables)
        credentialId = EnvUtils.parseEnv(credentialId, variables)
    }
}
