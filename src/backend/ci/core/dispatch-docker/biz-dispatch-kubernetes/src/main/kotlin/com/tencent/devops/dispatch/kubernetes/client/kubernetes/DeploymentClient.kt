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

package com.tencent.devops.dispatch.kubernetes.client.kubernetes

import com.tencent.devops.dispatch.kubernetes.config.KubernetesClientConfig
import com.tencent.devops.dispatch.kubernetes.pojo.BuildContainer
import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreApi
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Container
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1DeploymentSpec
import io.kubernetes.client.openapi.models.V1LabelSelector
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1PodSpec
import io.kubernetes.client.openapi.models.V1PodTemplateSpec
import io.kubernetes.client.openapi.models.V1ResourceRequirements
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DeploymentClient @Autowired constructor(
    private val k8sConfig: KubernetesClientConfig
) {

    fun create(
        buildContainer: BuildContainer,
        containerName: String
    ) {
        val deployment =
            with(buildContainer) {
                V1Deployment()
                    .apiVersion("apps/v1")
                    .kind("Deployment")
                    .metadata(
                        V1ObjectMeta()
                            .name(name)
                            .namespace(k8sConfig.nameSpace)
                            .labels(mapOf("container-name" to containerName))
                    )
                    .spec(
                        V1DeploymentSpec()
                            .replicas(1)
                            .selector(
                                V1LabelSelector()
                                    .matchLabels(mapOf("container-name" to containerName))
                            )
                            .template(
                                V1PodTemplateSpec()
                                    .metadata(
                                        V1ObjectMeta()
                                            .labels(mutableMapOf("container-name" to containerName)
                                            )
                                    )
                                    .spec(
                                        V1PodSpec()
                                            .containers(listOf(
                                                V1Container()
                                                    .name(image.split(":")[0])
                                                    .resources(
                                                        V1ResourceRequirements()
                                                            .limits(mapOf(
                                                                "cpu" to Quantity(cpu.toString()),
                                                                "memory" to Quantity(memory),
                                                                "ephemeral-storage" to Quantity(disk)
                                                            ))
                                                    )
                                                    .image(image)
                                            ))
                                    )
                            )
            }
        AppsV1Api().createNamespacedDeploymentWithHttpInfo(
            k8sConfig.nameSpace, deployment, null, null, null
        )
    }
}
