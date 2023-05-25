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

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.user.UserReportResource
import com.tencent.devops.process.pojo.Report
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.pojo.StreamGitProjectPipeline
import com.tencent.devops.stream.pojo.StreamGitRequestEventReq
import com.tencent.devops.stream.pojo.StreamModelDetail
import com.tencent.devops.stream.util.GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class StreamDetailService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val streamGitProjectInfoCache: StreamGitProjectInfoCache,
    private val streamGitConfig: StreamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamDetailService::class.java)
    }

    @Value("\${gateway.reportPrefix}")
    private lateinit var reportPrefix: String

    private val channelCode = ChannelCode.GIT

    fun getProjectLatestBuildDetail(userId: String, gitProjectId: Long, pipelineId: String?): StreamModelDetail? {
        val conf = streamBasicSettingService.getStreamBasicSettingAndCheck(gitProjectId)
        val eventBuildRecord =
            gitRequestEventBuildDao.getLatestBuild(dslContext, gitProjectId, pipelineId) ?: return null
        val eventRecord = gitRequestEventDao.get(
            dslContext = dslContext,
            id = eventBuildRecord.eventId,
            scmType = streamGitConfig.getScmType()
        ) ?: return null
        // 如果是来自fork库的分支，单独标识
        val pathWithNamespace = eventRecord.sourceGitProjectId?.let {
            streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                gitProjectId = it,
                useAccessToken = true,
                userId = conf.enableUserId
            )
        }?.pathWithNamespace
        val realEvent = GitCommonUtils.checkAndGetForkBranch(eventRecord, pathWithNamespace)

        val modelDetail = client.get(ServiceBuildResource::class).getBuildRecordByExecuteCount(
            userId = userId,
            projectId = conf.projectCode!!,
            pipelineId = eventBuildRecord.pipelineId,
            buildId = eventBuildRecord.buildId,
            executeCount = null,
            channelCode = channelCode
        ).data!!
        val pipeline = getPipelineWithId(userId, gitProjectId, eventBuildRecord.pipelineId)
        return StreamModelDetail(pipeline, StreamGitRequestEventReq(realEvent, conf.homepage), modelDetail)
    }

    fun getBuildDetail(userId: String, gitProjectId: Long, buildId: String): StreamModelDetail? {
        val conf = streamBasicSettingService.getStreamBasicSettingAndCheck(gitProjectId)
        val eventBuildRecord = gitRequestEventBuildDao.getByBuildId(dslContext, buildId) ?: return null
        val eventRecord = gitRequestEventDao.get(
            dslContext = dslContext,
            id = eventBuildRecord.eventId,
            scmType = streamGitConfig.getScmType()
        ) ?: return null
        // 如果是来自fork库的分支，单独标识
        val pathWithNamespace = eventRecord.sourceGitProjectId?.let {
            streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                gitProjectId = it,
                useAccessToken = true,
                userId = conf.enableUserId
            )
        }?.pathWithNamespace
        val realEvent = GitCommonUtils.checkAndGetForkBranch(eventRecord, pathWithNamespace)

        val modelDetail = client.get(ServiceBuildResource::class).getBuildRecordByExecuteCount(
            userId = userId,
            projectId = conf.projectCode!!,
            pipelineId = eventBuildRecord.pipelineId,
            buildId = buildId,
            executeCount = null,
            channelCode = channelCode
        ).data!!
        val pipeline = getPipelineWithId(userId, gitProjectId, eventBuildRecord.pipelineId)
        // 获取备注信息
        val remark = client.get(ServiceBuildResource::class)
            .getBatchBuildStatus(conf.projectCode!!, setOf(buildId), channelCode).data?.first()?.remark
        return StreamModelDetail(
            gitProjectPipeline = pipeline,
            gitRequestEvent = StreamGitRequestEventReq(realEvent, conf.homepage),
            modelDetail = modelDetail,
            buildHistoryRemark = remark
        )
    }

    fun buildTriggerReview(
        userId: String,
        gitProjectId: Long,
        buildId: String,
        approve: Boolean
    ): Boolean {
        val conf = streamBasicSettingService.getStreamBasicSettingAndCheck(gitProjectId)
        val eventBuildRecord = gitRequestEventBuildDao.getByBuildId(dslContext, buildId) ?: return false
        return client.get(ServiceBuildResource::class).buildTriggerReview(
            userId = userId,
            projectId = conf.projectCode!!,
            pipelineId = eventBuildRecord.pipelineId,
            buildId = buildId,
            approve = approve,
            channelCode = channelCode
        ).data!!
    }

    fun search(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        buildId: String,
        page: Int?,
        pageSize: Int?
    ): FileInfoPage<FileInfo> {
        val conf = streamBasicSettingService.getStreamBasicSettingAndCheck(gitProjectId)
        val prop = listOf(Property("pipelineId", pipelineId), Property("buildId", buildId))
        return client.get(ServiceArtifactoryResource::class).search(
            userId = userId,
            projectId = conf.projectCode!!,
            page = page,
            pageSize = pageSize,
            searchProps = prop
        ).data!!
    }

    fun downloadUrl(
        userId: String,
        gitUserId: String,
        gitProjectId: Long,
        artifactoryType: ArtifactoryType,
        path: String
    ): Url {
        TODO("开源版根据用户需求自由实现")
    }

    fun getReports(userId: String, gitProjectId: Long, pipelineId: String, buildId: String): List<Report> {
        val conf = streamBasicSettingService.getStreamBasicSettingAndCheck(gitProjectId)
        val reportList = client.get(UserReportResource::class)
            .getStream(userId, conf.projectCode!!, pipelineId, buildId)
            .data!!.toMutableList()
        // 更换域名来支持stream 的页面
        reportList.forEachIndexed { index, report ->
            if (report.type == ReportTypeEnum.INTERNAL.name) {
                reportList[index] = report.copy(
                    indexFileUrl = reportPrefix + report.indexFileUrl
                )
            }
        }
        return reportList.toList()
    }

    fun getPipelineWithId(
        userId: String,
        gitProjectId: Long,
        pipelineId: String
    ): StreamGitProjectPipeline? {
        logger.info("StreamDetailService|getPipelineWithId|pipelineId|$pipelineId|gitProjectId|$gitProjectId")
        val pipeline = pipelineResourceDao.getPipelineById(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId
        ) ?: return null

        return StreamGitProjectPipeline(
            gitProjectId = gitProjectId,
            pipelineId = pipeline.pipelineId,
            filePath = pipeline.filePath,
            displayName = pipeline.displayName,
            enabled = pipeline.enabled,
            creator = pipeline.creator,
            latestBuildBranch = null
        )
    }
}
