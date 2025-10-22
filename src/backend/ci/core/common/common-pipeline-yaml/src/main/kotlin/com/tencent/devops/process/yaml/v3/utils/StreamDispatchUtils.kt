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

package com.tencent.devops.process.yaml.v3.utils

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.pojo.ThirdPartyContainerInfoV3
import com.tencent.devops.process.yaml.v3.models.job.Container
import com.tencent.devops.process.yaml.v3.models.job.Container2
import com.tencent.devops.process.yaml.v3.models.job.Job
import org.slf4j.LoggerFactory

@Suppress("ALL")
object StreamDispatchUtils {

    private val logger = LoggerFactory.getLogger(StreamDispatchUtils::class.java)

    /**
     * 解析 jobs.runsOn.container
     * 注：因为要蓝盾也要支持所以环境变量替换会在蓝盾层面去做
     * @return image,username,password
     */
    fun parseRunsOnContainer(
        job: Job,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): ThirdPartyContainerInfoV3 {
        return try {
            val container = YamlUtil.getObjectMapper().readValue(
                JsonUtil.toJson(job.runsOn.container!!),
                Container::class.java
            )

            ThirdPartyContainerInfoV3(
                image = container.takeImage(),
                imageCode = container.takeImageCode(),
                imageVersion = container.takeImageVersion(),
                userName = container.credentials?.username,
                password = container.credentials?.password,
                credId = null,
                acrossTemplateId = null,
                options = container.options,
                imagePullPolicy = container.imagePullPolicy,
                imageType = container.takeImageType()
            )
        } catch (e: Exception) {
            val container = YamlUtil.getObjectMapper().readValue(
                JsonUtil.toJson(job.runsOn.container!!),
                Container2::class.java
            )

            ThirdPartyContainerInfoV3(
                image = container.takeImage(),
                imageCode = container.takeImageCode(),
                imageVersion = container.takeImageVersion(),
                userName = null,
                password = null,
                credId = container.credentials,
                acrossTemplateId = buildTemplateAcrossInfo?.templateId,
                options = container.options,
                imagePullPolicy = container.imagePullPolicy,
                imageType = container.takeImageType()
            )
        }
    }
}
