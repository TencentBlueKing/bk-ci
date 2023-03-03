package com.tencent.devops.dispatch.macos.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.macos.api.BuildPasswordResource
import com.tencent.devops.dispatch.macos.pojo.PasswordInfo
import com.tencent.devops.dispatcher.macos.service.MacVmService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildPasswordResourceImpl @Autowired constructor(
    private val macVmService: MacVmService
) : BuildPasswordResource {
    override fun get(
        projectId: String,
        pipelineId: String,
        buildId: String,
        realIp: String,
        publicKey: String
    ): Result<PasswordInfo?> {
        return Result(
            macVmService.getPassword(
                projectId,
                pipelineId,
                buildId,
                realIp,
                publicKey
            )
        )
    }
}
