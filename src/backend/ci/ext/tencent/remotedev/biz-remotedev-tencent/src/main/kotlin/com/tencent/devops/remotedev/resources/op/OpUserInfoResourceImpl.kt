package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpUserInfoResource
import com.tencent.devops.remotedev.service.UserInfoCertService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpUserInfoResourceImpl @Autowired constructor(
    private val userInfoCertService: UserInfoCertService
) : OpUserInfoResource {
    override fun authCheckCallback(id: Long) {
        userInfoCertService.asyncAuthCheckITSMCallBack(id)
    }
}