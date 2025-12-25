package com.tencent.devops.project.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.common.api.constant.DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.common.api.exception.CustomMessageException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.security.util.BkCryptoUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.SignaturePlatformDetailsDao
import com.tencent.devops.project.pojo.SignatureCallbackInfo
import com.tencent.devops.project.pojo.SignatureCallbackResponse
import com.tencent.devops.project.pojo.SignaturePlatformDetails
import com.tencent.devops.project.pojo.SignaturePlatformUpdateRequest
import com.tencent.devops.project.pojo.UserSignatureStatusDTO
import com.tencent.devops.project.pojo.UserSignatureStatusResponse
import jakarta.servlet.http.HttpServletRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Service
class SignatureManageService(
    private val signaturePlatformDetailsDao: SignaturePlatformDetailsDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val authProjectApi: AuthProjectApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val client: Client,
    private val objectMapper: ObjectMapper
) {
    @Value("\${aes.aesKey}")
    private lateinit var aesKey: String

    @Value("\${signature.clientId:}")
    private lateinit var bkCiClientId: String

    fun listSignatureProjects(): List<String> {
        return if (eSignControl()) {
            redisOperation.getSetMembers(PROJECTS_REQUIRING_SIGNATURE_VERIFICATION)?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun getSignatureStatus(projectId: String, userId: String): UserSignatureStatusResponse {
        // 1. 如果项目不需要签名校验，直接返回成功
        if (!isSignatureVerificationRequired(projectId)) {
            return UserSignatureStatusResponse(userId = userId, signed = true)
        }
        logger.info("Get signature status for project[$projectId], user[$userId]")
        // 2. 校验用户项目权限
        verifyUserProjectPermission(userId, projectId)
        // 3. 获取签名服务的平台信息
        val platform = getPlatformByProjectId(projectId)
            ?: return UserSignatureStatusResponse(userId = userId, signed = true)
        // 4. 检查缓存中是否已有签名状态
        if (isUserSignedInCache(buildUserStatusCacheKey(platform), userId)) {
            return UserSignatureStatusResponse(userId = userId, signed = true)
        }
        // 5. 调用外部服务查询实时签名状态
        return fetchLiveSignatureStatus(userId, projectId)
    }

    /**
     * 签名回调处理
     */
    fun callback(callbackInfo: SignatureCallbackInfo): SignatureCallbackResponse {
        val headers = extractAndValidateCallbackHeaders()
        logger.info("Signature callback received: $headers, body: $callbackInfo")
        verifyCallbackSignature(headers)

        // 更新用户签名状态缓存
        updateUserSignatureCacheFromCallback(headers.platform, callbackInfo)

        return SignatureCallbackResponse.success()
    }

    private fun isSignatureVerificationRequired(projectId: String): Boolean {
        return eSignControl() && redisOperation.isMember(PROJECTS_REQUIRING_SIGNATURE_VERIFICATION, projectId)
    }

    private fun verifyUserProjectPermission(userId: String, projectId: String) {
        val isProjectUser = authProjectApi.isProjectUser(
            user = userId,
            projectCode = projectId,
            serviceCode = pipelineAuthServiceCode,
            group = null
        )
        if (!isProjectUser) {
            throw PermissionForbiddenException("User '$userId' does not have permission for project '$projectId'.")
        }
    }

    private fun getPlatformByProjectId(projectId: String): String? {
        return redisOperation.get(PROJECT_SIGNATURE_PLATFORM_KEY.format(projectId)).also {
            if (it == null) {
                logger.error("found project id not found for platform failed :$projectId")
            }
        }
    }

    private fun getPlatformDetails(platform: String): SignaturePlatformDetails {
        return signaturePlatformDetailsDao.get(
            dslContext = dslContext,
            platform = platform
        )?.apply {
            this.platformSecret = BkCryptoUtil.decryptSm4OrAes(aesKey, this.platformSecret)
        } ?: throw CustomMessageException(
            "Platform details not found for platform: $platform.The data may be inconsistent."
        )
    }

    private fun isUserSignedInCache(cacheKey: String, userId: String): Boolean {
        return redisOperation.isMember(cacheKey, userId)
    }

    fun fetchLiveSignatureStatus(
        userId: String,
        projectId: String
    ): UserSignatureStatusResponse {
        val platform = getPlatformByProjectId(projectId) ?: return UserSignatureStatusResponse(
            userId = userId, signed = true
        )
        logger.info("fetch live signature status {}|{}|{}", projectId, platform, userId)
        // 获取用户信息并校验用户是否存在
        val userNickName = try {
            getUserNickname(userId)
        } catch (ex: Exception) {
            logger.warn("Failed to get user nickname, use userId instead", ex)
            userId
        }
        try {
            // 构建请求
            val request = buildSignatureQueryRequest(
                userId = userId.removeSuffix("@tai"),
                nickName = userNickName.removeSuffix("@tai"),
                platform = platform,
                nonce = generateRandomString(12),
                timestamp = System.currentTimeMillis()
            )

            // 执行请求并解析响应
            val statusData = OkhttpUtils.doHttp(request).use { response ->
                if (!response.isSuccessful) {
                    logger.warn("Failed to get signature status. URL: ${request.url}, Response: $response")
                    throw RemoteServiceException("Remote signature service request failed.")
                }
                val responseBody = response.body?.string() ?: throw RemoteServiceException(
                    "Remote signature service returned an empty body."
                )
                logger.info("Signature status response from remote: $responseBody")
                val queryResponse = JsonUtil.to(responseBody, ResponseDTO::class.java)
                if (queryResponse.result != "success") {
                    logger.warn(
                        "Signature service indicates failure. URL: ${request.url}, Message: ${queryResponse.message}"
                    )
                    throw RemoteServiceException(
                        "Signature service returned a failure result: ${queryResponse.message}"
                    )
                }

                queryResponse.data ?: throw OperationException("Signature service returned null data.")
            }
            return processSignatureStatusResult(
                userId = userId,
                statusData = statusData,
                platform = platform
            )
        } catch (ex: Exception) {
            // 若第三方接口出现故障，不影响用户使用，告警，通知开发人员处理
            logger.warn("fetch Live Signature Status failed! {}|{}|{}", platform, projectId, userId, ex)
            return UserSignatureStatusResponse(
                userId = userId,
                signed = false
            )
        }
    }

    fun generateRandomString(length: Int): String = buildString(length) {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9') // 使用范围操作符
        repeat(length) {
            append(charset.random()) // 直接使用随机元素
        }
    }

    private fun processSignatureStatusResult(
        userId: String,
        statusData: UserSignatureStatusDTO,
        platform: String
    ): UserSignatureStatusResponse {
        val platformDetails = getPlatformDetails(platform)
        val cacheKey = buildUserStatusCacheKey(platform)
        return if (statusData.isSigned()) {
            redisOperation.addSetValue(cacheKey, userId)
            UserSignatureStatusResponse(
                userId = userId,
                signed = true
            )
        } else {
            val targetLanguage = I18nUtil.getLanguage(userId)
            val (information, agreementTips) = if (targetLanguage == DEFAULT_LOCALE_LANGUAGE) {
                Pair(platformDetails.platform, platformDetails.informationCn)
            } else {
                Pair(platformDetails.platformName, platformDetails.informationEn)
            }
            UserSignatureStatusResponse(
                userId = userId,
                signed = false,
                schemeQrcodeUrl = statusData.schemeQrcodeUrl,
                qrCodeUrl = statusData.qrCodeUrl,
                projectInformation = information,
                agreementTips = agreementTips
            )
        }
    }

    private fun getUserNickname(userId: String): String {
        return client.get(ServiceDeptResource::class).getUserInfo(userId = userId, name = userId)
            .data?.displayName ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.FAILED_USER_INFORMATION,
            params = arrayOf(userId)
        )
    }

    private fun buildSignatureQueryRequest(
        userId: String,
        nickName: String,
        platform: String,
        nonce: String,
        timestamp: Long
    ): Request {
        val platformDetails = getPlatformDetails(platform)
        val token = cryptoToken(bkCiClientId, platformDetails.platformSecret, nonce, timestamp)
        val body = SignatureStatusQueryReq(user = userId, nick = nickName, tof_id = userId) // Renamed tof_id
        val requestBody = objectMapper.writeValueAsString(body).toRequestBody(mediaType)
        return Request.Builder()
            .url(platformDetails.url)
            .post(requestBody)
            .addHeader(buildHeader(platform, CLIENT_ID_PLACEHOLDER), bkCiClientId)
            .addHeader(buildHeader(platform, NONCE_PLACEHOLDER), nonce)
            .addHeader(buildHeader(platform, TIMESTAMP_PLACEHOLDER), timestamp.toString())
            .addHeader(buildHeader(platform, SIGNATURE_PLACEHOLDER), token)
            .build()
    }

    private fun buildHeader(
        platform: String,
        placeholder: String
    ): String = platform.plus(placeholder)

    private fun cryptoToken(
        clientId: String,
        secretKey: String,
        nonce: String,
        timestamp: Long
    ): String {
        return ShaUtils.sha256(clientId + secretKey + nonce + timestamp)
    }

    private data class CallbackHeaders(
        val platform: String,
        val clientId: String,
        val nonce: String,
        val timestamp: Long,
        val signature: String
    )

    private fun extractAndValidateCallbackHeaders(): CallbackHeaders {
        val request = getHttpServletRequest() ?: throw IllegalStateException("HttpServletRequest unavailable")
        val headerNames = request.headerNames.toList()
        logger.debug("Extracting callback headers: ${headerNames.joinToString()}") // 降级为debug日志

        // 1. 获取platform
        val platform = headerNames
            .asSequence()
            .mapNotNull { headerName ->
                when {
                    // 优先匹配固定平台标识
                    headerName.equals(PLATFORM_PLACEHOLDER, ignoreCase = true) -> request.getHeader(headerName)
                    // 次优先匹配包含clientId标识的header
                    headerName.contains(CLIENT_ID_PLACEHOLDER, ignoreCase = true) ->
                        headerName.substringBeforeIgnoreCase(CLIENT_ID_PLACEHOLDER).takeIf(String::isNotBlank)

                    else -> null
                }
            }.firstOrNull() ?: throw CustomMessageException("Missing required header: platform")

        // 2. 构建header映射（单次遍历优化）
        val headerMap = headerNames.associateWith { request.getHeader(it) }

        // 3. 预计算header模式
        val clientIdPattern = buildHeader(platform, CLIENT_ID_PLACEHOLDER)
        val noncePattern = buildHeader(platform, NONCE_PLACEHOLDER)
        val timestampPattern = buildHeader(platform, TIMESTAMP_PLACEHOLDER)
        val signaturePattern = buildHeader(platform, SIGNATURE_PLACEHOLDER)

        return CallbackHeaders(
            platform = platform,
            clientId = findHeaderByPattern(headerMap, clientIdPattern, "clientId"),
            nonce = findHeaderByPattern(headerMap, noncePattern, "nonce"),
            timestamp = findHeaderByPattern(headerMap, timestampPattern, "timestamp").toLong(),
            signature = findHeaderByPattern(headerMap, signaturePattern, "signature")
        )
    }

    fun String.substringBeforeIgnoreCase(delimiter: String): String {
        val lowerInput = this.lowercase()
        val lowerDelimiter = delimiter.lowercase()
        val index = lowerInput.indexOf(lowerDelimiter)
        return if (index >= 0) this.substring(0, index) else this
    }

    private fun findHeaderByPattern(
        headerMap: Map<String, String>,
        pattern: String,
        headerType: String // 新增参数明确header类型
    ): String {
        require(pattern.isNotBlank()) { "Pattern for $headerType must not be blank" }

        return headerMap.entries
            .firstOrNull { (key, _) ->
                key.contains(pattern, ignoreCase = true)
            }?.value ?: throw CustomMessageException(
            "Missing required header for $headerType: '$pattern'"
        )
    }

    private fun getHttpServletRequest(): HttpServletRequest? {
        return (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
    }

    private fun verifyCallbackSignature(headers: CallbackHeaders) {
        if (System.currentTimeMillis() - headers.timestamp > 10000) { // 超过10秒（10,000毫秒）
            throw CustomMessageException("Timestamp expired. Allowed range: ±10 seconds")
        }
        val platformDetails = getPlatformDetails(headers.platform)
        val expectedSignature = cryptoToken(
            headers.clientId,
            platformDetails.platformSecret,
            headers.nonce,
            headers.timestamp
        )
        if (headers.signature != expectedSignature) {
            throw CustomMessageException("Callback signature verification failed.")
        }
    }

    private fun updateUserSignatureCacheFromCallback(
        platform: String,
        callbackInfo: SignatureCallbackInfo
    ) {
        val key = buildUserStatusCacheKey(platform)
        if (callbackInfo.isSigned()) {
            redisOperation.addSetValue(key, callbackInfo.user)
        } else {
            redisOperation.removeSetMember(key, callbackInfo.user)
        }
    }

    private fun buildUserStatusCacheKey(platform: String): String {
        return USER_SIGNATURE_STATUS_CACHE_KEY.format(platform)
    }

    fun createOrUpdatePlatform(details: SignaturePlatformDetails) {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = SIGNATURE_PLATFORM_DATA_MODIFY_LOCK_KEY,
            expiredTimeInSeconds = 10
        )
        try {
            redisLock.lock()
            val oldProjectsVerificationRequired = signaturePlatformDetailsDao.list(
                dslContext = dslContext
            ).flatMap { it.projectIds }.distinct()
            details.platformSecret = BkCryptoUtil.encryptSm4ButAes(aesKey, details.platformSecret)
            // 更新数据库
            signaturePlatformDetailsDao.createOrUpdate(dslContext, details)
            // 刷新缓存
            val latestClientDetails = signaturePlatformDetailsDao.list(dslContext)
            val latestProjectsVerificationRequired = latestClientDetails.flatMap { it.projectIds }.distinct()
            latestClientDetails.forEach {
                it.projectIds.forEach { projectId ->
                    val key = String.format(PROJECT_SIGNATURE_PLATFORM_KEY, projectId)
                    redisOperation.set(key, it.platform, expired = false)
                }
            }
            val toAddRecords = latestProjectsVerificationRequired.filter { it !in oldProjectsVerificationRequired }
            val toDeleteRecords = oldProjectsVerificationRequired.filter { it !in latestProjectsVerificationRequired }
            if (toDeleteRecords.isNotEmpty()) {
                redisOperation.sremove(PROJECTS_REQUIRING_SIGNATURE_VERIFICATION, *toDeleteRecords.toTypedArray())
                redisOperation.sremove(PROJECTS_REQUIRING_SIGNATURE_PRE_CHECK, *toDeleteRecords.toTypedArray())
            }
            if (toAddRecords.isNotEmpty()) {
                redisOperation.sadd(PROJECTS_REQUIRING_SIGNATURE_PRE_CHECK, *toAddRecords.toTypedArray())
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun deletePlatform(platform: String) {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = SIGNATURE_PLATFORM_DATA_MODIFY_LOCK_KEY,
            expiredTimeInSeconds = 10
        )
        try {
            redisLock.lock()
            val clientDetails = signaturePlatformDetailsDao.get(dslContext, platform)
                ?: throw CustomMessageException("platform not exist :$platform")
            val projectIds = clientDetails.projectIds
            // 刷新缓存
            projectIds.forEach { projectCode ->
                redisOperation.delete(String.format(PROJECT_SIGNATURE_PLATFORM_KEY, projectCode))
            }
            if (projectIds.isNotEmpty()) {
                redisOperation.sremove(PROJECTS_REQUIRING_SIGNATURE_VERIFICATION, *projectIds.toTypedArray())
                redisOperation.sremove(PROJECTS_REQUIRING_SIGNATURE_PRE_CHECK, *projectIds.toTypedArray())
            }
            signaturePlatformDetailsDao.delete(dslContext, platform)
        } finally {
            redisLock.unlock()
        }
    }

    fun updatePlatformInformation(platform: String, request: SignaturePlatformUpdateRequest) {
        if (request.platformName == null && request.informationCn == null && request.informationEn == null) {
            throw CustomMessageException("At least one field must be provided for update")
        }
        signaturePlatformDetailsDao.get(dslContext, platform)
            ?: throw CustomMessageException("Platform not found: $platform")
        logger.info("Updating platform information for: $platform, request: $request")
        signaturePlatformDetailsDao.updateInformation(
            dslContext = dslContext,
            platform = platform,
            platformName = request.platformName,
            informationCn = request.informationCn,
            informationEn = request.informationEn
        )
    }

    private fun eSignControl(): Boolean {
        return try {
            return redisOperation.get(E_SIGNATURE_VERIFICATION_CONTROL)?.toBooleanStrict() == true
        } catch (ex: Exception) {
            logger.error("e Sign Control failed!")
            return false
        }
    }

    data class SignatureStatusQueryReq(
        val user: String,
        val nick: String,
        val tof_id: String
    )

    data class ResponseDTO(
        val message: String?,
        val result: String,
        val data: UserSignatureStatusDTO?
    )

    companion object {
        private val logger = LoggerFactory.getLogger(SignatureManageService::class.java)

        // 用户电子签状态。set类型，一个平台一个Key
        private const val USER_SIGNATURE_STATUS_CACHE_KEY = "user:signature:status:%s:cache"

        // 预校验的项目ID名单
        private const val PROJECTS_REQUIRING_SIGNATURE_PRE_CHECK = "projects:signature:pre:check"

        // 电子签校验的项目ID名单
        private const val PROJECTS_REQUIRING_SIGNATURE_VERIFICATION = "projects:signature:verification:required"

        private const val E_SIGNATURE_VERIFICATION_CONTROL = "e:signature:verification:control"

        // 项目所属的平台
        private const val PROJECT_SIGNATURE_PLATFORM_KEY = "projects:signature:%s:platform"
        private const val SIGNATURE_PLATFORM_DATA_MODIFY_LOCK_KEY = "signature:platform:data:modify:lock:key"
        private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        private const val CLIENT_ID_PLACEHOLDER = "-clientId"
        private const val NONCE_PLACEHOLDER = "-Nonce"
        private const val TIMESTAMP_PLACEHOLDER = "-Timestamp"
        private const val SIGNATURE_PLACEHOLDER = "-Signature"
        private const val PLATFORM_PLACEHOLDER = "x-esign-platform"
    }
}
