package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceLunaResource
import com.tencent.devops.plugin.pojo.luna.LunaUploadParam
import com.tencent.devops.plugin.service.LunaService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceLunaResourceImpl @Autowired constructor(
    private val lunaService: LunaService
) : ServiceLunaResource {
    override fun pushFile(uploadParam: LunaUploadParam): Result<String> {
        return Result(lunaService.pushFile(uploadParam))
    }
}