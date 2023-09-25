package com.tencent.devops.remotedev.service.gitproxy

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.devops.remotedev.pojo.gitproxy.CreateGitProxyData
import com.tencent.devops.remotedev.pojo.gitproxy.FetchRepoResp
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitProxyService @Autowired constructor(
    private val gitproxyBkRepoClient: GitproxyBkRepoClient
) {
    fun createRepo(
        userId: String,
        data: CreateGitProxyData
    ): Boolean {
        gitproxyBkRepoClient.createRepo(
            userId = userId,
            projectId = data.projectId,
            repoName = data.repoName,
            url = data.url,
            desc = data.desc
        )
        return true
    }

    fun fetchRepo(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int
    ): Page<FetchRepoResp> {
        val repos = gitproxyBkRepoClient.fetchRepo(userId, projectId, page, pageSize)
        val resp = repos.records.map { record ->
            FetchRepoResp(
                url = record.configuration.proxy.url,
                proxyUrl = record.configuration.url,
                creator = record.createdBy,
                createdDate = record.createdDate,
                repoName = record.name
            )
        }
        return Page(
            pageNumber = repos.pageNumber,
            pageSize = repos.pageSize,
            totalRecords = repos.totalRecords,
            records = resp
        )
    }

    fun deleteRepo(
        userId: String,
        projectId: String,
        repoName: String
    ): Boolean {
        gitproxyBkRepoClient.deleteRepo(userId, projectId, repoName)
        return true
    }
}
