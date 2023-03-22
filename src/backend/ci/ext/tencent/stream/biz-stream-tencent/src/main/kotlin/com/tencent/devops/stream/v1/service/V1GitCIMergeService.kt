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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.constant.StreamCode.BK_PROJECT_CANNOT_OPEN_STREAM
import com.tencent.devops.stream.service.StreamScmService
import com.tencent.devops.stream.v1.components.V1StreamGitProjectInfoCache
import com.tencent.devops.stream.v1.dao.V1GitPipelineResourceDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventBuildDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventDao
import com.tencent.devops.stream.v1.pojo.V1GitCIBuildHistory
import com.tencent.devops.stream.v1.pojo.V1GitMergeHistory
import com.tencent.devops.stream.v1.pojo.V1GitRequestEventReq
import com.tencent.devops.stream.v1.utils.V1GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Suppress("NestedBlockDepth", "ComplexMethod")
@Service
class V1GitCIMergeService @Autowired constructor(
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
        private val logger = LoggerFactory.getLogger(V1GitCIMergeService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getMergeBuildList(userId: String, gitProjectId: Long, page: Int?, pageSize: Int?): Page<V1GitMergeHistory> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        logger.info("get merge build list, gitProjectId: $gitProjectId")
        val conf = streamBasicSettingService.getGitCIConf(gitProjectId)
            ?: throw CustomException(Response.Status.FORBIDDEN,
                MessageUtil.getMessageByLocale(
                    messageCode = BK_PROJECT_CANNOT_OPEN_STREAM,
                    language = I18nUtil.getLanguage(userId)
                ))
        val mergeList = gitRequestEventDao.getMergeRequestList(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            page = pageNotNull,
            pageSize = pageSizeNotNull
        )
        if (mergeList.isEmpty()) {
            logger.info("Get merge request build list return empty, gitProjectId: $gitProjectId")
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0L,
                records = emptyList()
            )
        }

        val mergeHistoryMap = mutableMapOf<Long, V1GitMergeHistory>()
        mergeList.forEach { event ->
            val mrId = event.mergeRequestId ?: return@forEach
            // 如果是来自fork库的分支，单独标识
            val gitProjectInfoCache = event.sourceGitProjectId?.let {
                lazy {
                    streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                        gitProjectId = it,
                        useAccessToken = true,
                        getProjectInfo = streamScmService::getProjectInfoRetry
                    )
                }
            }
            val realEvent = V1GitCommonUtils.checkAndGetForkBranch(event, gitProjectInfoCache)

            val mergeHistory = V1GitMergeHistory(
                id = realEvent.id ?: return@forEach,
                gitProjectId = gitProjectId,
                mergeRequestId = mrId,
                mrTitle = realEvent.mrTitle!!,
                branch = realEvent.branch,
                targetBranch = realEvent.targetBranch!!,
                extensionAction = realEvent.extensionAction,
                operationKind = realEvent.operationKind,
                commitTimeStamp = realEvent.commitTimeStamp,
                totalCommitCount = realEvent.totalCommitCount,
                userId = realEvent.userId,
                description = realEvent.description
            )
            val mergeBuildsList = gitRequestEventBuildDao.getRequestBuildsByEventId(
                dslContext = dslContext,
                eventId = realEvent.id!!,
                gitProjectId = gitProjectId
            )
            logger.info("Get merge build list mergeBuildsList: $mergeBuildsList, gitProjectId: $gitProjectId")
            val builds = mergeBuildsList.map { it.buildId }.toSet()
            val buildList = client.get(ServiceBuildResource::class).getBatchBuildStatus(
                projectId = conf.projectCode!!,
                buildId = builds,
                channelCode = channelCode
            ).data
            if (buildList?.isEmpty() == false) {
                logger.info("Get merge build history list buildHistoryList: $buildList, gitProjectId: $gitProjectId")
                val records = mutableListOf<V1GitCIBuildHistory>()
                mergeBuildsList.forEach nextBuild@{
                    try {
                        val history = getBuildHistory(buildList, it.buildId ?: return@nextBuild)
                        val pipeline = pipelineResourceDao.getPipelineById(dslContext, gitProjectId, it.pipelineId)
                            ?: return@nextBuild
                        records.add(
                            V1GitCIBuildHistory(
                                displayName = pipeline.displayName,
                                pipelineId = pipeline.pipelineId,
                                gitRequestEvent = V1GitRequestEventReq(realEvent),
                                buildHistory = history
                            )
                        )
                    } catch (e: Exception) {
                        logger.warn(
                            "Load gitProjectId: ${it.gitProjectId}, " +
                                "eventId: ${it.eventId}, pipelineId: ${it.pipelineId} failed with error: ",
                            e
                        )
                        return@nextBuild
                    }
                }
                mergeHistory.buildRecords = records
            } else {
                logger.info("Get branch build history list return empty, gitProjectId: $gitProjectId")
                return@forEach
            }
            if (mergeHistoryMap.containsKey(mrId)) {
                mergeHistoryMap[mrId]?.buildRecords?.addAll(mergeHistory.buildRecords ?: return@forEach)
            } else {
                mergeHistoryMap[mrId] = mergeHistory
            }
        }
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = mergeHistoryMap.size.toLong(),
            records = mergeHistoryMap.values.toList()
        )
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
