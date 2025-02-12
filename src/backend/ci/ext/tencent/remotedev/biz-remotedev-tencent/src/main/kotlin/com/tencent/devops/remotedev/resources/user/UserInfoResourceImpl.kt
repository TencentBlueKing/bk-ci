package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserInfoResource
import com.tencent.devops.remotedev.pojo.TrustDeviceInfo
import com.tencent.devops.remotedev.pojo.TrustDeviceTokenGetData
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionData
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionResult
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoAuthCheck
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckData
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckResult
import com.tencent.devops.remotedev.service.TrustDeviceService
import com.tencent.devops.remotedev.service.UserInfoCertService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserInfoResourceImpl @Autowired constructor(
    private val userInfoCertService: UserInfoCertService,
    private val trustDeviceService: TrustDeviceService
) : UserInfoResource {
    override fun realNameCert(name: String): Result<Boolean> {
        return Result(userInfoCertService.needRealNameCert(name))
    }

    override fun multipleCert(
        userId: String?,
        deviceId: String?,
        token: String?,
        data: UserInfoCheckData
    ): Result<UserInfoCheckResult> {
        if (userId != null && deviceId != null && token != null) {
            if (trustDeviceService.checkTrustDevice(userId, deviceId, token)) {
                return Result(UserInfoCheckResult.noCheck())
            }
        }
        return Result(userInfoCertService.multipleCert(data))
    }

    override fun faceRecognition(data: FaceRecognitionData): Result<FaceRecognitionResult> {
        return Result(userInfoCertService.faceRecognition(data))
    }

    override fun asyncAuthCheck(data: UserInfoAuthCheck): Result<Boolean> {
        userInfoCertService.asyncAuthCheck(data)
        return Result(true)
    }

    override fun getTrustDeviceToken(data: TrustDeviceTokenGetData): Result<String> {
        return Result(trustDeviceService.getOrCreateToken(data.userId, data.deviceId, data.detail))
    }

    override fun verifyTrustDeviceToken(userId: String, deviceId: String, token: String): Result<Boolean> {
        return Result(trustDeviceService.checkTrustDevice(userId, deviceId, token))
    }

    override fun getTrustDeviceList(userId: String): Result<List<TrustDeviceInfo>> {
        return Result(trustDeviceService.fetchTrustDeviceList(userId))
    }

    override fun deleteTrustDevice(userId: String, deviceId: String): Result<Boolean> {
        trustDeviceService.deleteTrustDevice(userId, deviceId)
        return Result(true)
    }
}
