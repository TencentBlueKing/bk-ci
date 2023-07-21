package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.service.ServiceReplicaResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.web.RestResource

@RestResource
class ServiceReplicaResourceImpl(
    private val bkRepoClient: BkRepoClient
) : ServiceReplicaResource {
    override fun createReplicaTask(
        userId: String,
        projectId: String,
        repoName: String,
        fullPath: String
    ): Result<Boolean> {
        if (bkRepoClient.useBkRepo()) {
            bkRepoClient.createReplicaTask(userId, projectId, repoName, fullPath)
        }
        return Result(true)
    }
}
