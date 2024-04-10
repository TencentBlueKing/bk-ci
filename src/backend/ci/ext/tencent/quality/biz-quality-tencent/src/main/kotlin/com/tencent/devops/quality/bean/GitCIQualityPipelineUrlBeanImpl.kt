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

package com.tencent.devops.quality.bean

import com.tencent.devops.artifactory.api.service.ServiceShortUrlResource
import com.tencent.devops.artifactory.pojo.CreateShortUrlRequest
import com.tencent.devops.common.client.Client
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.springframework.beans.factory.annotation.Value

class GitCIQualityPipelineUrlBeanImpl constructor(
    private val client: Client
) : QualityUrlBean {

    @Value("\${gitci.v2GitUrl:#{null}}")
    private val v2GitUrl: String? = null

    override fun genBuildDetailUrl(
        projectCode: String,
        pipelineId: String,
        buildId: String,
        position: String,
        stageId: String?,
        runtimeVariable: Map<String, String>?
    ): String {
        logger.info("[$buildId]|genGitCIBuildDetailUrl| host=$v2GitUrl")
        val project = client.getScm(ServiceGitCiResource::class)
            .getGitCodeProjectInfo(projectCode.removePrefix("git_"))
            .data ?: return ""
        val urlParam = StringBuffer("")
        if (!position.isNullOrBlank() && !stageId.isNullOrBlank()) {
            when (position) {
                ControlPointPosition.BEFORE_POSITION -> {
                    urlParam.append("?checkIn=$stageId")
                }
                ControlPointPosition.AFTER_POSITION -> {
                    urlParam.append("?checkOut=$stageId")
                }
            }
        }
        val url = "$v2GitUrl/pipeline/$pipelineId/detail/$buildId$urlParam#${project.pathWithNamespace}"
        return client.get(ServiceShortUrlResource::class).createShortUrl(CreateShortUrlRequest(url, TTL)).data!!
    }

    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(GitCIQualityPipelineUrlBeanImpl::class.java)
        private const val TTL = 24 * 3600 * 3
    }
}
