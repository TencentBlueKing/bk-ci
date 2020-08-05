package com.tencent.devops.ticket.service

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.ticket.dao.CertDao
import org.jooq.DSLContext
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
}