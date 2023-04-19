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

package com.tencent.devops.process.bean

import com.tencent.devops.artifactory.api.service.ServiceShortUrlResource
import com.tencent.devops.artifactory.pojo.CreateShortUrlRequest
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil

class TencentPipelineUrlBeanImpl constructor(
    private val commonConfig: CommonConfig,
    private val client: Client
) : PipelineUrlBean {

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(TencentPipelineUrlBeanImpl::class.java)
        private const val TTL = 24 * 3600 * 3
    }
    override fun genBuildDetailUrl(
        projectCode: String,
        pipelineId: String,
        buildId: String,
        position: String?,
        stageId: String?,
        needShortUrl: Boolean
    ): String {
        val devopsHostGateway = HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)
        logger.info("[$buildId]|genBuildDetailUrl| host=$devopsHostGateway")
        val url = "$devopsHostGateway/console/pipeline/$projectCode/$pipelineId/detail/$buildId"
        return try {
            if (needShortUrl) {
                client.get(ServiceShortUrlResource::class).createShortUrl(CreateShortUrlRequest(url, TTL)).data!!
            } else url
        } catch (ignore: Throwable) {
            logger.warn("[$buildId]|genBuildDetailUrl| failed with:", ignore)
            url
        }
    }

    override fun genAppBuildDetailUrl(projectCode: String, pipelineId: String, buildId: String): String {
        val devopsOuterHostGateWay = HomeHostUtil.getHost(commonConfig.devopsOuterHostGateWay!!)
        logger.info("[$buildId]|genBuildDetailUrl| outHost=$devopsOuterHostGateWay")
        val url = "$devopsOuterHostGateWay/app/download/devops_app_forward.html" +
            "?flag=buildReport&projectId=$projectCode&pipelineId=$pipelineId&buildId=$buildId"
        return try {
            client.get(ServiceShortUrlResource::class).createShortUrl(CreateShortUrlRequest(url, TTL)).data!!
        } catch (ignore: Throwable) {
            logger.warn("[$buildId]|genBuildDetailUrl| failed with:", ignore)
            url
        }
    }
}
