/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineArchiveEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBatchArchiveEvent
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.model.process.tables.records.TPipelineBuildSummaryRecord
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.PipelineCollation
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.service.PipelineListQueryParamService
import com.tencent.devops.process.util.BuildMsgUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList")
class ArchivePipelineManageService @Autowired constructor(
    @Qualifier(ARCHIVE_SHARDING_DSL_CONTEXT)
    private var archiveShardingDslContext: DSLContext,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineListQueryParamService: PipelineListQueryParamService,
    private val pipelineStatusService: PipelineStatusService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val sampleEventDispatcher: SampleEventDispatcher
) {

    companion object {
        private const val PIPELINE_BATCH_ARCHIVE_EXPIRED_IN_HOUR = 36L // 流水线批量归档过期时间
    }

    fun migrateData(
        userId: String,
        projectId: String,
        pipelineId: String,
        cancelFlag: Boolean = false
    ): Boolean {
        // 发送迁移归档流水线数据消息
        pipelineEventDispatcher.dispatch(
            PipelineArchiveEvent(
                source = "archive_pipeline",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                cancelFlag = cancelFlag
            )
        )
        return true
    }

    fun batchMigrateData(
        userId: String,
        projectId: String,
        cancelFlag: Boolean = false,
        pipelineIds: Set<String>
    ): Boolean {
        sampleEventDispatcher.dispatch(
            PipelineBatchArchiveEvent(
                source = "batch_archive_pipeline",
                projectId = projectId,
                pipelineIds = pipelineIds,
                userId = userId,
                cancelFlag = cancelFlag,
                expiredInHour = PIPELINE_BATCH_ARCHIVE_EXPIRED_IN_HOUR
            )
        )
        return true
    }

    fun getArchivedPipelineList(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int,
        filterByPipelineName: String?,
        filterByCreator: String?,
        filterByLabels: String?,
        sortType: PipelineSortType?,
        collation: PipelineCollation?
    ): Page<PipelineInfo> {
        val pipelineFilterParamList = pipelineListQueryParamService.generatePipelineFilterParams(
            projectId = projectId,
            filterByPipelineName = filterByPipelineName,
            filterByCreator = filterByCreator,
            filterByLabels = filterByLabels
        )
        val totalCount = pipelineBuildSummaryDao.listPipelineInfoBuildSummaryCount(
            dslContext = archiveShardingDslContext,
            projectId = projectId,
            pipelineFilterParamList = pipelineFilterParamList,
            userId = userId
        )
        val pipelineRecords = pipelineBuildSummaryDao.listPipelineInfoBuildSummary(
            dslContext = archiveShardingDslContext,
            projectId = projectId,
            sortType = sortType,
            pipelineFilterParamList = pipelineFilterParamList,
            page = page,
            pageSize = pageSize,
            collation = collation ?: PipelineCollation.DEFAULT,
            userId = userId
        )
        return if (pipelineRecords.isNotEmpty) {
            val pipelineIds = pipelineRecords.map { it.pipelineId }
            val latestBuildMap = mutableMapOf<String/*buildId*/, String/*pipelineId*/>()
            // 根据pipelineId获取流水线构建汇总信息
            val pipelineBuildSummaryMap = pipelineBuildSummaryDao.listSummaryByPipelineIds(
                dslContext = archiveShardingDslContext,
                pipelineIds = pipelineIds,
                projectId = projectId
            ).map { pipelineBuildSummaryRecord ->
                val latestBuildId = pipelineBuildSummaryRecord.latestBuildId
                val pipelineId = pipelineBuildSummaryRecord.pipelineId
                if (latestBuildId != null) {
                    latestBuildMap[latestBuildId] = pipelineId
                }
                pipelineId to pipelineBuildSummaryRecord
            }.toMap()

            // 根据latestBuildId获取最新构建的信息
            val pipelineBuildMap = pipelineBuildDao.listBuildInfoByBuildIds(
                dslContext = archiveShardingDslContext,
                projectId = projectId,
                buildIds = latestBuildMap.keys
            ).map { it.pipelineId to it }.toMap()
            val pipelineInfos = mutableListOf<PipelineInfo>()
            // 组装生成流水线信息
            pipelineRecords.forEach { pipelineRecord ->
                generateArchivedPipelineInfo(
                    pipelineRecord = pipelineRecord,
                    pipelineBuildSummaryMap = pipelineBuildSummaryMap,
                    pipelineBuildMap = pipelineBuildMap,
                    pipelineInfos = pipelineInfos
                )
            }
            Page(page = page, pageSize = pageSize, count = totalCount, records = pipelineInfos)
        } else {
            Page(page = page, pageSize = pageSize, count = totalCount, records = emptyList())
        }
    }

    private fun generateArchivedPipelineInfo(
        pipelineRecord: TPipelineInfoRecord,
        pipelineBuildSummaryMap: Map<String, TPipelineBuildSummaryRecord>,
        pipelineBuildMap: Map<String, BuildInfo>,
        pipelineInfos: MutableList<PipelineInfo>
    ) {
        val pipelineInfo = pipelineInfoDao.convert(pipelineRecord, null)!!
        val pipelineBuildSummaryRecord = pipelineBuildSummaryMap[pipelineRecord.pipelineId]
        val buildStatusOrd = pipelineBuildSummaryRecord?.latestStatus
        val pipelineBuildStatus = pipelineStatusService.getBuildStatus(buildStatusOrd)
        pipelineInfo.latestBuildStartTime = (pipelineBuildSummaryRecord?.latestStartTime)?.timestampmilli() ?: 0
        pipelineInfo.latestBuildEndTime = (pipelineBuildSummaryRecord?.latestEndTime)?.timestampmilli() ?: 0
        pipelineInfo.latestBuildStatus = pipelineBuildStatus
        pipelineInfo.latestBuildNum = pipelineBuildSummaryRecord?.buildNum
        pipelineInfo.latestBuildId = pipelineBuildSummaryRecord?.latestBuildId
        val pipelineBuildHistoryRecord = pipelineBuildMap[pipelineRecord.pipelineId]
        pipelineInfo.trigger = pipelineBuildHistoryRecord?.trigger
        pipelineBuildHistoryRecord?.let {
            pipelineInfo.lastBuildMsg = BuildMsgUtils.getBuildMsg(
                buildMsg = pipelineBuildHistoryRecord.buildMsg,
                startType = StartType.toStartType(pipelineBuildHistoryRecord.trigger),
                channelCode = pipelineBuildHistoryRecord.channelCode
            )
        }
        pipelineInfos.add(pipelineInfo)
    }
}
