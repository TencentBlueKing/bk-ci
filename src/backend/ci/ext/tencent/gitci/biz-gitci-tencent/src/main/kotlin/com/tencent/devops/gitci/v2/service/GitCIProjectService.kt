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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.pojo.enums.GitCIProjectType
import com.tencent.devops.gitci.v2.dao.GitCIBasicSettingDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.common.ci.OBJECT_KIND_MERGE_REQUEST
import com.tencent.devops.common.ci.OBJECT_KIND_TAG_PUSH
import com.tencent.devops.common.client.Client
import com.tencent.devops.gitci.dao.GitRequestEventDao
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.git.GitTagPushEvent
import com.tencent.devops.gitci.pojo.v2.project.CIInfo
import com.tencent.devops.gitci.pojo.v2.project.ProjectCIInfo
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.scm.pojo.GitCodeProjectsOrder
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

@Service
class GitCIProjectService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val scmService: ScmService,
    private val oauthService: OauthService,
    private val gitCIBasicSettingDao: GitCIBasicSettingDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIProjectService::class.java)
    }

    fun getProjectList(
        userId: String,
        type: GitCIProjectType?,
        search: String?,
        page: Int?,
        pageSize: Int?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?
    ): Pagination<ProjectCIInfo> {
        val realPage = if (page == null || page <= 0) {
            1
        } else {
            page
        }
        val realPageSize = if (pageSize == null || pageSize <= 0) {
            10
        } else {
            pageSize
        }
        val token = oauthService.getAndCheckOauthToken(userId).accessToken
        val gitProjects = scmService.getProjectList(
            accessToken = token,
            userId = userId,
            page = realPage,
            pageSize = realPageSize,
            search = search,
            orderBy = orderBy ?: GitCodeProjectsOrder.UPDATE,
            sort = sort ?: GitCodeBranchesSort.DESC,
            owned = null,
            minAccessLevel = if (type == GitCIProjectType.MY_PROJECT) {
                GitAccessLevelEnum.DEVELOPER
            } else {
                null
            }
        )
        if (gitProjects.isNullOrEmpty()) {
            return Pagination(false, emptyList())
        }
        val projectIdMap = gitCIBasicSettingDao.searchProjectByIds(
            dslContext = dslContext,
            projectIds = gitProjects.map { it.id!! }.toSet()
        ).associateBy { it.id }
        val lastBuildMap =
            gitRequestEventBuildDao.lastBuildByProject(dslContext, projectIdMap.keys).associateBy { it.gitProjectId }
        val eventMap = gitRequestEventDao.getRequestsById(
            dslContext = dslContext,
            requestIds = lastBuildMap.values.map { it.eventId.toInt() }.toSet()
        ).associateBy { it.id }
        val result = gitProjects.map {
            val project = projectIdMap[it.id]
            ProjectCIInfo(
                id = it.id!!,
                projectCode = project?.projectCode,
                public = it.public,
                name = it.name,
                nameWithNamespace = it.nameWithNamespace,
                httpsUrlToRepo = it.httpsUrlToRepo,
                webUrl = it.webUrl,
                avatarUrl = it.avatarUrl,
                description = it.description,
                ciInfo = CIInfo(
                    enableCI = project?.enableCi ?: false,
                    lastBuildMessage = getEventMessage(
                        event = eventMap[lastBuildMap[it.id!!]?.eventId],
                        gitProjectId = it.id!!
                    ),
                    lastBuildStatus = lastBuildMap[it.id!!]?.buildStatus,
                    lastBuildPipelineId = lastBuildMap[it.id!!]?.pipelineId,
                    lastBuildId = lastBuildMap[it.id!!]?.buildId
                )
            )
        }
        return Pagination(
            hasNext = gitProjects.size == realPageSize,
            records = result
        )
    }

    private fun getEventMessage(event: GitRequestEvent?, gitProjectId: Long): String? {
        if (event == null) {
            return null
        }
        val messageTitle = when (event.objectKind) {
            OBJECT_KIND_MERGE_REQUEST -> {
                val branch = GitCommonUtils.checkAndGetForkBranchName(
                    gitProjectId = gitProjectId,
                    sourceGitProjectId = event.sourceGitProjectId,
                    branch = event.branch,
                    client = client
                )
                "[$branch] Merge requests [!${event.mergeRequestId}] ${event.extensionAction} by ${event.userId}"
            }
            OBJECT_KIND_MANUAL -> {
                "[${event.branch}] Manual Triggered by ${event.userId}"
            }
            OBJECT_KIND_TAG_PUSH -> {
                val eventMap = try {
                    objectMapper.readValue<GitTagPushEvent>(event.event)
                } catch (e: Exception) {
                    logger.error("event as GitTagPushEvent error ${e.message}")
                    null
                }
                "[${eventMap?.create_from}] Tag [${event.branch}] pushed by ${event.userId}"
            }
            else -> {
                "[${event.branch}] Commit [${event.commitId.subSequence(0, 7)}] pushed by ${event.userId}"
            }
        }
        return messageTitle
    }
}
