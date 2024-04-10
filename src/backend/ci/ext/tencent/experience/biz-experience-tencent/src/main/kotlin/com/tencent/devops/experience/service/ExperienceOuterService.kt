package com.tencent.devops.experience.service

import com.tencent.bkuser.api.ProfilesApi
import com.tencent.bkuser.api.V1Api
import com.tencent.bkuser.model.Profile
import com.tencent.bkuser.model.ProfileLogin
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.ci.UserUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.constant.ExperienceMessageCode.ACCOUNT_HAS_BEEN_BLOCKED
import com.tencent.devops.experience.constant.ExperienceMessageCode.ACCOUNT_INFORMATION_ABNORMAL
import com.tencent.devops.experience.constant.ExperienceMessageCode.LOGIN_ACCOUNT_FREQUENT
import com.tencent.devops.experience.constant.ExperienceMessageCode.LOGIN_EXPIRED
import com.tencent.devops.experience.constant.ExperienceMessageCode.LOGIN_IP_FREQUENTLY
import com.tencent.devops.experience.constant.ExperienceMessageCode.OUTER_LOGIN_WRONG_PASSWORD
import com.tencent.devops.experience.constant.ExperienceMessageCode.UNABLE_GET_IP
import com.tencent.devops.experience.constant.ExperienceMessageCode.USER_NEED_TAI_ACCOUNT
import com.tencent.devops.experience.dao.ExperienceGroupOuterDao
import com.tencent.devops.experience.dao.ExperienceOuterLoginRecordDao
import com.tencent.devops.experience.pojo.outer.OuterCanAddParam
import com.tencent.devops.experience.pojo.outer.OuterCanAddVO
import com.tencent.devops.experience.pojo.outer.OuterLoginParam
import com.tencent.devops.experience.pojo.outer.OuterProfileVO
import com.tencent.devops.project.api.service.ServiceProjectResource
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.RedisStringCommands
import org.springframework.data.redis.core.types.Expiration
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.ws.rs.core.Response

@Service
class ExperienceOuterService @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val loginApi: V1Api,
    private val profileApi: ProfilesApi,
    private val experienceOuterLoginRecordDao: ExperienceOuterLoginRecordDao,
    private val groupOuterDao: ExperienceGroupOuterDao,
    private val dslContext: DSLContext
) {
    @Value("\${esb.code:#{null}}")
    val appCode: String? = null

    @Value("\${esb.secret:#{null}}")
    val appSecret: String? = null

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
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ExperienceMessageCode.OUTER_LOGIN_ERROR
            )
        }

        // IP 限制频率
        if (isIpLimit(realIp)) {
            logger.warn("over limit , ip : {}", realIp)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = LOGIN_IP_FREQUENTLY
            )
        }

        // 账号频率限制
        if (isAccountLimit(params.username)) {
            logger.warn("over limit , account : {}", params.username)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = LOGIN_ACCOUNT_FREQUENT
            )
        }

        try {
            var exception: Exception? = null
            var outerProfileVO: OuterProfileVO? = null
            // 先用太湖登录
            try {
                outerProfileVO = taiLogin(params)
            } catch (e: Exception) {
                exception = e
            }
            // 再用蓝鲸外部登录
            if (null == outerProfileVO) {
                try {
                    outerProfileVO = bkOuterLogin(params)
                    exception = null
                } catch (ignored: Exception) {
                }
            }
            // 没有一个正常登录,则抛异常
            if (null != exception) {
                throw exception
            }

            // 设置token
            val token = DigestUtils.md5Hex(outerProfileVO!!.username + outerProfileVO.email + UUIDUtil.generate())
            redisOperation.set(tokenRedisKey(token), JsonUtil.toJson(outerProfileVO), EXPIRE_SECS)

            // 单token有效
            singleTokenLogin(outerProfileVO.username, token)

            // 记录登录历史
            experienceOuterLoginRecordDao.add(
                dslContext = dslContext,
                username = outerProfileVO.username,
                realIp = realIp,
                loginTime = LocalDateTime.now(),
                appVersion = appVersion ?: "",
                platform = platform
            )
            return token
        } catch (e: ErrorCodeException) {
            logger.warn("login error", e)
            throw ErrorCodeException(
                statusCode = e.statusCode,
                errorCode = e.errorCode
            )
        } catch (e: Exception) {
            logger.warn("login bad request", e)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ExperienceMessageCode.OUTER_LOGIN_ERROR
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
                errorCode = LOGIN_EXPIRED
            )
        }

        try {
            // 获取账号信息
            val profileVO = JsonUtil.getObjectMapper().readValue(profileStr, OuterProfileVO::class.java)

            // TOKEN对应的账号是否正常
            checkNormal(token, profileVO)

            return profileVO
        } catch (e: Exception) {
            logger.warn("decode profile failed , token:{}", token, e)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ACCOUNT_INFORMATION_ABNORMAL
            )
        }
    }

    fun renewToken(token: String) {
        redisOperation.expire(tokenRedisKey(token), EXPIRE_SECS)
    }

    fun outerList(projectId: String): List<String> {
        return profileApi.v2ProfilesList(
            null, null, 100000, listOf("username", "departments"), "domain", listOf(DOMAIN),
            null, null, null, null, null, null, null, null
        ).results
            .filter { it.departments?.any { d -> d.fullName == projectId } ?: false }
            .map { it.username.replace("@$DOMAIN", "") }
    }

    fun isBlackIp(realIp: String?): Boolean {
        if (realIp == null) {
            logger.warn("Can not get client real ip")
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = UNABLE_GET_IP
            )
        }
        return redisOperation.isMember("e:out:l:black:ip", realIp)
    }

    fun outerCanAdd(projectId: String, param: OuterCanAddParam): OuterCanAddVO {
        val userIds = param.userIds.split(",")
        val bkOuters = outerList(projectId)

        val legalUserIds = mutableListOf<String>()
        val illegalUserIds = mutableListOf<String>()

        for (u in userIds) {
            val isProjectUser = lazy {
                client.get(ServiceProjectResource::class).verifyUserProjectPermission(
                    projectCode = projectId,
                    userId = u
                ).data ?: false
            }
            if (UserUtil.isTaiUser(u) && isProjectUser.value) {
                legalUserIds.add(u)
            } else if (!UserUtil.isTaiUser(u) && bkOuters.contains(u)) {
                legalUserIds.add(u)
            } else {
                illegalUserIds.add(u)
            }
        }

        return OuterCanAddVO(legalUserIds, illegalUserIds)
    }

    // 一个账户只能占用一个token
    private fun singleTokenLogin(username: String, token: String) {
        val redisKey = "e:o:l:single:$username"
        val oldToken = redisOperation.getAndSet(redisKey, token, EXPIRE_SECS + 300)
        if (null != oldToken) {
            redisOperation.delete(tokenRedisKey(oldToken))
        }
    }

    private fun checkNormal(token: String, profileVO: OuterProfileVO) {
        if (profileVO.type == TYPE_TAI) {
            // 太湖账户没有异常体系
            return
        }
        val checkKey = "e:out:check:$token"
        val checkNow = redisOperation.execute {
            it.stringCommands().set(
                checkKey.toByteArray(),
                "0".toByteArray(),
                Expiration.seconds(30),
                RedisStringCommands.SetOption.SET_IF_ABSENT
            )
        }
        if (checkNow == true) {
            val profilesRead = try {
                profileApi.v2ProfilesRead("${profileVO.username}@$DOMAIN", "status", "username")
            } catch (e: Exception) {
                null
            }
            if (null == profilesRead || profilesRead.status != Profile.StatusEnum.NORMAL) {
                redisOperation.set(checkKey, "1") // 将缓存置为不正常用户
                logger.warn("v2ProfilesRead , status is not normal , token:{}", token)
                throw ErrorCodeException(
                    statusCode = Response.Status.BAD_REQUEST.statusCode,
                    errorCode = ACCOUNT_HAS_BEEN_BLOCKED
                )
            }
        } else {
            val checkResult = redisOperation.get(checkKey)
            if (checkResult == "1") {
                logger.warn("v2ProfilesRead, redis , status is not normal , token:{}", token)
                throw ErrorCodeException(
                    statusCode = Response.Status.BAD_REQUEST.statusCode,
                    errorCode = ACCOUNT_HAS_BEEN_BLOCKED
                )
            }
        }
    }

    private fun bkOuterLogin(params: OuterLoginParam): OuterProfileVO {
        // 登录账号
        val data = ProfileLogin()
        data.username = params.username
        data.password = params.password
        data.domain = DOMAIN
        val profile = loginApi.v1LoginLogin(data)

        // 判断账号没有被封
        if (profile.status != Profile.StatusEnum.NORMAL) {
            logger.warn("bkOuterLogin status is not normal , status : {}", profile.status)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ACCOUNT_HAS_BEEN_BLOCKED
            )
        }
        return OuterProfileVO(
            username = profile.username.replace("@$DOMAIN", ""),
            logo = logo(),
            email = profile.email,
            type = TYPE_BK_OUTER
        )
    }

    private fun taiLogin(params: OuterLoginParam): OuterProfileVO {
        val accountId = params.username.removeSuffix("@tai")
        val authorization = """{"bk_app_code":"$appCode","bk_app_secret":"$appSecret"}"""
        val requestBody = """{"account_id":"$accountId","password":"${params.password}"}"""
        val request = Request.Builder()
            .url("https://bk-unity-user.apigw.o.woa.com/prod/api/v1/open/odc-tai/user-credentials/authenticate/")
            .header("X-Bkapi-Authorization", authorization)
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body?.string() ?: "null"
            if (!response.isSuccessful) {
                logger.warn("taiLogin failed , body is $responseBody")
                throw ErrorCodeException(
                    statusCode = Response.Status.BAD_REQUEST.statusCode,
                    errorCode = ACCOUNT_INFORMATION_ABNORMAL
                )
            }
            val taiLogin = JsonUtil.to(responseBody, TaiLogin::class.java)
            val username = taiLogin.data.username
            if (!taiLogin.data.matched) {
                logger.warn("password error , username: $accountId")
                throw ErrorCodeException(
                    statusCode = Response.Status.BAD_REQUEST.statusCode,
                    errorCode = OUTER_LOGIN_WRONG_PASSWORD
                )
            }
            if (!username.endsWith("@tai")) {
                logger.warn("taiLogin is not support inner user , username: $username")
                throw ErrorCodeException(
                    statusCode = Response.Status.BAD_REQUEST.statusCode,
                    errorCode = USER_NEED_TAI_ACCOUNT
                )
            }
            return OuterProfileVO(
                username = username,
                logo = logo(),
                email = "",
                type = TYPE_TAI
            )
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

    data class TaiLogin(
        val data: Data
    ) {
        data class Data(
            val matched: Boolean,
            val username: String
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceOuterService::class.java)
        private val df = DateTimeFormatter.ofPattern("HHmmss")
        private const val EXPIRE_SECS: Long = 30 * 24 * 60 * 60
        private const val DOMAIN = "app.devops"
        private const val TYPE_BK_OUTER = 1
        private const val TYPE_TAI = 2
    }
}
