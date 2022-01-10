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

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryDownLoadResource
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.FileInfoPage
import com.tencent.devops.artifactory.pojo.Property
import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.pojo.GitCIBuildHistory
import com.tencent.devops.stream.pojo.GitCIModelDetail
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.StreamBasicSettingService
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.user.TXUserReportResource
import com.tencent.devops.process.pojo.Report
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class GitCIDetailService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val repositoryConfService: GitRepositoryConfService,
    private val pipelineResourceDao: GitPipelineResourceDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCIDetailService::class.java)
    }

    @Value("\${gateway.reportPrefix}")
    private lateinit var reportPrefix: String

    private val channelCode = ChannelCode.GIT

    fun getProjectLatestBuildDetail(userId: String, gitProjectId: Long, pipelineId: String?): GitCIModelDetail? {
        val conf = streamBasicSettingService.getGitCIConf(gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启Stream，无法查询"
        )
        val eventBuildRecord = gitRequestEventBuildDao.getLatestBuild(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId
        ) ?: return null
        val eventRecord = gitRequestEventDao.get(dslContext, eventBuildRecord.eventId) ?: return null
        // 如果是来自fork库的分支，单独标识
        val realEvent = GitCommonUtils.checkAndGetForkBranch(eventRecord, client)
        val modelDetail = client.get(ServiceBuildResource::class).getBuildDetail(
            userId = userId,
            projectId = conf.projectCode!!,
            pipelineId = eventBuildRecord.pipelineId,
            buildId = eventBuildRecord.buildId,
            channelCode = channelCode
        ).data!!
        val pipeline = getPipelineWithId(userId, gitProjectId, eventBuildRecord.pipelineId)
        return GitCIModelDetail(pipeline, realEvent, modelDetail)
    }

    fun getBuildDetail(userId: String, gitProjectId: Long, buildId: String): GitCIModelDetail? {
        val conf = streamBasicSettingService.getGitCIConf(gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启Stream，无法查询"
        )
        val eventBuildRecord = gitRequestEventBuildDao.getByBuildId(dslContext, buildId) ?: return null
        val eventRecord = gitRequestEventDao.get(dslContext, eventBuildRecord.eventId) ?: return null
        // 如果是来自fork库的分支，单独标识
        val realEvent = GitCommonUtils.checkAndGetForkBranch(eventRecord, client)
        val modelDetail = client.get(ServiceBuildResource::class).getBuildDetail(
            userId = userId,
            projectId = conf.projectCode!!,
            pipelineId = eventBuildRecord.pipelineId,
            buildId = buildId,
            channelCode = channelCode
        ).data!!
        val pipeline = getPipelineWithId(userId, gitProjectId, eventBuildRecord.pipelineId)
        return GitCIModelDetail(pipeline, realEvent, modelDetail)
    }

    fun batchGetBuildHistory(
        userId: String,
        gitProjectId: Long,
        buildIds: List<String>
    ): Map<String, GitCIBuildHistory> {
        val conf = streamBasicSettingService.getGitCIConf(gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启Stream，无法查询"
        )
        val history = client.get(ServiceBuildResource::class).getBatchBuildStatus(
            projectId = conf.projectCode!!,
            buildId = buildIds.toSet(),
            channelCode = channelCode
        ).data!!
        val infoMap = mutableMapOf<String, GitCIBuildHistory>()
        history.forEach {
            val buildRecord = gitRequestEventBuildDao.getByBuildId(dslContext, it.id) ?: return@forEach
            val eventRecord = gitRequestEventDao.get(dslContext, buildRecord.eventId) ?: return@forEach
            // 如果是来自fork库的分支，单独标识
            val realEvent = GitCommonUtils.checkAndGetForkBranch(eventRecord, client)
            val pipeline = pipelineResourceDao.getPipelineById(
                dslContext = dslContext,
                gitProjectId = gitProjectId,
                pipelineId = buildRecord.pipelineId
            ) ?: return@forEach
            infoMap[it.id] = GitCIBuildHistory(
                displayName = pipeline.displayName,
                pipelineId = pipeline.pipelineId,
                gitRequestEvent = realEvent,
                buildHistory = it
            )
        }
        return infoMap
    }

    fun search(
        userId: String,
        gitProjectId: Long,
        pipelineId: String,
        buildId: String,
        page: Int?,
        pageSize: Int?
    ): FileInfoPage<FileInfo> {
        val conf = streamBasicSettingService.getGitCIConf(gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启Stream，无法查询"
        )

        val propMap = HashMap<String, String>()
        propMap["pipelineId"] = pipelineId
        propMap["buildId"] = buildId
        // val searchProps = SearchProps(emptyList(), propMap)

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
        val conf = streamBasicSettingService.getGitCIConf(gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启Stream，无法查询"
        )

        // 校验工蜂项目权限
        val checkAuth = client.getScm(ServiceGitCiResource::class).checkUserGitAuth(gitUserId, gitProjectId.toString())
        if (!checkAuth.data!!) {
            throw CustomException(Response.Status.FORBIDDEN, "用户没有工蜂项目权限，无法获取下载链接")
        }

        try {
            val url = client.get(ServiceArtifactoryDownLoadResource::class).downloadIndexUrl(
                projectId = conf.projectCode!!,
                artifactoryType = artifactoryType,
                userId = userId,
                path = path,
                ttl = 10,
                directed = true
            ).data!!
            return Url(getUrl(url.url)!!, getUrl(url.url2))
        } catch (e: Exception) {
            logger.error("Artifactory download url failed. ${e.message}")
            throw CustomException(Response.Status.BAD_REQUEST, "Artifactory download url failed. ${e.message}")
        }
    }

    private fun getUrl(url: String?): String? {
        if (url == null) {
            return url
        }
        // 没有被替换掉域名的url
        return if (!url.startsWith("/")) {
            url
        } else {
            reportPrefix + url
        }
    }

    fun getReports(userId: String, gitProjectId: Long, pipelineId: String, buildId: String): List<Report> {
        val conf = streamBasicSettingService.getGitCIConf(gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            "项目未开启Stream，无法查询"
        )
        val reportList = client.get(TXUserReportResource::class)
            .getGitCI(userId, conf.projectCode!!, pipelineId, buildId)
            .data!!.toMutableList()
        // 更换域名来支持工蜂的页面
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
    ): GitProjectPipeline? {
        logger.info("get pipeline with pipelineId: $pipelineId, gitProjectId: $gitProjectId")
        val conf = streamBasicSettingService.getGitCIConf(gitProjectId)
        if (conf == null) {
            repositoryConfService.initGitCISetting(userId, gitProjectId)
            return null
        }
        val pipeline = pipelineResourceDao.getPipelineById(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId
        ) ?: return null

        return GitProjectPipeline(
            gitProjectId = gitProjectId,
            pipelineId = pipeline.pipelineId,
            filePath = pipeline.filePath,
            displayName = pipeline.displayName,
            enabled = pipeline.enabled,
            creator = pipeline.creator,
            latestBuildInfo = null,
            latestBuildBranch = null
        )
    }
}
