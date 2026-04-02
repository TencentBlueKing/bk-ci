package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.ci.UserUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserInfoResource
import com.tencent.devops.remotedev.pojo.TrustDeviceInfo
import com.tencent.devops.remotedev.pojo.TrustDeviceTokenGetData
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognition
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionData
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionResult
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoAuthCheck
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckData
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckResult
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoMoaCheckConfig
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.TrustDeviceService
import com.tencent.devops.remotedev.service.UserInfoCertService
import com.tencent.devops.remotedev.service.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserInfoResourceImpl @Autowired constructor(
    private val userInfoCertService: UserInfoCertService,
    private val trustDeviceService: TrustDeviceService,
    private val workspaceService: WorkspaceService,
    private val permissionService: PermissionService
) : UserInfoResource {
    override fun realNameCert(name: String): Result<Boolean> {
        return Result(userInfoCertService.needRealNameCert(name))
    }

    override fun multipleCert(
        userId: String,
        deviceId: String?,
        token: String?,
        data: UserInfoCheckData
    ): Result<UserInfoCheckResult> {
        permissionService.checkViewerPermission(userId, data.workspaceName, data.projectId)
        val res = userInfoCertService.multipleCert(data)
        // 产品要求：集团员工跳过人脸识别
        if (!UserUtil.isTaiUser(userId)) {
            res.faceRecognition = FaceRecognition(0, "", false)
        }
        if (deviceId != null && token != null) {
            // 如果是云桌面拥有者 + token有效，才豁免ioa认证。
            if (workspaceService.checkExistWorkspaceSharedInfo(
                workspaceName = data.workspaceName,
                sharedUser = userId,
                assignType = WorkspaceShared.AssignType.OWNER
            ) && trustDeviceService.checkTrustDevice(
                    userId = userId,
                    deviceId = deviceId,
                    token = token
            )
            ) {
                return Result(res.copy(moa = UserInfoMoaCheckConfig(false)))
            }
        }
        return Result(res)
    }

    override fun faceRecognition(data: FaceRecognitionData): Result<FaceRecognitionResult> {
        return Result(userInfoCertService.faceRecognition(data))
    }

    override fun asyncAuthCheck(userId: String, data: UserInfoAuthCheck): Result<Boolean> {
        userInfoCertService.asyncAuthCheck(userId, data)
        return Result(true)
    }

    override fun getTrustDeviceToken(userId: String, data: TrustDeviceTokenGetData): Result<String> {
        return Result(trustDeviceService.getOrCreateToken(userId, data.deviceId, data.detail))
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
