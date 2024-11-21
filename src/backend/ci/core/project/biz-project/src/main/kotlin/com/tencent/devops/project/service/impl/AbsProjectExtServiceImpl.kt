package com.tencent.devops.project.service.impl

import com.tencent.devops.artifactory.api.service.ServiceBkRepoResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.service.ProjectExtService
import org.springframework.beans.factory.annotation.Autowired

abstract class AbsProjectExtServiceImpl : ProjectExtService {
    @Autowired
    private lateinit var client: Client

    override fun enableProject(userId: String, projectId: String, enabled: Boolean): Boolean {
        client.get(ServiceBkRepoResource::class).enableProject(userId, projectId, enabled)
        return true
    }
}
