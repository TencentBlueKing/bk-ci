package com.tencent.devops.remotedev.service

import com.tencent.devops.auth.api.service.ServiceResourceGroupResource
import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.enum.JoinedType
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.pojo.async.AsyncUserAuthCheck
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionData
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionResult
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoAuthCheck
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckData
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckResult
import com.tencent.devops.remotedev.service.client.FaceCheckData
import com.tencent.devops.remotedev.service.client.TaiClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserInfoCertService @Autowired constructor(
    private val client: Client,
    private val taiClient: TaiClient,
    private val tokenService: ClientTokenService,
    private val streamBridge: StreamBridge,
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

    fun asyncAuthCheck(data: UserInfoAuthCheck) {
        AsyncExecute.dispatch(
            streamBridge = streamBridge,
            data = AsyncUserAuthCheck(projectId = data.projectId, userId = data.userId),
            errorLogTag = USER_CERT_LOG_PREFIX
        )
    }

    fun doAsyncAuthCheck(data: AsyncUserAuthCheck) {
        val groupList = client.get(ServiceResourceGroupResource::class).getMemberGroupsDetails(
            userId = data.userId,
            projectCode = data.projectId,
            resourceType = AuthResourceType.PROJECT.value,
            memberId = data.userId,
            maxExpiredAt = LocalDateTime.now().plusDays(30).timestampmilli(),
            groupName = null,
            minExpiredAt = null,
            relatedResourceType = null,
            relatedResourceCode = null,
            action = null,
            start = null,
            limit = null
        ).data?.records?.filter { it.joinedType == JoinedType.DIRECT }?.ifEmpty { null } ?: return
    }

    fun asyncAuthCheckITSMCallBack(
        recordId: Long,
        projectId: String,
        userId: String,
        groupIds: Set<Int>
    ) {
        groupIds.forEach {
            client.get(ServiceResourceMemberResource::class).renewalGroupMember(
                token = tokenService.getSystemToken(),
                userId = userId,
                projectCode = projectId,
                renewalConditionReq = GroupMemberSingleRenewalReq(
                    groupId = it,
                    targetMember = ResourceMemberInfo(
                        id = userId,
                        type = "user"
                    ),
                    renewalDuration = 30
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserInfoCertService::class.java)
        private const val USER_CERT_LOG_PREFIX = "USER_CERT_LOG"
    }
}