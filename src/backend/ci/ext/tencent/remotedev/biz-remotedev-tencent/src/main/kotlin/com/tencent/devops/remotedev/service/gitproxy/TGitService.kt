package com.tencent.devops.remotedev.service.gitproxy

import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.dao.ProjectTGitLinkDao
import com.tencent.devops.repository.api.ServiceOauthResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 工蜂获取云研发相关信息接口
 */
@Service
class TGitService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val projectTGitLinkDao: ProjectTGitLinkDao,
    private val gitProxyTGitService: GitProxyTGitService
) {
    fun checkUserOauthToken(userId: String): Boolean {
        client.get(ServiceOauthResource::class).tGitGet(userId).data ?: return false
        return true
    }

    fun fetchProject(tGitId: Long): List<String> {
        return projectTGitLinkDao.fetchByTGitId(dslContext, tGitId, null).map { it.projectId }
    }

    fun bindTGitProject(tGitId: Long, projectIds: List<String>): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        projectIds.forEach { projectId ->
            try {
                val res = gitProxyTGitService.linkTGit(projectId, setOf(tGitId)).values.firstOrNull() ?: false
                result[projectId] = res
            } catch (e: Exception) {
                logger.error("bindTGitProject $tGitId|$projectId bind error", e)
                result[projectId] = false
            }
        }
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TGitService::class.java)
    }
}