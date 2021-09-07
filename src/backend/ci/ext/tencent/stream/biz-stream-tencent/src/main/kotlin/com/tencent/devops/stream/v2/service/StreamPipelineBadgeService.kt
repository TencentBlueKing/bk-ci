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

package com.tencent.devops.stream.v2.service

import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.OkhttpUtils.stringLimit
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.stream.common.StreamPipelineBadgeType
import com.tencent.devops.stream.config.StreamBadgeConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import sun.font.CreatedFontTracker.MAX_FILE_SIZE
import java.net.URLDecoder

@Service
class StreamPipelineBadgeService @Autowired constructor(
    private val dslContext: DSLContext,
    val badgeConfig: StreamBadgeConfig,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamPipelineBadgeType::class.java)
    }

    fun get(gitProjectId: Long, filePath: String, branch: String?, objectKind: String?): String {
        val (pipelineName, type) = getType(gitProjectId, filePath, branch, objectKind)
        val params = mapOf(
            "color" to type.color,
            "labelColor" to type.labelColor,
            "label" to pipelineName,
            "message" to type.text,
            "logo" to "${badgeConfig.logoUrl}/${type.logo}.svg"
        )
        val url = "${badgeConfig.serverUrl}/release?${OkhttpUtils.joinParams(params)}"

        OkhttpUtils.doGet(url).use { response ->
            val body = response.stringLimit(readLimit = MAX_FILE_SIZE, errorMsg = "请求文件不能超过1M")
            logger.info("get badge body: $body")
            return body
        }
    }

    private fun getType(
        gitProjectId: Long,
        filePath: String,
        branch: String?,
        objectKind: String?
    ): Pair<String, StreamPipelineBadgeType> {
        val realFilePath = URLDecoder.decode(filePath, "UTF-8")
        val pipeline = pipelineResourceDao.getPipelineByFile(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            filePath = realFilePath
        )
        if (pipeline?.pipelineId.isNullOrBlank()) {
            return Pair(realFilePath, StreamPipelineBadgeType.NOT_FOUND)
        }
        val buildHistory = gitRequestEventBuildDao.getLastEventByPipelineId(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            pipelineId = pipeline!!.pipelineId,
            branch = branch,
            objectKind = objectKind
        ) ?: return Pair(pipeline.displayName, StreamPipelineBadgeType.NEVER_BUILD)

        return if (BuildStatus.parse(buildHistory.buildStatus).isSuccess()) {
            Pair(pipeline.displayName, StreamPipelineBadgeType.SUCCEEDED)
        } else {
            Pair(pipeline.displayName, StreamPipelineBadgeType.FAILED)
        }
    }
}
