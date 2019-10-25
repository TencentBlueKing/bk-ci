package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.tcm.TcmReqParam
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceTcmResource
import com.tencent.devops.plugin.service.TcmService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTcmResourceImpl @Autowired constructor(private val tcmService: TcmService) : ServiceTcmResource {
    override fun startTask(tcmReqParam: TcmReqParam, buildId: String, userId: String): Result<String> {
        return Result(tcmService.startTask(tcmReqParam, buildId, userId))
    }
}