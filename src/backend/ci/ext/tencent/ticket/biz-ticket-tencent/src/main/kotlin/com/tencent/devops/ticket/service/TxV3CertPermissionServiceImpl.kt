package com.tencent.devops.ticket.service

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.TActionUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.ticket.dao.CertDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class TxV3CertPermissionServiceImpl @Autowired constructor(
    val client: Client,
    val certDao: CertDao,
    val dslContext: DSLContext,
    val tokenService: ClientTokenService,
    val authResourceApi: AuthResourceApiStr
) : CertPermissionService {
    override fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!validatePermission(userId, projectId, authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        authPermission: AuthPermission,
        message: String
    ) {
        val checkResult = client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            relationResourceType = null,
            resourceType = AuthResourceType.TICKET_CERT.value,
            resourceCode = resourceCode,
            action = buildCertAction(authPermission)
        ).data ?: false
        if (!checkResult) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            relationResourceType = null,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId,
            action = buildCertAction(authPermission)
        ).data ?: false
    }

    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        authPermission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            relationResourceType = null,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = resourceCode,
            action = buildCertAction(authPermission)
        ).data ?: false
    }

    override fun filterCert(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): List<String> {

        val certIamInfo = client.get(ServicePermissionAuthResource::class).getUserResourceByPermission(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            action = buildCertAction(authPermission),
            resourceType = AuthResourceType.TICKET_CERT.value,
            projectCode = projectId
        ).data ?: emptyList()

        logger.info("filterCert user[$userId] project[$projectId] auth[$authPermission] list[$certIamInfo]")
        if (certIamInfo.contains("*")) {
            return getAllCertByProject(projectId)
        }
        return certIamInfo
    }

    override fun filterCerts(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>> {
        val actions = TActionUtils.buildActionList(authPermissions, AuthResourceType.TICKET_CERT)
        val certResultMap = mutableMapOf<AuthPermission, List<String>>()
        val certIamInfo = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            action = actions,
            resourceType = AuthResourceType.TICKET_CERT.value,
            projectCode = projectId
        ).data ?: emptyMap()

        val projectAllCertIds: List<String> by lazy { getAllCertByProject(projectId) }

        certIamInfo.forEach { key, value ->
            val ids =
                if (value.contains("*")) {
                    projectAllCertIds
                } else {
                    value
                }
            certResultMap[key] = ids

            if (key == AuthPermission.VIEW) {
                certResultMap[AuthPermission.LIST] = ids
            }
        }
        return certResultMap
    }

    override fun createResource(userId: String, projectId: String, certId: String) {
        authResourceApi.createResource(
            user = userId,
            serviceCode = null,
            resourceType = AuthResourceType.TICKET_CERT.value,
            projectCode = projectId,
            resourceCode = certId,
            resourceName = certId
        )
    }

    override fun deleteResource(projectId: String, certId: String) {
        return
    }

    private fun buildCertAction(permission: AuthPermission): String {
        return TActionUtils.buildAction(permission, AuthResourceType.TICKET_CERT)
    }

    private fun getAllCertByProject(projectId: String): List<String> {
        val idList = mutableListOf<String>()
        val count = certDao.countByProject(dslContext, projectId, null)
        val records = certDao.listByProject(dslContext, projectId, 0, count.toInt())
        records.map {
            idList.add(it.certId)
        }
        return idList
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxV3CertPermissionServiceImpl::class.java)
    }
}
