package com.tencent.devops.remotedev.service.gitproxy

import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.config.TGitConfig
import com.tencent.devops.remotedev.dao.ProjectTGitLinkDao
import com.tencent.devops.remotedev.pojo.TGitRepoDaoData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitBindRemotedevData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitCredType
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoStatus
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
    private val tGitConfig: TGitConfig,
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

    fun bindTGitProject(userId: String, data: TGitBindRemotedevData): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        data.projectIds.forEach { projectId ->
            try {
                // 入库
                projectTGitLinkDao.batchAdd(
                    dslContext = dslContext,
                    projectId = projectId,
                    data = listOf(
                        TGitRepoDaoData(
                            tgitId = data.tgitId,
                            status = TGitRepoStatus.TO_BE_MIGRATED,
                            oauthUser = userId,
                            gitType = data.projectType.name,
                            url = data.tgitUrl,
                            cred = userId,
                            credType = TGitCredType.OAUTH_USER
                        )
                    )
                )
                val res = gitProxyTGitService.linkTGit(projectId, setOf(data.tgitId)).values.firstOrNull() ?: false
                result[projectId] = res
            } catch (e: Exception) {
                logger.error("bindTGitProject ${data.tgitId}|$projectId bind error", e)
                result[projectId] = false
            }
        }
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TGitService::class.java)
    }
}