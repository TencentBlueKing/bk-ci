package com.tencent.devops.experience.service

import com.tencent.bkuser.ApiException
import com.tencent.bkuser.api.ProfilesApi
import com.tencent.bkuser.api.V1Api
import com.tencent.bkuser.model.Profile
import com.tencent.bkuser.model.ProfileLogin
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.constant.ExperienceMessageCode.ACCOUNT_HAS_BEEN_BLOCKED
import com.tencent.devops.experience.constant.ExperienceMessageCode.ACCOUNT_INFORMATION_ABNORMAL
import com.tencent.devops.experience.constant.ExperienceMessageCode.LOGIN_ACCOUNT_FREQUENT
import com.tencent.devops.experience.constant.ExperienceMessageCode.LOGIN_EXPIRED
import com.tencent.devops.experience.constant.ExperienceMessageCode.LOGIN_IP_FREQUENTLY
import com.tencent.devops.experience.constant.ExperienceMessageCode.UNABLE_GET_IP
import com.tencent.devops.experience.dao.ExperienceOuterLoginRecordDao
import com.tencent.devops.experience.pojo.outer.OuterLoginParam
import com.tencent.devops.experience.pojo.outer.OuterProfileVO
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.connection.RedisStringCommands
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.types.Expiration
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.ws.rs.core.Response

@Service
class ExperienceOuterService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val loginApi: V1Api,
    private val profileApi: ProfilesApi,
    private val experienceOuterLoginRecordDao: ExperienceOuterLoginRecordDao,
    private val dslContext: DSLContext
) {
    fun outerLogin(
        platform: Int,
        appVersion: String?,
        realIp: String,
        params: OuterLoginParam
    ): String {
        // IP黑名单
        if (isBlackIp(realIp)) {
            logger.warn("it is black ip : {}", realIp)
            throw ErrorCodeException(
                statusCode = Response.Status.UNAUTHORIZED.statusCode,
                errorCode = ExperienceMessageCode.OUTER_LOGIN_ERROR,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }

        // IP 限制频率
        if (isIpLimit(realIp)) {
            logger.warn("over limit , ip : {}", realIp)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = LOGIN_IP_FREQUENTLY,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
        }

        // 账号频率限制
        if (isAccountLimit(params.username)) {
            logger.warn("over limit , account : {}", params.username)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = LOGIN_ACCOUNT_FREQUENT,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }

        try {
            // 登录账号
            val data = ProfileLogin()
            data.username = params.username
            data.password = params.password
            data.domain = domain
            val profile = loginApi.v1LoginLogin(data)

            // 判断账号没有被封
            if (profile.status != Profile.StatusEnum.NORMAL) {
                logger.warn("profile status is not normal , status : {}", profile.status)
                throw ErrorCodeException(
                    statusCode = Response.Status.UNAUTHORIZED.statusCode,
                    errorCode = ACCOUNT_HAS_BEEN_BLOCKED,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            }

            // 设置token , 存放信息
            val outerProfileVO = OuterProfileVO(
                username = profile.username.replace("@$domain", ""),
                logo = logo(),
                email = profile.email
            )

            // 设置token
            val token = DigestUtils.md5Hex(profile.username + profile.id + UUIDUtil.generate())
            redisOperation.set(tokenRedisKey(token), JsonUtil.toJson(outerProfileVO), expireSecs)

            // 单token有效
            singleTokenLogin(profile.username, token)

            // 记录登录历史
            experienceOuterLoginRecordDao.add(
                dslContext = dslContext,
                username = profile.username,
                realIp = realIp,
                loginTime = LocalDateTime.now(),
                appVersion = appVersion ?: "",
                platform = platform
            )
            return token
        } catch (e: ApiException) {
            logger.warn("login error", e)
            throw ErrorCodeException(
                statusCode = e.code,
                errorCode = ExperienceMessageCode.OUTER_LOGIN_ERROR,
                defaultMessage = e.responseBody,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        } catch (e: Exception) {
            logger.warn("login bad request", e)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ExperienceMessageCode.OUTER_LOGIN_ERROR,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
    }

    fun outerAuth(token: String): OuterProfileVO {
        // token过期
        val profileStr = redisOperation.get(tokenRedisKey(token))
        if (StringUtils.isBlank(profileStr)) {
            logger.warn("get profile by token failed , token:{}", token)
            throw ErrorCodeException(
                statusCode = Response.Status.UNAUTHORIZED.statusCode,
                errorCode = LOGIN_EXPIRED,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }

        try {
            // 获取账号信息
            val profileVO = JsonUtil.getObjectMapper().readValue<OuterProfileVO>(profileStr, OuterProfileVO::class.java)

            // TOKEN对应的账号是否正常
            checkNormal(token, profileVO)

            return profileVO
        } catch (e: Exception) {
            logger.warn("decode profile failed , token:{}", token, e)
            throw ErrorCodeException(
                statusCode = Response.Status.UNAUTHORIZED.statusCode,
                errorCode = ACCOUNT_INFORMATION_ABNORMAL,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
    }

    fun renewToken(token: String) {
        redisOperation.expire(tokenRedisKey(token), expireSecs)
    }

    fun outerList(projectId: String): List<String> {
        return profileApi.v2ProfilesList(
            null, null, 100000, listOf("username", "departments"), "domain", listOf(domain),
            null, null, null, null, null, null, null, null
        ).results
            .filter { it.departments?.filter { d -> d.fullName == projectId }?.any() ?: false }
            .map { it.username.replace("@$domain", "") }
    }

    fun isBlackIp(realIp: String?): Boolean {
        if (realIp == null) {
            logger.warn("Can not get client real ip")
            throw ErrorCodeException(
                statusCode = Response.Status.UNAUTHORIZED.statusCode,
                errorCode = UNABLE_GET_IP,
                language = I18nUtil.getLanguage()
            )
        }
        return redisOperation.isMember("e:out:l:black:ip", realIp)
    }

    // 一个账户只能占用一个token
    private fun singleTokenLogin(username: String, token: String) {
        val redisKey = "e:o:l:single:$username"
        val oldToken = redisOperation.getAndSet(redisKey, token, expireSecs + 300)
        if (null != oldToken) {
            redisOperation.delete(tokenRedisKey(oldToken))
        }
    }

    private fun checkNormal(token: String, profileVO: OuterProfileVO) {
        val checkKey = "e:out:check:$token"
        val checkNow = redisOperation.execute(RedisCallback {
            it.stringCommands().set(
                checkKey.toByteArray(),
                "0".toByteArray(),
                Expiration.seconds(30),
                RedisStringCommands.SetOption.SET_IF_ABSENT
            )
        })
        if (checkNow == true) {
            val profilesRead = try {
                profileApi.v2ProfilesRead("${profileVO.username}@$domain", "status", "username")
            } catch (e: Exception) {
                null
            }
            if (null == profilesRead || profilesRead.status != Profile.StatusEnum.NORMAL) {
                redisOperation.set(checkKey, "1") // 将缓存置为不正常用户
                logger.warn("v2ProfilesRead , status is not normal , token:{}", token)
                throw ErrorCodeException(
                    statusCode = Response.Status.UNAUTHORIZED.statusCode,
                    errorCode = ACCOUNT_HAS_BEEN_BLOCKED,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            }
        } else {
            val checkResult = redisOperation.get(checkKey)
            if (checkResult == "1") {
                logger.warn("v2ProfilesRead, redis , status is not normal , token:{}", token)
                throw ErrorCodeException(
                    statusCode = Response.Status.UNAUTHORIZED.statusCode,
                    errorCode = ACCOUNT_HAS_BEEN_BLOCKED,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            }
        }
    }

    private fun isIpLimit(realIp: String): Boolean {
        val nowMinute = LocalDateTime.now().plusMinutes(1).withSecond(0)
        val limitKey = "e:out:l:ip:$realIp:${df.format(nowMinute)}"
        val limit = redisOperation.increment(limitKey, 1)
        if (limit == 1L) {
            redisOperation.expire(limitKey, 60)
        }
        return (limit ?: 0) > 10 // 60s内只能登录10次
    }

    private fun isAccountLimit(username: String): Boolean {
        val nowMinute = LocalDateTime.now().plusMinutes(1).withSecond(0)
        val limitKey = "e:out:l:ip:$username:${df.format(nowMinute)}"
        val limit = redisOperation.increment(limitKey, 1)
        if (limit == 1L) {
            redisOperation.expire(limitKey, 60)
        }
        return (limit ?: 0) > 5 // 60s内只能登录5次
    }

    private fun tokenRedisKey(token: String) = "e:out:l:$token"
    private fun logo() = "${HomeHostUtil.outerServerHost()}/app/download/devops_app.png"

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceOuterService::class.java)
        private val df = DateTimeFormatter.ofPattern("HHmmss")
        private const val expireSecs: Long = 30 * 24 * 60 * 60
        private const val domain = "app.devops"
    }
}
