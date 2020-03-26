package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.BuildExtServiceEnvResource
import com.tencent.devops.store.pojo.dto.UpdateExtServiceEnvInfoDTO
import com.tencent.devops.store.service.ExtServiceEnvService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildExtServiceEnvResourceImpl @Autowired constructor(
    private val extServiceEnvService: ExtServiceEnvService
) : BuildExtServiceEnvResource {

    override fun updateExtServiceEnv(
        projectCode: String,
        serviceCode: String,
        version: String,
        updateExtServiceEnvInfo: UpdateExtServiceEnvInfoDTO
    ): Result<Boolean> {
        return extServiceEnvService.updateExtServiceEnvInfo(projectCode, serviceCode, version, updateExtServiceEnvInfo)
    }
}