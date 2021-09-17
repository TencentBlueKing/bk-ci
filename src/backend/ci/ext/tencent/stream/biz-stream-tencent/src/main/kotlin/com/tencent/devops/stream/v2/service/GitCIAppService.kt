package com.tencent.devops.stream.v2.service

import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.v2.dao.GitCIBasicSettingDao
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.project.pojo.app.AppProjectVO
import com.tencent.devops.project.pojo.enums.ProjectSourceEnum
import com.tencent.devops.stream.pojo.GitProjectPipeline
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@SuppressWarnings("LongParameterList")
class GitCIAppService @Autowired constructor(
    private val dslContext: DSLContext,
    private val scmService: ScmService,
    private val gitCIBasicSettingDao: GitCIBasicSettingDao,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val oauthService: OauthService
) {

    fun getGitCIProjectList(
        userId: String,
        page: Int,
        pageSize: Int,
        searchName: String?
    ): Pagination<AppProjectVO> {
        val token = oauthService.getAndCheckOauthToken(userId).accessToken
        val projectIdMap = scmService.getProjectList(
            accessToken = token,
            userId = userId,
            page = page,
            pageSize = pageSize,
            search = searchName,
            orderBy = null, sort = null, owned = null, minAccessLevel = null
        )?.associateBy { it.id!! }?.toMap()
        val hasNext = projectIdMap?.values?.size == pageSize
        if (projectIdMap.isNullOrEmpty()) {
            return Pagination(false, emptyList())
        }
        val projects = gitCIBasicSettingDao.searchProjectByIds(dslContext, projectIdMap.keys)
        val result = projects.map {
            AppProjectVO(
                projectCode = it.projectCode,
                projectName = it.name,
                logoUrl = projectIdMap[it.id]?.avatarUrl,
                projectSource = ProjectSourceEnum.GIT_CI.id
            )
        }
        return Pagination(hasNext, result)
    }

    // 返回GITCI保存的流水线
    fun getGitCIPipelines(
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType?,
        search: String?
    ): Pagination<GitProjectPipeline> {
        val limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val gitProjectId = getGitProjectId(projectId)
        val pipelines = pipelineResourceDao.getAppDataByGitProjectId(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            keyword = search,
            offset = limit.offset,
            limit = limit.limit,
            orderBy = sortType ?: PipelineSortType.CREATE_TIME
        )
        if (pipelines.isNullOrEmpty()) {
            return Pagination(false, emptyList())
        }
        return Pagination(
            pipelines.size == pageSize,
            pipelines.map {
                GitProjectPipeline(
                    gitProjectId = gitProjectId,
                    pipelineId = it.pipelineId,
                    filePath = it.filePath,
                    displayName = it.displayName,
                    enabled = it.enabled,
                    creator = it.creator,
                    latestBuildInfo = null
                )
            }
        )
    }

    fun getGitCIPipeline(
        projectId: String,
        pipelineId: String
    ): GitProjectPipeline? {
        val gitProjectId = getGitProjectId(projectId)
        val pipeline = pipelineResourceDao.getPipelineById(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId
        ) ?: return null
        with(pipeline) {
            return GitProjectPipeline(
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                filePath = filePath,
                displayName = displayName,
                enabled = enabled,
                creator = creator,
                latestBuildInfo = null
            )
        }
    }

    private fun getGitProjectId(projectId: String): Long {
        return if (projectId.trim().startsWith("git")) {
            projectId.removePrefix("git_").toLong()
        } else {
            projectId.toLong()
        }
    }
}
