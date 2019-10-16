package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.ServiceTstackResource
import com.tencent.devops.dispatch.pojo.TstackConfig
import com.tencent.devops.dispatch.service.TstackBuildService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTstackResourceImpl @Autowired constructor(val tstackBuildService: TstackBuildService) : ServiceTstackResource {
    override fun getTstackConfig(projectId: String): Result<TstackConfig> {
        return Result(tstackBuildService.getTstackConfig(projectId))
    }
}