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

package com.tencent.devops.stream.v1.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.service.StreamScmService
import com.tencent.devops.stream.v1.components.V1StreamGitProjectInfoCache
import com.tencent.devops.stream.v1.dao.V1GitPipelineResourceDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventBuildDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventDao
import com.tencent.devops.stream.v1.pojo.V1BranchBuildHistory
import com.tencent.devops.stream.v1.pojo.V1GitCIBuildHistory
import com.tencent.devops.stream.v1.pojo.V1GitRequestEventReq
import com.tencent.devops.stream.v1.pojo.enums.V1BranchType
import com.tencent.devops.stream.v1.utils.V1GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class V1GitCIBranchService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val streamBasicSettingService: V1StreamBasicSettingService,
    private val gitRequestEventBuildDao: V1GitRequestEventBuildDao,
    private val gitRequestEventDao: V1GitRequestEventDao,
    private val pipelineResourceDao: V1GitPipelineResourceDao,
    private val streamGitProjectInfoCache: V1StreamGitProjectInfoCache,
    private val streamScmService: StreamScmService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(V1GitCIBranchService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getBranchBuildList(userId: String, gitProjectId: Long, defaultBranch: String?): List<V1BranchBuildHistory> {
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
        logger.info("Get branch build history list buildHistoryList, gitProjectId: $gitProjectId")

        val result = mutableListOf<V1BranchBuildHistory>()
        branchBuildsList.forEach nextBuild@{
            val gitCIBuildHistoryList = mutableListOf<V1GitCIBuildHistory>()
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
                    V1GitCIBuildHistory(
                        displayName = pipeline.displayName,
                        pipelineId = pipeline.pipelineId,
                        gitRequestEvent = V1GitRequestEventReq(gitRequestEvent),
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
                V1BranchBuildHistory(
                    branchName = V1GitCommonUtils.checkAndGetForkBranchName(
                        gitProjectId = it.gitProjectId,
                        sourceGitProjectId = it.sourceGitProjectId,
                        branch = it.branch,
                        gitProjectCache = gitProjectInfoCache
                    ),
                    buildTotal = it.buildTotal,
                    branchType = if (default.equals(it.branch, true)) {
                        V1BranchType.Default
                    } else {
                        V1BranchType.Active
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
