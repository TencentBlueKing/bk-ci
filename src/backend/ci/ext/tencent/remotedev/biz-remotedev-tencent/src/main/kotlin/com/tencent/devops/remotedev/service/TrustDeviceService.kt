package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.auth.pojo.TokenInfo
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_ILLEGAL_ERROR
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_SECRET_ERROR
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_VALIDATE_ERROR
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.remotedev.dao.TrustDeviceDao
import com.tencent.devops.remotedev.pojo.TrustDeviceInfo
import com.tencent.devops.remotedev.pojo.TrustDeviceInfoDetail
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class TrustDeviceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val trustDeviceDao: TrustDeviceDao
) {
    @Value("\${remoteDev.trustDevice.secret:#{null}}")
    private val secret: String? = null

    @Value("\${remoteDev.trustDevice.expireDays:#{7}}")
    private val expireDays: Int = 7

    fun checkTrustDevice(userId: String, deviceId: String, token: String): Boolean {
        // 先判断是否过期
        if (!verifyToken(token)) {
            return false
        }
        // 在判断是否一致
        return trustDeviceDao.fetchAny(dslContext, userId, deviceId)?.token == token
    }

    fun getOrCreateToken(userId: String, deviceId: String, detail: TrustDeviceInfoDetail): String {
        // 先看有没有，没有就创建新的，有就返回
        val tokenR = trustDeviceDao.fetchAny(dslContext, userId, deviceId)?.token
        if (tokenR == null || !verifyToken(tokenR)) {
            val tokenN = genToken(userId, deviceId).accessToken!!
            trustDeviceDao.addOrUpdateDevice(dslContext, userId, deviceId, tokenN, detail)
            return tokenN
        }
        return tokenR
    }

    fun fetchTrustDeviceList(userId: String): List<TrustDeviceInfo> {
        return trustDeviceDao.fetchList(dslContext, userId).map {
            TrustDeviceInfo(
                deviceId = it.deviceId,
                createTime = it.createTime,
                updateTime = it.updateTime,
                detail = JsonUtil.to(it.deviceInfo.data(), object : TypeReference<TrustDeviceInfoDetail>() {})
            )
        }
    }

    fun deleteTrustDevice(userId: String, deviceId: String) {
        trustDeviceDao.delete(dslContext, userId, deviceId)
    }

    private fun verifyToken(token: String): Boolean {
        val tokenInfo = getTokenNoThrow(token)
        return tokenInfo != null && tokenInfo.expirationTime < System.currentTimeMillis()
    }

    private fun genToken(userId: String, deviceId: String): TokenInfo {
        logger.info("genToken|$userId|$deviceId|$expireDays")
        if (secret.isNullOrBlank()) {
            logger.error("genToken|[remotedev.trustDevice.secret] is not found")
            throw ErrorCodeException(
                errorCode = PARAMETER_SECRET_ERROR,
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                defaultMessage = "genToken|[remotedev.trustDevice.secret] is not found",
                params = arrayOf("config", "genToken|[remotedev.trustDevice.secret] is not found")
            )
        }
        val tokenInfo = TokenInfo(
            userId = genUserDetails(userId, deviceId),
            expirationTime = System.currentTimeMillis() + (expireDays * 24 * 60 * 60 * 1000),
            accessToken = null
        )
        tokenInfo.accessToken = try {
            AESUtil.encrypt(secret, JsonUtil.toJson(tokenInfo, formatted = false))
        } catch (ignore: Throwable) {
            logger.error("genToken|failed", ignore)
            throw ErrorCodeException(
                errorCode = PARAMETER_SECRET_ERROR,
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                defaultMessage = "genToken|failed because encoding failed ",
                params = arrayOf("encoding", "genToken|failed because encoding failed")
            )
        }
        return tokenInfo
    }

    private fun getTokenNoThrow(token: String): TokenInfo? {
        return try {
            val info = getTokenInfo(token)
            logger.debug("getTokenNoThrow|$info")
            return info
        } catch (ignore: Throwable) {
            null
        }
    }

    private fun getTokenInfo(token: String): TokenInfo {
        val result = try {
            AESUtil.decrypt(secret!!, token)
        } catch (ignore: Throwable) {
            logger.error("getTokenInfo|Access token illegal $token", ignore)
            throw ErrorCodeException(
                errorCode = PARAMETER_ILLEGAL_ERROR,
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                defaultMessage = "Access token illegal",
                params = arrayOf("token", "Access token illegal")
            )
        }
        try {
            val tokenInfo = JsonUtil.to(result, TokenInfo::class.java)
            return tokenInfo
        } catch (ignore: Throwable) {
            logger.error("getTokenInfo|Access token illegal $token", ignore)
            throw ErrorCodeException(
                errorCode = PARAMETER_VALIDATE_ERROR,
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                defaultMessage = "Access token invalid: ${ignore.message}",
                params = arrayOf("token", "Access token invalid: ${ignore.message}")
            )
        }
    }

    private fun genUserDetails(userId: String, deviceId: String) = "$userId;$deviceId"

    companion object {
        private val logger = LoggerFactory.getLogger(TrustDeviceService::class.java)
    }
}