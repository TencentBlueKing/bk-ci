package com.tencent.devops.gitci.v2.service

import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.v2.dao.GitCIBasicSettingDao
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.project.pojo.app.AppProjectVO
import com.tencent.devops.project.pojo.enums.ProjectSourceEnum
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
            search = searchName
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

    // 返回GITCI保存的流水线ID
    fun getGitCIPipelines(
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType?,
        search: String?
    ): Pagination<String> {
        val limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val pipelines = pipelineResourceDao.getAppDataByGitProjectId(
            dslContext = dslContext,
            gitProjectId = if (projectId.trim().startsWith("git")) {
                projectId.removePrefix("git_").toLong()
            } else {
                projectId.toLong()
            },
            keyword = search,
            offset = limit.offset,
            limit = limit.limit,
            orderBy = sortType ?: PipelineSortType.CREATE_TIME
        )
        if (pipelines.isNullOrEmpty()) {
            return Pagination(false, emptyList())
        }
        val hasNext = pipelines.size == pageSize
        return Pagination(hasNext, pipelines.map { it.pipelineId })
    }
}
