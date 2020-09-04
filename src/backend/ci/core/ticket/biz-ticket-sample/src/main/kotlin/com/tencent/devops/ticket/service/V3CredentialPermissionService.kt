package com.tencent.devops.ticket.service

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.ticket.dao.CredentialDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class V3CredentialPermissionService @Autowired constructor(
    private val dslContext: DSLContext,
    private val credentialDao: CredentialDao,
    authResourceApi: AuthResourceApi,
    authPermissionApi: AuthPermissionApi,
    ticketAuthServiceCode: TicketAuthServiceCode
) : AbstractCredentialPermissionService(
        authResourceApi = authResourceApi,
        authPermissionApi = authPermissionApi,
        ticketAuthServiceCode = ticketAuthServiceCode
) {

    override fun supplierForFakePermission(projectId: String): () -> MutableList<String> {
        return {
            val fakeList = mutableListOf<String>()
            credentialDao.listByProject(
                    dslContext = dslContext,
                    projectId = projectId,
                    offset = 0,
                    limit = 500 // 一个项目不会有太多凭证
            ).forEach {
                fakeList.add(it.credentialId)
            }
            fakeList
        }
    }

    override fun filterCredential(userId: String, projectId: String, authPermission: AuthPermission): List<String> {
        val credentialInfo = super.filterCredential(userId, projectId, authPermission)
        logger.info("filterCredential user[$userId] project[$projectId] auth[$authPermission] list[$credentialInfo]")
        if (credentialInfo.contains("*")) {
            return getAllCredentialsByProject(projectId)
        }
        return credentialInfo
    }

    override fun filterCredentials(userId: String, projectId: String, authPermissions: Set<AuthPermission>): Map<AuthPermission, List<String>> {
        val credentialMaps = super.filterCredentials(userId, projectId, authPermissions)
        val credentialResultMap = mutableMapOf<AuthPermission, List<String>>()
        credentialMaps.forEach { key, value ->
            if (value.contains("*")) {
                logger.info("filterCredential user[$userId] project[$projectId] auth[$key] list[$value]")
                credentialResultMap[key] = getAllCredentialsByProject(projectId)
            } else {
                credentialResultMap[key] = value
            }
        }
        return credentialResultMap
    }

    override fun createResource(userId: String, projectId: String, credentialId: String, authGroupList: List<BkAuthGroup>?) {
        authResourceApi.createResource(userId, ticketAuthServiceCode, AuthResourceType.TICKET_CREDENTIAL, projectId, credentialId, credentialId)
    }

    private fun getAllCredentialsByProject(projectId: String): List<String> {
        val idList = mutableListOf<String>()
        val count = credentialDao.countByProject(dslContext, projectId)
        credentialDao.listByProject(dslContext, projectId, 0, count.toInt()).filter { idList.add(it.credentialId) }
        return idList
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}