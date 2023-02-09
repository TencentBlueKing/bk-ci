package com.tencent.devops.worker.common.api.atom

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi

class StoreSdkResourceApi : AbstractBuildResourceApi(), StoreSdkApi {

    override fun isPlatformCodeRegistered(platformCode: String): Result<Boolean> {
        val path = "/ms/store/api/build/store/docking/platforms/codes/$platformCode/user/validate"
        val request = buildGet(path)
        val responseContent = request(
            request,
            "Failed to obtain the registration information of the atom docking platform"
        )
        return objectMapper.readValue(responseContent)
    }
}
