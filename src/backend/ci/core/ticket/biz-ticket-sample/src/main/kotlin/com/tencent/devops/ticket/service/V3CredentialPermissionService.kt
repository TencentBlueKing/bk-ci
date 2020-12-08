package com.tencent.devops.ticket.service

import com.tencent.devops.common.api.util.OwnerUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.ticket.dao.CredentialDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class V3CredentialPermissionService @Autowired constructor(
    private val dslContext: DSLContext,
    private val credentialDao: CredentialDao,
    private val client: Client,
    private val redisOperation: RedisOperation,
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

    override fun validatePermission(userId: String, projectId: String, authPermission: AuthPermission, message: String) {
        if (isProjectOwner(projectId, userId)) {
            return
        }
        super.validatePermission(userId, projectId, authPermission, message)
    }

    override fun validatePermission(userId: String, projectId: String, resourceCode: String, authPermission: AuthPermission, message: String) {
        if (isProjectOwner(projectId, userId)) {
            return
        }
        super.validatePermission(userId, projectId, resourceCode, authPermission, message)
    }

    override fun validatePermission(userId: String, projectId: String, authPermission: AuthPermission): Boolean {
        if (isProjectOwner(projectId, userId)) {
            return true
        }
        return super.validatePermission(userId, projectId, authPermission)
    }

    override fun validatePermission(userId: String, projectId: String, resourceCode: String, authPermission: AuthPermission): Boolean {
        if (isProjectOwner(projectId, userId)) {
            return true
        }
        return super.validatePermission(userId, projectId, resourceCode, authPermission)
    }

    override fun filterCredential(userId: String, projectId: String, authPermission: AuthPermission): List<String> {
        val credentialInfo = if (isProjectOwner(projectId, userId)) {
            arrayListOf("*")
        } else {
            super.filterCredential(userId, projectId, authPermission)
        }
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
            if (isProjectOwner(projectId, userId)) {
                credentialResultMap[key] = getAllCredentialsByProject(projectId)
                return@forEach
            }
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

    private fun isProjectOwner(projectId: String, userId: String): Boolean {
        val cacheOwner = redisOperation.get(OwnerUtils.getOwnerRedisKey(projectId))
        if (cacheOwner.isNullOrEmpty()) {
            val projectVo = client.get(ServiceProjectResource::class).get(projectId).data ?: return false
            val projectCreator = projectVo.creator
            logger.info("credentials permission get ProjectOwner $projectId | $projectCreator| $userId")
            return if (!projectCreator.isNullOrEmpty()) {
                redisOperation.set(OwnerUtils.getOwnerRedisKey(projectId), projectCreator!!)
                userId == projectCreator
            } else {
                false
            }
        } else {
            logger.info("credentials cache $projectId |$userId | $cacheOwner | ${userId == cacheOwner}")
            return userId == cacheOwner
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}