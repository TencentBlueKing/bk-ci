package com.tencent.devops.repository.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.client.Client
import com.tencent.devops.external.api.ServiceGithubResource
import com.tencent.devops.repository.pojo.Project
import com.tencent.devops.repository.service.IGithubService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxGithubService @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper
): IGithubService {

    override fun getGithubOauth(projectId: String, userId: String, repoHashId: String?): String {
        return client.get(ServiceGithubResource::class).getOauth(projectId, userId, repoHashId).data!!.redirectUrl
    }

    override fun getProject(accessToken: String, userId: String): List<Project> {
        val repos = client.get(ServiceGithubResource::class).getProject(accessToken, userId).data!!
        val projects = repos.map {
            Project(
                it.id,
                it.name,
                it.fullName,
                it.sshUrl,
                it.httpUrl,
                it.updatedAt
            )
        }
        return projects
    }
}