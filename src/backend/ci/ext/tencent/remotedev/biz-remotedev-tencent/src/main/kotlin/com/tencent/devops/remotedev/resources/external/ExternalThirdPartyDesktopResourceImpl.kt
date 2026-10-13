package com.tencent.devops.remotedev.resources.external

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.external.ExternalThirdPartyDesktopResource
import com.tencent.devops.remotedev.pojo.expert.WorkspaceTaskStatus
import com.tencent.devops.remotedev.pojo.remotedev.CreateCvmResp
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopCdsKeyResp
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopRegisterReq
import com.tencent.devops.remotedev.service.thirdparty.ThirdPartyDesktopService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalThirdPartyDesktopResourceImpl @Autowired constructor(
    private val thirdPartyDesktopService: ThirdPartyDesktopService
) : ExternalThirdPartyDesktopResource {

    override fun registerVm(token: String, data: ThirdPartyDesktopRegisterReq): Result<CreateCvmResp?> {
        return Result(thirdPartyDesktopService.registerVm(token, data.ip))
    }

    override fun getTaskStatus(token: String, taskId: String): Result<WorkspaceTaskStatus?> {
        return Result(thirdPartyDesktopService.getTaskStatus(token, taskId))
    }

    override fun getCdsKey(token: String, vmId: String, envId: String): Result<ThirdPartyDesktopCdsKeyResp> {
        return Result(thirdPartyDesktopService.getCdsKey(token, vmId, envId))
    }

    override fun assignWorkspace(token: String, envId: String): Result<Boolean> {
        return Result(thirdPartyDesktopService.assignWorkspace(token, envId))
    }
}
