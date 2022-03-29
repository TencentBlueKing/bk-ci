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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.pojo.BranchBuildHistory
import com.tencent.devops.stream.pojo.GitCIBuildHistory
import com.tencent.devops.stream.pojo.GitRequestEventReq
import com.tencent.devops.stream.pojo.enums.BranchType
import com.tencent.devops.stream.trigger.StreamGitProjectInfoCache
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.StreamBasicSettingService
import com.tencent.devops.stream.v2.service.StreamScmService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class GitCIBranchService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val streamGitProjectInfoCache: StreamGitProjectInfoCache,
    private val streamScmService: StreamScmService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCIBranchService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getBranchBuildList(userId: String, gitProjectId: Long, defaultBranch: String?): List<BranchBuildHistory> {
        val default = (defaultBranch ?: "master").removePrefix("refs/heads/")
        logger.info("get branch build list, gitProjectId: $gitProjectId")
        val conf = streamBasicSettingService.getGitCIConf(gitProjectId)
            ?: throw CustomException(Response.Status.FORBIDDEN, "项目未开启Stream，无法查询")

        val branchBuildsList = gitRequestEventBuildDao.getBranchBuildList(dslContext, gitProjectId)
        if (branchBuildsList.isEmpty()) {
            logger.info("Get branch build list return empty, gitProjectId: $gitProjectId")
            return emptyList()
        }
        logger.info("Get branch build list branchBuildsList: $branchBuildsList, gitProjectId: $gitProjectId")

        val builds = mutableSetOf<String>()
        branchBuildsList.forEach {
            builds.addAll(it.buildIds.split(","))
        }
        logger.info("${conf.projectCode}|$builds|$channelCode")
        val buildHistoryList = client.get(ServiceBuildResource::class).getBatchBuildStatus(
            projectId = conf.projectCode!!,
            buildId = builds,
            channelCode = channelCode
        ).data
        if (null == buildHistoryList) {
            logger.info("Get branch build history list return empty, gitProjectId: $gitProjectId")
            return emptyList()
        }
        logger.info("Get branch build history list buildHistoryList: $buildHistoryList, gitProjectId: $gitProjectId")

        val result = mutableListOf<BranchBuildHistory>()
        branchBuildsList.forEach nextBuild@{
            val gitCIBuildHistoryList = mutableListOf<GitCIBuildHistory>()
            it.buildIds.split(",").forEach { buildIdIt ->
                val history = getBuildHistory(buildHistoryList, buildIdIt)
                val gitRequestBuildEvent = gitRequestEventBuildDao.getByBuildId(dslContext, buildIdIt) ?: return@forEach
                val gitRequestEvent = gitRequestEventDao.get(dslContext, gitRequestBuildEvent.eventId) ?: return@forEach
                val pipeline = pipelineResourceDao.getPipelineById(
                    dslContext = dslContext,
                    gitProjectId = gitProjectId,
                    pipelineId = gitRequestBuildEvent.pipelineId
                ) ?: return@forEach

                gitCIBuildHistoryList.add(
                    GitCIBuildHistory(
                        displayName = pipeline.displayName,
                        pipelineId = pipeline.pipelineId,
                        gitRequestEvent = GitRequestEventReq(gitRequestEvent),
                        buildHistory = history
                    )
                )
            }
            // 如果是来自fork库的分支，单独标识
            val gitProjectInfoCache = it.sourceGitProjectId?.let { id ->
                lazy {
                    streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                        gitProjectId = id,
                        useAccessToken = true,
                        getProjectInfo = streamScmService::getProjectInfoRetry
                    )
                }
            }
            result.add(
                BranchBuildHistory(
                    branchName = GitCommonUtils.checkAndGetForkBranchName(
                        gitProjectId = it.gitProjectId,
                        sourceGitProjectId = it.sourceGitProjectId,
                        branch = it.branch,
                        gitProjectCache = gitProjectInfoCache
                    ),
                    buildTotal = it.buildTotal,
                    branchType = if (default.equals(it.branch, true)) {
                        BranchType.Default
                    } else {
                        BranchType.Active
                    },
                    buildHistory = gitCIBuildHistoryList
                )
            )
        }
        return result
    }

    private fun getBuildHistory(buildHistoryList: List<BuildHistory>, buildIdIt: String): BuildHistory? {
        buildHistoryList.forEach {
            if (it.id == buildIdIt) {
                return it
            }
        }
        return null
    }
}
