package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserInfoResource
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionData
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionResult
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoAuthCheck
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckData
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckResult
import com.tencent.devops.remotedev.service.UserInfoCertService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserInfoResourceImpl @Autowired constructor(
    private val userInfoCertService: UserInfoCertService
) : UserInfoResource {
    override fun realNameCert(name: String): Result<Boolean> {
        return Result(userInfoCertService.needRealNameCert(name))
    }

    override fun multipleCert(data: UserInfoCheckData): Result<UserInfoCheckResult> {
        return Result(userInfoCertService.multipleCert(data))
    }

    override fun faceRecognition(data: FaceRecognitionData): Result<FaceRecognitionResult> {
        return Result(userInfoCertService.faceRecognition(data))
    }

    override fun asyncAuthCheck(data: UserInfoAuthCheck): Result<Boolean> {
        userInfoCertService.asyncAuthCheck(data)
        return Result(true)
    }
}
