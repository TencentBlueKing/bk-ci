package com.tencent.devops.support.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.ServiceMessageApproveResource
import com.tencent.devops.support.model.approval.CreateMoaApproveRequest
import com.tencent.devops.support.services.MessageApproveService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceMessageApproveResourceImpl @Autowired constructor(private val messageApproveService: MessageApproveService) : ServiceMessageApproveResource {

    override fun moaComplete(taskId: String): Result<Boolean> {
        return messageApproveService.moaComplete(taskId)
    }

    override fun createMoaMessageApproval(userId: String, createMoaApproveRequest: CreateMoaApproveRequest): Result<Boolean> {
        return messageApproveService.createMoaMessageApproval(userId, createMoaApproveRequest)
    }
}