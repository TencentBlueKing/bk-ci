package com.tencent.devops.remotedev.service.gitproxy

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.pojo.gitproxy.CreateGitProxyData
import com.tencent.devops.remotedev.pojo.gitproxy.FetchRepoResp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitProxyService @Autowired constructor(
    private val gitproxyBkRepoClient: GitproxyBkRepoClient,
    private val redisOperation: RedisOperation
) {
    @ActionAuditRecord(
        actionId = ActionId.CODE_PROXY_CREATE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CODE_PROXY,
            instanceNames = "#data?.repoName",
            instanceIds = "#data?.repoName"
        ),
        content = ActionAuditContent.CODE_PROXY_CREATE_CONTENT
    )
    fun createRepo(
        userId: String,
        data: CreateGitProxyData
    ): Boolean {
        // 判断项目是否存在，不存在创建
        if (!ifExistBkRepoProject(userId, data.projectId)) {
            gitproxyBkRepoClient.createProject(userId, data.projectId)
            redisOperation.set("REDIS_BKREPO_PROJECT:${data.projectId}", "", 10 * 60)
        }
        gitproxyBkRepoClient.createRepo(
            userId = userId,
            projectId = data.projectId,
            repoName = data.repoName,
            url = data.url,
            desc = data.desc,
            gitType = data.gitType
        )
        return true
    }

    private fun ifExistBkRepoProject(userId: String, projectId: String): Boolean {
        if (redisOperation.get("REDIS_BKREPO_PROJECT:$projectId") != null) {
            return true
        }
        if (gitproxyBkRepoClient.existProject(userId, projectId) == true) {
            redisOperation.set("REDIS_BKREPO_PROJECT:$projectId", "", 10 * 60)
            return true
        }
        return false
    }

    fun fetchRepo(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int,
        gitType: ScmType?
    ): Page<FetchRepoResp> {
        val repos = gitproxyBkRepoClient.fetchRepo(userId, projectId, page, pageSize, gitType)
        val resp = repos.records.map { record ->
            FetchRepoResp(
                url = record.configuration.proxy.url,
                proxyUrl = record.configuration.url,
                creator = record.createdBy,
                createdDate = record.createdDate,
                repoName = record.name,
                type = record.type,
                desc = record.description
            )
        }
        return Page(
            pageNumber = repos.pageNumber,
            pageSize = repos.pageSize,
            totalRecords = repos.totalRecords,
            records = resp
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.CODE_PROXY_DELETE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.CODE_PROXY,
            instanceNames = "#repoName",
            instanceIds = "#repoName"
        ),
        content = ActionAuditContent.CODE_PROXY_DELETE_CONTENT
    )
    fun deleteRepo(
        userId: String,
        projectId: String,
        repoName: String
    ): Boolean {
        gitproxyBkRepoClient.deleteRepo(userId, projectId, repoName)
        return true
    }

    companion object {
        // bkrepo project 缓存
        const val REDIS_BKREPO_PROJECT = "remotedev:bkrepo:existProject"
    }
}
