package com.tencent.devops.ticket.service

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.ticket.dao.CertDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class V3CertPermissionService @Autowired constructor(
    private val certDao: CertDao,
    private val dslContext: DSLContext,
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

    override fun filterCert(userId: String, projectId: String, authPermission: AuthPermission): List<String> {
        val certInfo = super.filterCert(userId, projectId, authPermission)
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

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}