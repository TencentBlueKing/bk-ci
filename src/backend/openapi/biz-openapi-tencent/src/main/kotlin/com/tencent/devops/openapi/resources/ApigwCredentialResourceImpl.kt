package com.tencent.devops.openapi.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.ApigwCredentialResource
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.Credential
import com.tencent.devops.ticket.pojo.enums.Permission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwCredentialResourceImpl @Autowired constructor(private val client: Client) : ApigwCredentialResource {
    override fun hasPermissionList(
        userId: String,
        projectId: String,
        credentialTypesString: String?,
        permission: Permission,
        page: Int?,
        pageSize: Int?
    ): Result<Page<Credential>> {
        logger.info("get credential of project($projectId) by user($userId)")
        return client.get(ServiceCredentialResource::class).hasPermissionList(
            userId = userId,
            projectId = projectId,
            credentialTypesString = credentialTypesString,
            permission = permission,
            page = page,
            pageSize = pageSize,
            keyword = null
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwCredentialResourceImpl::class.java)
    }
}