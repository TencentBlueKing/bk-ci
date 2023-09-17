package com.tencent.devops.remotedev.service.gitproxy

import com.tencent.devops.remotedev.pojo.gitproxy.CreateGitProxyData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitProxyService @Autowired constructor(
    private val bkRepoClient: BkRepoClient
) {
    fun createRepo(
        userId: String,
        data: CreateGitProxyData
    ): Boolean {
        val repoName = data.url.removeSuffix(".git").split("/").last()
        bkRepoClient.createRepo(userId, data.projectId, repoName, data.url)
        return true
    }

    fun fetchRepo(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int
    ): Map<String, String> {
        // TODO: 目前不知道返回类型
        return bkRepoClient.fetchRepo(userId, projectId, page, pageSize)
    }

    fun deleteRepo(
        userId: String,
        projectId: String,
        repoName: String
    ): Boolean {
        bkRepoClient.deleteRepo(userId, projectId, repoName)
        return true
    }
}
