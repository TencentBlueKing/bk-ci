package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.service.BkTicketService
import com.tencent.devops.remotedev.service.WorkspaceService

@RestResource
@Suppress("ALL")
class ServiceRemoteDevResourceImpl(
    private val bkTicketService: BkTicketService
) : ServiceRemoteDevResource {
    override fun validateUserTicket(userId: String, isOffshore: Boolean, ticket: String): Result<Boolean> {
        return Result(bkTicketService.validateUserTicket(userId, isOffshore, ticket))
    }

}
