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

package com.tencent.devops.dispatch.kubernetes.config

import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.Toleration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
class DispatchBuildConfig {

    @Value("\${dispatch.build.deployment.cpu:#{null}}")
    var deploymentCpu: Int = 32

    @Value("\${dispatch.build.deployment.memory:#{null}}")
    var deploymentMemory: String = "65535M"

    @Value("\${dispatch.build.deployment.disk:#{null}}")
    var deploymentDisk: String = "500G"

    @Value("\${dispatch.build.container.registry.host:#{null}}")
    val registryHost: String? = null

    @Value("\${dispatch.build.container.registry.userName:#{null}}")
    val registryUser: String? = null

    @Value("\${dispatch.build.container.registry.password:#{null}}")
    val registryPwd: String? = null

    @Value("\${dispatch.build.label:#{null}}")
    val workloadLabel: String? = null

    @Value("\${dispatch.build.volumeMount.path:#{null}}")
    val volumeMountPath: String? = null

    @Value("\${dispatch.build.volume.configMap.key:#{null}}")
    val volumeConfigMapKey: String? = null

    @Value("\${dispatch.build.volume.configMap.path:#{null}}")
    val volumeConfigMapPath: String? = null

    @Value("\${dispatch.build.volume.hostPath.hostdir:#{null}}")
    val volumeHostPathHostDir: String? = null
}

@Component
@ConfigurationProperties(prefix = "dispatch.build")
data class DispatchBuildConfiguration(
    var tolerations: List<Toleration>? = null
)
