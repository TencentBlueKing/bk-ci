package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.OpPrivateVMResource
import com.tencent.devops.dispatch.service.PrivateVMService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPrivateVMResourceImpl @Autowired constructor(private val privateVMService: PrivateVMService) : OpPrivateVMResource {

    override fun list() =
            Result(privateVMService.list())

    override fun bind(vmId: Int, projectId: String): Result<Boolean> {
        privateVMService.add(vmId, projectId)
        return Result(true)
    }

    override fun unbind(vmId: Int, projectId: String): Result<Boolean> {
        privateVMService.delete(vmId, projectId)
        return Result(true)
    }
}