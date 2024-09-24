package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionData
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionResult
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckData
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckResult
import com.tencent.devops.remotedev.service.client.FaceCheckData
import com.tencent.devops.remotedev.service.client.TaiClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserInfoCertService @Autowired constructor(
    private val taiClient: TaiClient,
    private val apiGwService: ApiGwService
) {
    fun needRealNameCert(username: String): Boolean {
        val res = try {
            taiClient.realTimeUser(username)
        } catch (e: Exception) {
            logger.error("$USER_CERT_LOG_PREFIX|realTimeUser error", e)
            return true
        }
        return res.faceRecognized
    }

    fun multipleCert(data: UserInfoCheckData): UserInfoCheckResult {
        try {
            return apiGwService.workspaceAccessManageControl(data.projectId, data.workspaceName) ?: run {
                logger.error("$USER_CERT_LOG_PREFIX|workspaceAccessManageControl null")
                return UserInfoCheckResult.noCheck()
            }
        } catch (e: Exception) {
            logger.error("$USER_CERT_LOG_PREFIX|workspaceAccessManageControl error", e)
            return UserInfoCheckResult.noCheck()
        }
    }

    fun faceRecognition(data: FaceRecognitionData): FaceRecognitionResult {
        try {
            return taiClient.faceCheck(data.username, FaceCheckData(data.base64FaceData))
        } catch (e: Exception) {
            logger.error("$USER_CERT_LOG_PREFIX|faceCheck error", e)
            return FaceRecognitionResult.noCheck()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserInfoCertService::class.java)
        private const val USER_CERT_LOG_PREFIX = "USER_CERT_LOG"
    }
}