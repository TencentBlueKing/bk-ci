package com.tencent.devops.project.service.impl

import com.tencent.devops.artifactory.api.service.ServiceBkRepoResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.service.ProjectExtService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class AbsProjectExtServiceImpl : ProjectExtService {
    @Autowired
    private lateinit var client: Client

    companion object {
        private val logger = LoggerFactory.getLogger(AbsProjectExtServiceImpl::class.java)
    }

    override fun enableProject(userId: String, projectId: String, enabled: Boolean): Boolean {
        logger.info("enable project|$userId|$projectId|$enabled")
        client.get(ServiceBkRepoResource::class).enableProject(userId, projectId, enabled)
        client.get(ServiceProjectAuthResource::class).modifyProjectEnabled(
            projectCode = projectId,
            enabled = enabled
        )
        return true
    }
}
