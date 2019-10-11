package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.ServiceVMResource
import com.tencent.devops.dispatch.pojo.VM
import com.tencent.devops.dispatch.service.VMService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceVMResourceImpl @Autowired constructor(val vmService: VMService) : ServiceVMResource {

    override fun get(vmHashId: String): Result<VM> {
        if (vmHashId.isBlank()) {
            throw ParamBlankException("无效的VM哈希ID")
        }
        return Result(vmService.queryVMById(HashUtil.decodeOtherIdToInt(vmHashId)))
    }
}