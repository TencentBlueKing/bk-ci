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

package com.tencent.devops.gitci.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventDao
import com.tencent.devops.gitci.pojo.GitCIBuildHistory
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCIHistoryService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitCISettingDao: GitCISettingDao,
    private val repositoryConfService: GitRepositoryConfService,
    private val pipelineResourceDao: GitPipelineResourceDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCIHistoryService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getHistoryBuildList(
        userId: String,
        gitProjectId: Long,
        page: Int?,
        pageSize: Int?,
        branch: String?,
        triggerUser: String?,
        pipelineId: String?
    ): Page<GitCIBuildHistory> {
        logger.info("get history build list, gitProjectId: $gitProjectId")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId)
        if (conf == null) {
            repositoryConfService.initGitCISetting(userId, gitProjectId)
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0,
                records = emptyList()
            )
        }
        val gitRequestBuildList = gitRequestEventBuildDao.getRequestEventBuildList(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            branchName = branch,
            triggerUser = triggerUser,
            pipelineId = pipelineId
        )
        val builds = gitRequestBuildList.map { it.buildId }.toSet()
        logger.info("get history build list, build ids: $builds")
        val buildHistoryList = client.get(ServiceBuildResource::class).getBatchBuildStatus(conf.projectCode!!, builds, channelCode).data
        if (null == buildHistoryList) {
            logger.info("Get branch build history list return empty, gitProjectId: $gitProjectId")
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0,
                records = emptyList()
            )
        }

        val records = mutableListOf<GitCIBuildHistory>()
        gitRequestBuildList.forEach {
            val gitRequestEvent = gitRequestEventDao.get(dslContext, it.eventId) ?: return@forEach
            var realEvent = gitRequestEvent

            // 如果是来自fork库的分支，单独标识
//            if (gitRequestEvent.sourceGitProjectId != null) {
//                try {
//                    val gitToken = client.getScm(ServiceGitResource::class).getToken(gitRequestEvent.sourceGitProjectId!!).data!!
//                    logger.info("get token for gitProjectId[${gitRequestEvent.sourceGitProjectId!!}] form scm, token: $gitToken")
//                    val sourceRepositoryConf = client.getScm(ServiceGitResource::class).getProjectInfo(gitToken.accessToken, gitRequestEvent.sourceGitProjectId!!).data
//                    realEvent = gitRequestEvent.copy(
//                        branch = if (sourceRepositoryConf != null) "${sourceRepositoryConf.name}:${gitRequestEvent.branch}"
//                        else gitRequestEvent.branch)
//                } catch (e: Exception) {
//                    logger.error("Cannot get source GitProjectInfo: ", e)
//                }
//            }

            val buildHistory = getBuildHistory(it.buildId, buildHistoryList) ?: return@forEach
            val pipeline = pipelineResourceDao.getPipelineById(dslContext, gitProjectId, it.pipelineId) ?: return@forEach
            records.add(GitCIBuildHistory(
                displayName = pipeline.displayName,
                pipelineId = pipeline.pipelineId,
                gitRequestEvent = realEvent,
                buildHistory = buildHistory
            ))
        }
        val firstIndex = (pageNotNull - 1) * pageSizeNotNull
        val lastIndex = if (pageNotNull * pageSizeNotNull > records.size) records.size else pageNotNull * pageSizeNotNull
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = records.size.toLong(),
            records = records.subList(firstIndex, lastIndex)
        )
    }

    private fun getBuildHistory(buildId: String, buildHistoryList: List<BuildHistory>): BuildHistory? {
        buildHistoryList.forEach {
            if (it.id == buildId) {
                return it
            }
        }
        return null
    }
}
