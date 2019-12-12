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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.gitci.client

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.CommitCheckRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Collections

@Service
class ScmClient @Autowired constructor(
    private val client: Client
) {
    fun pushCommitCheck(
        commitId: String,
        description: String,
        mergeRequestId: Long,
        buildId: String,
        userId: String,
        status: String,
        gitProjectConf: GitRepositoryConf
    ) = try {
        val titleData = mutableListOf<String>()
        val resultMap = mutableMapOf<String, MutableList<List<String>>>()

        val token = getAccessToken(gitProjectConf.gitProjectId).first
        val buildNum = getBuildNum(gitProjectConf.projectCode.toString(), buildId)
        val request = CommitCheckRequest(
            projectName = gitProjectConf.gitProjectId.toString(),
            url = gitProjectConf.gitHttpUrl,
            type = ScmType.CODE_GIT,
            privateKey = null,
            passPhrase = null,
            token = token,
            region = null,
            commitId = commitId,
            state = status,
            targetUrl = gitProjectConf.homepage + "/ci/pipelines#/build/" + buildId + "?buildNum=" + buildNum,
            context = "",
            description = description,
            block = false,
            mrRequestId = mergeRequestId,
            reportData = Pair(titleData, resultMap)
        )
        logger.info("user $userId buildId $buildId pushCommitCheck: $request")
        client.getScm(ServiceGitResource::class).addCommitCheck(request)
    } catch (e: Exception) {
        logger.error("user $userId buildId $buildId pushCommitCheck error.", e)
    }

    private fun getAccessToken(gitProjectId: Long): Pair<String, String?> {
        val gitOauthData = client.getScm(ServiceGitResource::class).getToken(gitProjectId).data
            ?: throw RuntimeException("cannot found oauth access token for user($gitProjectId)")
        return gitOauthData.accessToken to null
    }

    private fun getBuildNum(projectCode: String, buildId: String): String {
        val buildHistoryList = client.get(ServiceBuildResource::class).getBatchBuildStatus(projectCode, Collections.singleton(buildId), ChannelCode.GIT).data
        return if (null == buildHistoryList || buildHistoryList.isEmpty()) {
            logger.info("Get branch build history list return empty, gitProjectId: $projectCode")
            ""
        } else {
            buildHistoryList[0].buildNum.toString()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ScmClient::class.java)
    }
}