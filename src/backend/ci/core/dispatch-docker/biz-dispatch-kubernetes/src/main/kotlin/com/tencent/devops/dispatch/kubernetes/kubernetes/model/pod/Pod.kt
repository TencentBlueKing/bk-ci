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

package com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod

import com.tencent.devops.dispatch.kubernetes.pojo.Ports
import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.openapi.models.V1Container
import io.kubernetes.client.openapi.models.V1ContainerPort
import io.kubernetes.client.openapi.models.V1EnvVar
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1PodSpec
import io.kubernetes.client.openapi.models.V1PodTemplateSpec
import io.kubernetes.client.openapi.models.V1ResourceRequirements

object Pod {

    fun template(
        pod: PodData
    ): V1PodTemplateSpec {
        with(pod) {
            return V1PodTemplateSpec()
                .metadata(
                    V1ObjectMeta().labels(labels)
                )
                .spec(
                    spec(container)
                )
        }
    }

    private fun spec(
        container: ContainerData
    ): V1PodSpec {
        with(container) {
            return V1PodSpec()
                .containers(
                    listOf(
                        V1Container()
                            .name(imageName)
                            .resources(
                                V1ResourceRequirements()
                                    .limits(
                                        mapOf(
                                            "cpu" to Quantity(cpu),
                                            "memory" to Quantity(memory),
                                            "ephemeral-storage" to Quantity(disk)
                                        )
                                    )
                            )
                            .image(image)
                            .ports(
                                ports?.map { port ->
                                    V1ContainerPort()
                                        .protocol(port.protocol)
                                        .containerPort(port.port?.toInt())
                                        .hostPort(port.targetPort?.toInt())
                                }
                            )
                            .env(
                                env?.map {
                                    V1EnvVar()
                                        .name(it.key)
                                        .value(it.value)
                                }
                            )
                            .command(commends)
                    )
                )
        }
    }
}
