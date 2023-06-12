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

package com.tencent.devops.stream.service

import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.pojo.enums.StreamPipelineBadgeType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLDecoder

@Service
class StreamPipelineBadgeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao
) {

    @Value("\${badge.serverUrl:#{null}}")
    private val badgeServerUrl: String? = null

    fun get(gitProjectId: Long, filePath: String, branch: String?, objectKind: String?): String {
        if (badgeServerUrl == null) {
            throw RuntimeException("can't found badge server info")
        }
        val (pipelineName, type) = getType(gitProjectId, filePath, branch, objectKind)

        val url = "$badgeServerUrl?label=$pipelineName&message=${type.text}&status=${type.name}&logo=${type.logo}"
        OkhttpUtils.doGet(url).use { resp ->
            if (!resp.isSuccessful) throw RuntimeException(
                "get badge error code: ${resp.code} message: ${resp.message}"
            )
            return resp.body!!.string()
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
        val buildHistory = gitRequestEventBuildDao.getLastBuildEventByPipelineId(
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
