package com.tencent.devops.ticket.service

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import com.tencent.devops.ticket.dao.CredentialDao
import org.jooq.DSLContext
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
}