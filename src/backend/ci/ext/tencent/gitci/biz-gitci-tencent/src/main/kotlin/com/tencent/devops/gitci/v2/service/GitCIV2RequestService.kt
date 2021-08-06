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

package com.tencent.devops.gitci.v2.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventDao
import com.tencent.devops.gitci.dao.GitRequestEventNotBuildDao
import com.tencent.devops.gitci.pojo.GitCIBuildHistory
import com.tencent.devops.gitci.pojo.GitRequestHistory
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.v2.message.RequestMessageContent
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.model.gitci.tables.records.TGitRequestEventBuildRecord
import com.tencent.devops.model.gitci.tables.records.TGitRequestEventNotBuildRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class GitCIV2RequestService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val gitCIBasicSettingService: GitCIBasicSettingService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCIV2RequestService::class.java)
        private const val ymlVersion = "v2.0"
    }

    private val channelCode = ChannelCode.GIT

    fun getRequestList(userId: String, gitProjectId: Long, page: Int?, pageSize: Int?): Page<GitRequestHistory> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        logger.info("get request list, gitProjectId: $gitProjectId")
        val conf = gitCIBasicSettingService.getGitCIBasicSettingAndCheck(gitProjectId)
        val count = gitRequestEventDao.getRequestCount(dslContext, gitProjectId)
        val requestList = gitRequestEventDao.getRequestList(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            page = pageNotNull,
            pageSize = pageSizeNotNull
        )
        if (requestList.isEmpty() || count == 0L) {
            logger.info("Get request build list return empty, gitProjectId: $gitProjectId")
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0L,
                records = emptyList()
            )
        }
        val resultList = mutableListOf<GitRequestHistory>()
        requestList.forEach { event ->
            // 如果是来自fork库的分支，单独标识
            val realEvent = GitCommonUtils.checkAndGetForkBranch(event, client)

            val requestHistory = GitRequestHistory(
                id = realEvent.id ?: return@forEach,
                gitProjectId = gitProjectId,
                commitId = realEvent.commitId,
                commitMsg = realEvent.commitMsg,
                branch = realEvent.branch,
                objectKind = realEvent.objectKind,
                commitTimeStamp = realEvent.commitTimeStamp,
                userId = realEvent.userId,
                description = realEvent.description,
                targetBranch = realEvent.targetBranch,
                mrTitle = realEvent.mrTitle,
                operationKind = realEvent.operationKind,
                mergeRequestId = realEvent.mergeRequestId,
                totalCommitCount = realEvent.totalCommitCount,
                buildRecords = mutableListOf()
            )

            // 已触发的所有记录
            val buildsList = gitRequestEventBuildDao.getRequestBuildsByEventId(dslContext, realEvent.id!!)
            logger.info("Get build list requestBuildsList: $buildsList, gitProjectId: $gitProjectId")
            val builds = buildsList.map { it.buildId }.toSet()
            val buildList = client.get(ServiceBuildResource::class)
                .getBatchBuildStatus(conf.projectCode!!, builds, channelCode).data
            if (buildList?.isEmpty() == false) {
                logger.info("Get build history list buildHistoryList: $buildList, gitProjectId: $gitProjectId")
                val records = mutableListOf<GitCIBuildHistory>()
                buildsList.forEach nextBuild@{
                    try {
                        val history = getBuildHistory(buildList, it.buildId ?: return@nextBuild)
                        val pipeline = pipelineResourceDao.getPipelineById(dslContext, gitProjectId, it.pipelineId)
                            ?: return@nextBuild
                        records.add(
                            GitCIBuildHistory(
                                displayName = pipeline.displayName,
                                pipelineId = pipeline.pipelineId,
                                gitRequestEvent = realEvent,
                                buildHistory = history,
                                reason = TriggerReason.TRIGGER_SUCCESS.name,
                                reasonDetail = null
                            )
                        )
                    } catch (e: Exception) {
                        logger.error(
                            "Load gitProjectId: ${it.gitProjectId}, eventId: ${it.eventId}, pipelineId:" +
                                " ${it.pipelineId} failed with error: ",
                            e
                        )
                        return@nextBuild
                    }
                }
                requestHistory.buildRecords.addAll(records)
            } else {
                logger.info("Get branch build history list return empty, gitProjectId: $gitProjectId")
            }
            // -------

            // 未触发的所有记录
            val noBuildList = gitRequestEventNotBuildDao.getRequestNoBuildsByEventId(dslContext, event.id!!)
            logger.info("Get no build list requestBuildsList: $noBuildList, gitProjectId: $gitProjectId")
            val records = mutableListOf<GitCIBuildHistory>()

            // 取所有记录的非空流水线ID，查出对应流水线
            val pipelineIds: List<String> = noBuildList.filter {
                !it.pipelineId.isNullOrBlank()
            }.map { it.pipelineId }
            val pipelineMap = pipelineResourceDao.getPipelinesInIds(dslContext, gitProjectId, pipelineIds).map {
                it.pipelineId to it
            }.toMap()

            noBuildList.forEach nextBuild@{
                val pipeline = if (it.pipelineId.isNullOrBlank()) null else pipelineMap[it.pipelineId]
                records.add(
                    GitCIBuildHistory(
                        displayName = pipeline?.displayName,
                        pipelineId = pipeline?.pipelineId,
                        gitRequestEvent = event,
                        buildHistory = null,
                        reason = it.reason,
                        reasonDetail = it.reasonDetail
                    )
                )
            }
            requestHistory.buildRecords.addAll(records)
            // -------

            resultList.add(requestHistory)
        }
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = count,
            records = resultList
        )
    }

    fun getRequestMap(
        userId: String,
        gitProjectId: Long?,
        requestIds: Set<Int>
    ): Map<Long, List<RequestMessageContent>> {
        val eventList = gitRequestEventDao.getRequestsById(
            dslContext = dslContext,
            requestIds = requestIds
        )
        if (eventList.isEmpty()) {
            return emptyMap()
        }
        val eventIds = eventList.map { it.id!! }.toSet()
        val resultMap = mutableMapOf<Long, List<RequestMessageContent>>()

        // 未触发的所有记录
        val noBuildList = gitRequestEventNotBuildDao.getListByEventIds(dslContext, eventIds)
        val noBuildMap = getNoBuildEventMap(noBuildList)
        // 触发的所有记录
        val buildList = gitRequestEventBuildDao.getByEventIds(dslContext, eventIds)
        val buildMap = getBuildEventMap(buildList)
        // 项目ID可能为空，用所有的构建记录的流水线ID查询流水线
        val pipelineMap = if (gitProjectId != null) {
            pipelineResourceDao.getPipelines(dslContext, gitProjectId).associateBy { it.pipelineId }
        } else {
            val pipelineIds =
                (noBuildList.map { it.pipelineId }.toSet() + buildList.map { it.pipelineId }.toSet()).toList()
            pipelineResourceDao.getPipelinesInIds(dslContext, null, pipelineIds).associateBy { it.pipelineId }
        }

        val processBuildList = client.get(ServiceBuildResource::class)
            .getBatchBuildStatus(
                projectId = "git_$gitProjectId",
                buildId = buildList.map { it.buildId }.toSet(),
                channelCode = channelCode
            ).data?.associateBy { it.id }

        eventList.forEach { event ->
            val eventId = event.id!!
            val records = mutableListOf<RequestMessageContent>()
            // 添加未构建的记录
            noBuildMap[eventId]?.forEach nextBuild@{
                val pipeline = if (it.pipelineId.isNullOrBlank()) {
                    null
                } else {
                    pipelineMap[it.pipelineId]
                }
                records.add(
                    RequestMessageContent(
                        id = it.id,
                        pipelineName = pipeline?.displayName ?: it.filePath,
                        buildBum = null,
                        triggerReasonName = it.reason,
                        triggerReasonDetail = it.reasonDetail
                    )
                )
            }
            // 添加构建的记录
            buildMap[eventId]?.forEach nextBuild@{
                val buildNum = processBuildList?.get(it.buildId)?.buildNum
                val pipeline = pipelineMap[it.pipelineId]
                records.add(
                    RequestMessageContent(
                        id = it.id,
                        pipelineName = pipeline?.displayName,
                        buildBum = buildNum,
                        triggerReasonName = TriggerReason.TRIGGER_SUCCESS.name,
                        triggerReasonDetail = null
                    )
                )
            }
            resultMap[eventId] = records
        }
        return resultMap
    }

    private fun getBuildHistory(buildHistoryList: List<BuildHistory>, buildIdIt: String): BuildHistory? {
        buildHistoryList.forEach {
            if (it.id == buildIdIt) {
                return it
            }
        }
        return null
    }

    private fun getBuildEventMap(
        buildList: List<TGitRequestEventBuildRecord>
    ): MutableMap<Long, MutableList<TGitRequestEventBuildRecord>> {
        val resultMap = mutableMapOf<Long, MutableList<TGitRequestEventBuildRecord>>()
        buildList.forEach {
            if (resultMap[it.eventId].isNullOrEmpty()) {
                resultMap[it.eventId] = mutableListOf(it)
            } else {
                resultMap[it.eventId]!!.add(it)
            }
        }
        return resultMap
    }

    private fun getNoBuildEventMap(
        noBuildList: List<TGitRequestEventNotBuildRecord>
    ): MutableMap<Long, MutableList<TGitRequestEventNotBuildRecord>> {
        val resultMap = mutableMapOf<Long, MutableList<TGitRequestEventNotBuildRecord>>()
        noBuildList.forEach {
            if (resultMap[it.eventId].isNullOrEmpty()) {
                resultMap[it.eventId] = mutableListOf(it)
            } else {
                resultMap[it.eventId]!!.add(it)
            }
        }
        return resultMap
    }
}
