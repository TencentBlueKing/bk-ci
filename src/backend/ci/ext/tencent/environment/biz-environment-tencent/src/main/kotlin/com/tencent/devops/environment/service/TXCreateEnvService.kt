package com.tencent.devops.environment.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TXCreateEnvService @Autowired constructor(
    private val client: Client
) : CreateEnvService() {
    override fun fetchUserWorkspaceId(projectId: String, userId: String): List<String> {
        return client.get(ServiceRemoteDevResource::class).getWorkspaceListNew(
            userId = userId,
            projectId = projectId,
            page = null,
            pageSize = null,
            search = WorkspaceSearch()
        ).data?.records?.map { it.workspaceName } ?: emptyList()
    }

    override fun getWorkspaceDisplayName(userId: String, projectId: String, workspaceId: String?): String? {
        return client.get(ServiceRemoteDevResource::class).getProjectWorkspace(
            userId, projectId, workspaceId ?: return null
        ).data?.displayName
    }
}