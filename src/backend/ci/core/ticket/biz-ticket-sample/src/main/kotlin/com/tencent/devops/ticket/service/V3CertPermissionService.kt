package com.tencent.devops.ticket.service

import com.tencent.devops.common.api.util.OwnerUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.ticket.dao.CertDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class V3CertPermissionService @Autowired constructor(
    private val certDao: CertDao,
    private val dslContext: DSLContext,
    private val client: Client,
    private val redisOperation: RedisOperation,
    authResourceApi: AuthResourceApi,
    authPermissionApi: AuthPermissionApi,
    ticketAuthServiceCode: TicketAuthServiceCode
) : AbstractCertPermissionService(
    authResourceApi = authResourceApi,
    authPermissionApi = authPermissionApi,
    ticketAuthServiceCode = ticketAuthServiceCode
) {

    override fun supplierForPermission(projectId: String): () -> MutableList<String> {
        return {
            val fakeList = mutableListOf<String>()
            certDao.listIdByProject(
                dslContext = dslContext,
                projectId = projectId,
                offset = 0,
                limit = 500
            ).forEach {
                fakeList.add(it)
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

    override fun filterCert(userId: String, projectId: String, authPermission: AuthPermission): List<String> {
        val certInfo = if (isProjectOwner(projectId, userId)) {
            arrayListOf("*")
        } else {
            super.filterCert(userId, projectId, authPermission)
        }
        logger.info("filterCert user[$userId] project[$projectId] auth[$authPermission] list[$certInfo]")
        if (certInfo.contains("*")) {
            return getAllCertByProject(projectId)
        }
        return certInfo
    }

    override fun filterCerts(userId: String, projectId: String, authPermissions: Set<AuthPermission>): Map<AuthPermission, List<String>> {
        val certMaps = super.filterCerts(userId, projectId, authPermissions)
        val certResultMap = mutableMapOf<AuthPermission, List<String>>()
        certMaps.forEach { key, value ->
            if (isProjectOwner(projectId, userId)) {
                certResultMap[key] = getAllCertByProject(projectId)
                return@forEach
            }
            if (value.contains("*")) {
                logger.info("filterCert user[$userId] project[$projectId] auth[$key] list[$value]")
                certResultMap[key] = getAllCertByProject(projectId)
                logger.info("filterCert user[$userId] project[$projectId] auth[$key] list[$value] ${certResultMap[key]}")
            } else {
                certResultMap[key] = value
            }
        }
        return certResultMap
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

    private fun isProjectOwner(projectId: String, userId: String): Boolean {
        val cacheOwner = redisOperation.get(OwnerUtils.getOwnerRedisKey(projectId))
        if (cacheOwner.isNullOrEmpty()) {
            val projectVo = client.get(ServiceProjectResource::class).get(projectId).data ?: return false
            val projectCreator = projectVo.creator
            logger.info("cert permission get ProjectOwner $projectId | $projectCreator | $userId")
            return if (!projectCreator.isNullOrEmpty()) {
                redisOperation.set(OwnerUtils.getOwnerRedisKey(projectId), projectCreator!!)
                userId == projectCreator
            } else {
                false
            }
        } else {
            return userId == cacheOwner
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}