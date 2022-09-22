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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.service.StreamScmService
import com.tencent.devops.stream.v1.components.V1StreamGitProjectInfoCache
import com.tencent.devops.stream.v1.dao.V1GitPipelineResourceDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventBuildDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventDao
import com.tencent.devops.stream.v1.pojo.V1GitCIBuildBranch
import com.tencent.devops.stream.v1.pojo.V1GitCIBuildHistory
import com.tencent.devops.stream.v1.pojo.V1GitRequestEventReq
import com.tencent.devops.stream.v1.utils.V1GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class V1GitCIHistoryService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitRequestEventBuildDao: V1GitRequestEventBuildDao,
    private val gitRequestEventDao: V1GitRequestEventDao,
    private val streamBasicSettingService: V1StreamBasicSettingService,
    private val repositoryConfService: V1GitRepositoryConfService,
    private val pipelineResourceDao: V1GitPipelineResourceDao,
    private val streamGitProjectInfoCache: V1StreamGitProjectInfoCache,
    private val streamScmService: StreamScmService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(V1GitCIHistoryService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getHistoryBuildList(
        userId: String,
        gitProjectId: Long,
        startBeginTime: String?,
        endBeginTime: String?,
        page: Int?,
        pageSize: Int?,
        branch: String?,
        sourceGitProjectId: Long?,
        triggerUser: String?,
        pipelineId: String?,
        commitMsg: String? = null,
        event: StreamGitObjectKind? = null,
        status: BuildStatus? = null
    ): Page<V1GitCIBuildHistory> {
        logger.info("get history build list, gitProjectId: $gitProjectId")
        // 校验查询时间范围跨度
        validateQueryTimeRange(startBeginTime, endBeginTime)
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20

        val conf = streamBasicSettingService.getGitCIConf(gitProjectId)
        if (conf == null) {
            repositoryConfService.initGitCISetting(userId, gitProjectId)
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0,
                records = emptyList()
            )
        }

        val totalPage = gitRequestEventBuildDao.getRequestEventBuildListCount(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            branchName = branch,
            sourceGitProjectId = sourceGitProjectId,
            triggerUser = triggerUser,
            pipelineId = pipelineId,
            event = event?.value
        )
        if (totalPage == 0) {
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0,
                records = emptyList()
            )
        }

        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page = pageNotNull, pageSize = pageSizeNotNull)
        val gitRequestBuildList = gitRequestEventBuildDao.getRequestEventBuildList(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            branchName = branch,
            sourceGitProjectId = sourceGitProjectId,
            triggerUser = triggerUser,
            pipelineId = pipelineId,
            event = event?.value,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        )
        val builds = gitRequestBuildList.map { it.buildId }.toSet()
        logger.info("get history build list, build ids: $builds")
        val buildHistoryList = client.get(ServiceBuildResource::class).getBatchBuildStatus(
            projectId = conf.projectCode!!,
            buildId = builds,
            channelCode = channelCode,
            startBeginTime = startBeginTime,
            endBeginTime = endBeginTime
        ).data
        if (null == buildHistoryList) {
            logger.info("Get branch build history list return empty, gitProjectId: $gitProjectId")
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0,
                records = emptyList()
            )
        }

        val records = mutableListOf<V1GitCIBuildHistory>()
        gitRequestBuildList.forEach {
            val buildHistory = getBuildHistory(
                buildId = it.buildId,
                buildHistoryList = buildHistoryList,
                status = status
            ) ?: return@forEach
            val gitRequestEvent = gitRequestEventDao.get(dslContext, it.eventId, commitMsg) ?: return@forEach
            // 如果是来自fork库的分支，单独标识
            val gitProjectInfoCache = gitRequestEvent.sourceGitProjectId?.let {
                lazy {
                    streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                        gitProjectId = it,
                        useAccessToken = true,
                        getProjectInfo = streamScmService::getProjectInfoRetry
                    )
                }
            }
            val realEvent = V1GitCommonUtils.checkAndGetForkBranch(gitRequestEvent, gitProjectInfoCache)
            val pipeline =
                pipelineResourceDao.getPipelineById(dslContext, gitProjectId, it.pipelineId) ?: return@forEach
            records.add(
                V1GitCIBuildHistory(
                    displayName = pipeline.displayName,
                    pipelineId = pipeline.pipelineId,
                    gitRequestEvent = V1GitRequestEventReq(realEvent),
                    buildHistory = buildHistory
                )
            )
        }
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = totalPage.toLong(),
            records = records
        )
    }

    fun getAllBuildBranchList(
        userId: String,
        gitProjectId: Long,
        page: Int?,
        pageSize: Int?,
        keyword: String?
    ): Page<V1GitCIBuildBranch> {
        logger.info("get all branch build list, gitProjectId: $gitProjectId")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        streamBasicSettingService.getGitCIConf(gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启Stream，无法查询"
        )
        val buildBranchList = gitRequestEventBuildDao.getAllBuildBranchList(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            page = null,
            pageSize = null,
            keyword = keyword
        )
        if (buildBranchList.isEmpty()) {
            logger.info("Get build branch list return empty, gitProjectId: $gitProjectId")
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0,
                records = emptyList()
            )
        }
        // 因为涉及到分组，selectCount无法拿到具体条数，所以拿出来全部查询自己分页
        val firstIndex = (pageNotNull - 1) * pageSizeNotNull
        val lastIndex = if (pageNotNull * pageSizeNotNull > buildBranchList.size) {
            buildBranchList.size
        } else {
            pageNotNull * pageSizeNotNull
        }
        // 如果是来自fork库的分支，单独标识
        val records = buildBranchList.subList(firstIndex, lastIndex).map {
            val gitProjectInfoCache = it.sourceGitProjectId?.let { id ->
                lazy {
                    streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                        gitProjectId = id,
                        useAccessToken = true,
                        getProjectInfo = streamScmService::getProjectInfoRetry
                    )
                }
            }
            V1GitCIBuildBranch(
                branchName = V1GitCommonUtils.checkAndGetForkBranchName(
                    gitProjectId = it.gitProjectId,
                    sourceGitProjectId = it.sourceGitProjectId,
                    branch = it.branch,
                    gitProjectCache = gitProjectInfoCache
                ),
                gitProjectId = it.gitProjectId,
                sourceGitProjectId = it.sourceGitProjectId
            )
        }
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = buildBranchList.size.toLong(),
            records = records
        )
    }

    private fun getBuildHistory(
        buildId: String,
        buildHistoryList: List<BuildHistory>,
        status: BuildStatus?
    ): BuildHistory? {
        buildHistoryList.forEach { build ->
            if (build.id != buildId) {
                return@forEach
            }
            if (status != null) {
                if (build.status != status.name) {
                    return@forEach
                }
            }
            return build
        }
        return null
    }

    private fun validateQueryTimeRange(
        startUpdateTime: String?,
        endUpdateTime: String?
    ) {
        if (startUpdateTime.isNullOrBlank() || endUpdateTime.isNullOrBlank()) return
        val convertStartUpdateTime = DateTimeUtil.stringToLocalDateTime(startUpdateTime)
        val convertEndUpdateTime = DateTimeUtil.stringToLocalDateTime(endUpdateTime)
        if (convertStartUpdateTime.isAfter(convertEndUpdateTime)) {
            // 超过查询时间范围则报错
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_QUERY_TIME_RANGE_ERROR,
                defaultMessage = "查询的时间范围跨度错误"
            )
        }
    }
}
