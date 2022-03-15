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

import com.tencent.devops.common.ci.v2.RepositoryHook
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.util.CommonCredentialUtils
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.stream.dao.GitPipelineRepoResourceDao
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.StreamRepoHookEvent
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepoTriggerEventService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitPipelineRepoResourceDao: GitPipelineRepoResourceDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RepoTriggerEventService::class.java)
    }

    fun saveRepoTriggerEvent(
        streamRepoHookEvent: StreamRepoHookEvent
    ) {
        gitPipelineRepoResourceDao.create(
            dslContext = dslContext,
            streamRepoHookEvent = streamRepoHookEvent
        )
    }

    fun getTargetProjectIdByPipeline(pipelineId: String): Long? {
        return gitPipelineRepoResourceDao.get(dslContext = dslContext, pipelineId = pipelineId)?.targetGitProjectId
    }

    fun getTargetPipelines(
        sourceGitProjectPath: String?
    ): List<StreamRepoHookEvent> {
        if (sourceGitProjectPath.isNullOrEmpty()) {
            return emptyList()
        }
        val sourceGitProjectPathList = mutableListOf<String>()
        var begin = sourceGitProjectPath.indexOf("/", 0)
        while (begin != -1) {
            sourceGitProjectPathList.add(sourceGitProjectPath.substring(0, begin) + "/**")
            begin = sourceGitProjectPath.indexOf("/", begin + 1)
        }
        sourceGitProjectPathList.add(sourceGitProjectPath)
        logger.info("find target repo pipelines in ($sourceGitProjectPathList)")
        return gitPipelineRepoResourceDao.getPipelineBySourcePath(
            dslContext = dslContext,
            sourceGitProjectPathList = sourceGitProjectPathList
        ).map {
            StreamRepoHookEvent(
                pipelineId = it.value1(),
                sourceGitProjectPath = it.value2(),
                targetGitProjectId = it.value3()
            )
        }
    }

    fun checkRepoTriggerCredentials(
        gitRequestEventForHandle: GitRequestEventForHandle,
        repoHook: RepositoryHook?
    ): Boolean {
        if (repoHook == null) {
            return true
        }
        val token = when {
            repoHook.credentialsForTicketId != null ->
                CommonCredentialUtils.getCredential(
                    client = client,
                    projectId = "git_${gitRequestEventForHandle.gitProjectId}",
                    credentialId = repoHook.credentialsForTicketId!!,
                    type = CredentialType.ACCESSTOKEN
                )["v1"] ?: return false
            repoHook.credentialsForToken != null -> repoHook.credentialsForToken!!
            else -> return false
        }
        val userId = client.getScm(ServiceGitResource::class)
            .getUserInfoByToken(token = token, useAccessToken = false).data?.username
            ?: gitRequestEventForHandle.userId
        return client.getScm(ServiceGitCiResource::class)
            .checkUserGitAuth(
                userId = userId,
                gitProjectId = gitRequestEventForHandle.gitProjectId.toString(),
                privateToken = token,
                useAccessToken = false,
                accessLevel = 40
            ).data ?: false
    }

    fun deleteRepoTriggerEvent(
        pipelineId: String
    ): Boolean {
        return gitPipelineRepoResourceDao.deleteByPipelineId(dslContext, pipelineId) > 0
    }
}
