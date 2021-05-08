package com.tencent.devops.experience.service

import com.tencent.bkuser.ApiException
import com.tencent.bkuser.api.V1Api
import com.tencent.bkuser.model.ProfileLogin
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.pojo.outer.OuterLoginParam
import com.tencent.devops.experience.pojo.outer.OuterProfileVO
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.ws.rs.core.Response

@Service
class ExperienceOuterService @Autowired constructor(
    private val redisOperation: RedisOperation
) {
    fun outerLogin(realIp: String, params: OuterLoginParam): String {
        if (getIpLimit(realIp)) {
            logger.warn("over limit , ip : {}", realIp)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ExperienceMessageCode.OUTER_LOGIN_ERROR,
                defaultMessage = "登录错误"
            )
        }

        val data = ProfileLogin()
        data.username = params.username
        data.password = params.password
        try {
            val profile = api.v1LoginLogin(data)
            val outerProfileVO = OuterProfileVO(
                username = profile.username,
                logo = "https://www.tencent.com/img/index/tencent_logo.png"
            )

            val token = DigestUtils.md5Hex(profile.username + profile.id + System.currentTimeMillis() + secretKey)
            redisOperation.set(redisKey(token), JsonUtil.toJson(outerProfileVO), expireSecs)
            return token
        } catch (e: ApiException) {
            logger.warn("login error", e)
            throw ErrorCodeException(
                statusCode = e.code,
                errorCode = ExperienceMessageCode.OUTER_LOGIN_ERROR,
                defaultMessage = e.responseBody
            )
        } catch (e: Exception) {
            logger.warn("login bad request", e)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ExperienceMessageCode.OUTER_LOGIN_ERROR,
                defaultMessage = "登录错误"
            )
        }
    }

    fun outerAuth(token: String): OuterProfileVO {
        val profileStr = redisOperation.get(redisKey(token))
        if (StringUtils.isBlank(profileStr)) {
            logger.warn("get profile by token failed , token:{}", token)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ExperienceMessageCode.OUTER_LOGIN_ERROR,
                defaultMessage = "登录过期,请重新登录"
            )
        }
        try {
            return JsonUtil.getObjectMapper().readValue<OuterProfileVO>(profileStr, OuterProfileVO::class.java)
        } catch (e: Exception) {
            logger.warn("decode profile failed , token:{}", token, e)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ExperienceMessageCode.OUTER_LOGIN_ERROR,
                defaultMessage = "登录过期,请重新登录"
            )
        }
    }

    fun renewToken(token: String) {
        redisOperation.expire(redisKey(token), expireSecs)
    }

    private fun getIpLimit(realIp: String): Boolean {
        val nowMinute = LocalDateTime.now().plusMinutes(1).withSecond(0)
        val limitKey = "e:out:l:ip:$realIp:${df.format(nowMinute)}"
        val limit = redisOperation.increment(limitKey, 1)
        redisOperation.expire(limitKey, 60)
        return limit ?: 0 > 5
    }

    private val api = V1Api()
    private fun redisKey(token: String) = "e:out:l:$token"

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceOuterService::class.java)
        private const val secretKey = "sd&t6y978*)hU(g9712U^Y&*HJT^G()Yuihyuib{L"
        private val df = DateTimeFormatter.ofPattern("HHmmss")
        private const val expireSecs: Long = 30 * 24 * 60 * 60
    }
}
