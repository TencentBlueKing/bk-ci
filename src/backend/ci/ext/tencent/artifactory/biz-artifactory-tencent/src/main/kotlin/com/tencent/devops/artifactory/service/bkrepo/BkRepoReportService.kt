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

package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.FILE_NOT_EXIST
import com.tencent.devops.artifactory.service.ReportService
import com.tencent.devops.artifactory.util.PathUtils
import com.tencent.devops.artifactory.util.RepoUtils
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.net.URLEncoder
import javax.ws.rs.NotFoundException

@Service
class BkRepoReportService @Autowired constructor(
    private val bkRepoClient: BkRepoClient,
    private val commonConfig: CommonConfig
) : ReportService {
    override fun get(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        path: String
    ) {
        logger.info("get, userId: $userId, projectId: $projectId, pipelineId: $pipelineId, buildId: $buildId, " +
            "elementId: $elementId, path: $path")
        val normalizedPath = PathUtils.normalize(path)
        val realPath = "/$pipelineId/$buildId/$elementId/${normalizedPath.removePrefix("/")}"
        bkRepoClient.getFileDetail(userId, projectId, RepoUtils.REPORT_REPO, realPath)
            ?: throw NotFoundException(
                    MessageUtil.getMessageByLocale(
                        messageCode = FILE_NOT_EXIST,
                        language = I18nUtil.getLanguage(userId),
                        params = arrayOf(path)
            ))

        val host = HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)
        val redirectUrlBuilder = StringBuilder()
        redirectUrlBuilder.append(
            "$host/bkrepo/api/user/generic/$projectId/${RepoUtils.REPORT_REPO}${
                urlEncode(realPath).replace("%2F", "/")
            }?preview=true"
        )
        val paramMap = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request.parameterMap
        paramMap.forEach { (key, value) ->
            value.forEach {
                redirectUrlBuilder.append("&${urlEncode(key)}=${urlEncode(it)}")
            }
        }
        val response = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).response!!
        response.sendRedirect(redirectUrlBuilder.toString())
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoReportService::class.java)
    }
}
