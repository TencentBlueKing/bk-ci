package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.core.type.TypeReference
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.devops.auth.api.service.ServiceResourceGroupResource
import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.enum.JoinedType
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.project.pojo.FetchRemoteDevData
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.dao.UserAuthApplyDao
import com.tencent.devops.remotedev.pojo.UserAuthInfo
import com.tencent.devops.remotedev.pojo.UserAuthRecordStatus
import com.tencent.devops.remotedev.pojo.async.AsyncUserAuthCheck
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionData
import com.tencent.devops.remotedev.pojo.userinfo.FaceRecognitionResult
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoAuthCheck
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckData
import com.tencent.devops.remotedev.pojo.userinfo.UserInfoCheckResult
import com.tencent.devops.remotedev.service.client.FaceCheckData
import com.tencent.devops.remotedev.service.client.TaiClient
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys.REMOTEDEV_USER_FACE_RECOGNITION_ERROR_CODE_KEY
import java.time.Duration
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service

@Service
class UserInfoCertService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val configCacheService: ConfigCacheService,
    private val tokenService: ClientTokenService,
    private val streamBridge: StreamBridge,
    private val userAuthApplyDao: UserAuthApplyDao,
    private val taiClient: TaiClient,
    private val apiGwService: ApiGwService,
    private val bkItsmService: BKItsmService
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
            val result = taiClient.faceCheck(data.username, FaceCheckData(data.base64FaceData))
            if (result.data != null) {
                return result.data
            }
            if (result.error == null) {
                logger.error("$USER_CERT_LOG_PREFIX|faceCheck data is null")
                return FaceRecognitionResult.noCheck()
            }
            // error exist
            val checkError = result.error.details?.filter { it.code in loadFaceRecognitionErrorCodeCache() }
            if (!checkError.isNullOrEmpty()) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.FACE_RECOGNITION_ERROR.errorCode,
                    params = arrayOf("${result.error.code}|${result.error.message}|$checkError")
                )
            }
            logger.error("$USER_CERT_LOG_PREFIX|faceCheck error ${result.error}")
            return FaceRecognitionResult.noCheck()
        } catch (e: Exception) {
            if (e is ErrorCodeException && e.errorCode == ErrorCodeEnum.FACE_RECOGNITION_ERROR.errorCode) {
                throw e
            }
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
        val lock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "$REMOTEDEV_USER_ATUCH_CHECK_REDIS_KEY_PREFIX:${data.projectId}:${data.userId}",
            expiredTimeInSeconds = 60
        )
        try {
            if (!lock.tryLock()) {
                return
            }
            // 如果已经有一个正在跑的就不重复触发了
            val runningRecord = userAuthApplyDao.fetchRunning(dslContext, data.projectId, data.userId)
            if (runningRecord != null) {
                // 这个单过期了，置为超时无效
                if (runningRecord.createTime.plusDays(TICKET_EXPIRT_DAYS) < LocalDateTime.now()) {
                    userAuthApplyDao.updateStatus(dslContext, runningRecord.id, UserAuthRecordStatus.TIME_OUT)
                } else {
                    return
                }
            }
            val expiredTime = LocalDateTime.now().plusDays(30).timestampmilli()
            val groupList = client.get(ServiceResourceGroupResource::class).getMemberGroupsDetails(
                userId = data.userId,
                projectCode = data.projectId,
                resourceType = AuthResourceType.PROJECT.value,
                memberId = data.userId,
                maxExpiredAt = expiredTime,
                groupName = null,
                minExpiredAt = null,
                relatedResourceType = null,
                relatedResourceCode = null,
                action = null,
                start = null,
                limit = null
            ).data?.records?.filter { it.joinedType == JoinedType.DIRECT && it.expiredAt < expiredTime }
                ?.ifEmpty { null } ?: return

            val admins = client.get(ServiceTxUserResource::class).getRemoteDevAdmin(
                FetchRemoteDevData(
                    setOf(data.projectId)
                )
            ).data?.get(data.projectId) ?: run {
                logger.warn("$USER_CERT_LOG_PREFIX|doAsyncAuthCheck|getRemoteDevAdmin|${data.projectId} is null")
                return
            }

            val recordId = userAuthApplyDao.create(
                dslContext = dslContext,
                projectId = data.projectId,
                userId = data.userId,
                status = UserAuthRecordStatus.RUNNING,
                info = UserAuthInfo(
                    groupIds = groupList.map { it.groupId }.toSet()
                )
            )

            val groupNames = groupList.map {
                "权限组：[${it.groupName}]剩余：${
                    (it.expiredAt - LocalDateTime.now().timestampmilli()) / (24 * 3600 * 1000)
                }天"
            }
            val sn = try {
                bkItsmService.userAuthCheck(
                    recordId = recordId,
                    projectId = data.projectId,
                    userId = data.userId,
                    groupNames = groupNames,
                    admins = admins
                )
            } catch (e: Exception) {
                logger.error("$USER_CERT_LOG_PREFIX|itsmUserAuthCheck|${data.projectId}|${data.userId}", e)
                userAuthApplyDao.updateStatus(dslContext, recordId, UserAuthRecordStatus.FAILURE)
                return
            }

            userAuthApplyDao.updateTicketId(dslContext, recordId, sn)
        } finally {
            lock.unlock()
        }
    }

    fun asyncAuthCheckITSMCallBack(
        recordId: Long
    ) {
        val record = userAuthApplyDao.fetchById(dslContext, recordId) ?: run {
            logger.warn("$USER_CERT_LOG_PREFIX|fetchById|$recordId is null")
            return
        }
        val groupIds = JsonUtil.to(record.authInfo.data(), object : TypeReference<UserAuthInfo>() {}).groupIds
        try {
            groupIds.forEach {
                client.get(ServiceResourceMemberResource::class).renewalGroupMember(
                    token = tokenService.getSystemToken(),
                    userId = record.userId,
                    projectCode = record.projectId,
                    renewalConditionReq = GroupMemberSingleRenewalReq(
                        groupId = it,
                        targetMember = ResourceMemberInfo(
                            id = record.userId,
                            type = "user"
                        ),
                        renewalDuration = 30
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("$USER_CERT_LOG_PREFIX|renewalGroupMember error", e)
            userAuthApplyDao.updateStatus(dslContext, recordId, UserAuthRecordStatus.FAILURE)
            return
        }
        userAuthApplyDao.updateStatus(dslContext, recordId, UserAuthRecordStatus.SUCCESS)
    }

    private fun loadFaceRecognitionErrorCodeCache(): Set<String> {
        return faceRecognitionErrorCodeCache.get(REMOTEDEV_USER_FACE_RECOGNITION_ERROR_CODE_KEY) ?: setOf()
    }

    private val faceRecognitionErrorCodeCache: LoadingCache<String, Set<String>> = Caffeine.newBuilder()
        .maximumSize(100L)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build { key -> configCacheService.get(key)?.split(";")?.toSet() ?: setOf() }

    companion object {
        private val logger = LoggerFactory.getLogger(UserInfoCertService::class.java)
        private const val TICKET_EXPIRT_DAYS = 15L
        private const val USER_CERT_LOG_PREFIX = "USER_CERT_LOG"
        private const val REMOTEDEV_USER_ATUCH_CHECK_REDIS_KEY_PREFIX = "remotedev:user_auth_check"
    }
}
