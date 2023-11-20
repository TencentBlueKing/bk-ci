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

package com.tencent.devops.process.yaml.modelTransfer

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.image.BuildType
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.MacOS
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.modelTransfer.inner.TransferCreator
import com.tencent.devops.process.yaml.v3.models.image.PoolImage
import com.tencent.devops.process.yaml.v3.models.image.PoolType
import com.tencent.devops.process.yaml.v3.models.job.Container
import com.tencent.devops.process.yaml.v3.models.job.Container2
import com.tencent.devops.process.yaml.v3.models.job.Job
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import com.tencent.devops.common.ci.image.PoolType as TXPoolType

@Primary
@Component
class TXDispatchTransfer @Autowired(required = false) constructor(
    client: Client,
    objectMapper: ObjectMapper,
    inner: TransferCreator
) : DispatchTransfer(client, objectMapper, inner) {

    override fun dispatcherLinux(
        job: Job,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): DispatchType? {
        // 公共docker构建机
        if (job.runsOn.poolName == "docker") {
            return TXPoolType.DockerOnDevCloud.toDispatchType(
                makeContainerPool(
                    BuildType.DEVCLOUD,
                    job,
                    buildTemplateAcrossInfo
                )
            )
        }
        return null
    }

    override fun dispatcherMacos(
        job: Job
    ): DispatchType? {
        if (job.runsOn.poolName?.startsWith("macos") == true) {
            return TXPoolType.Macos.toDispatchType(
                Pool(
                    macOS = MacOS(
                        systemVersion = job.runsOn.poolName?.removePrefix("macos-"),
                        xcodeVersion = job.runsOn.xcode
                    )
                )
            )
        }
        return null
    }

    override fun dispatcherWindows(
        job: Job
    ): DispatchType? {
        // windows公共构建机
        if (job.runsOn.poolName?.startsWith("windows") == true) {
            return TXPoolType.WindowsOnDevcloud.toDispatchType(
                Pool(
                    container = job.runsOn.poolName
                )
            )
        }
        return null
    }

    override fun dispatch2RunsOn(dispatcher: DispatchType) =
        PoolType.SelfHosted.toRunsOn(dispatcher)
            ?: TXPoolType.DockerOnDevCloud.toRunsOn(dispatcher)
            ?: TXPoolType.Macos.toRunsOn(dispatcher)
            ?: TXPoolType.WindowsOnDevcloud.toRunsOn(dispatcher)

    private fun makeContainerPool(
        buildType: BuildType,
        job: Job,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): Pool {
        var containerPool = Pool(
            credential = Credential(
                user = "",
                password = ""
            ),
            macOS = null,
            third = null,
            env = job.env,
            buildType = buildType,
            image = PoolImage(
                imageCode = inner.defaultImageCode,
                imageVersion = inner.defaultImageVersion,
                imageType = ImageType.BKSTORE
            ),
            performanceConfigId = job.runsOn.hwSpec
        )

        if (job.runsOn.container != null) {
            try {
                val container = YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(job.runsOn.container!!),
                    Container::class.java
                )
                val imageType = container.takeImageType()
                val imageCode = container.takeImageCode() ?: ""
                val imageVersion = container.takeImageVersion() ?: ""
                containerPool = Pool(
                    container = when (imageType) {
                        ImageType.THIRD -> container.image
                        else -> imageCode
                    },
                    credential = Credential(
                        user = container.credentials?.username,
                        password = container.credentials?.password
                    ),
                    macOS = null,
                    third = null,
                    env = job.env,
                    buildType = buildType,
                    image = PoolImage(
                        imageCode = imageCode,
                        imageVersion = imageVersion,
                        imageType = imageType
                    ),
                    performanceConfigId = job.runsOn.hwSpec
                )
            } catch (e: Exception) {
                val container = YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(job.runsOn.container!!),
                    Container2::class.java
                )
                val imageType = container.takeImageType()
                val imageCode = container.takeImageCode() ?: ""
                val imageVersion = container.takeImageVersion() ?: ""
                containerPool = Pool(
                    container = when (imageType) {
                        ImageType.THIRD -> container.image
                        else -> imageCode
                    },
                    credential = Credential(
                        user = "",
                        password = "",
                        credentialId = container.credentials,
                        fromRemote = if (buildTemplateAcrossInfo != null) Credential.Remote(
                            targetProjectId = buildTemplateAcrossInfo.targetProjectId,
                            templateId = buildTemplateAcrossInfo.templateId,
                            jobId = job.id ?: ""
                        ) else null
                    ),
                    macOS = null,
                    third = null,
                    env = job.env,
                    buildType = buildType,
                    image = PoolImage(
                        imageCode = imageCode,
                        imageVersion = imageVersion,
                        imageType = imageType
                    ),
                    performanceConfigId = job.runsOn.hwSpec
                )
            }
        }
        return containerPool
    }
}
