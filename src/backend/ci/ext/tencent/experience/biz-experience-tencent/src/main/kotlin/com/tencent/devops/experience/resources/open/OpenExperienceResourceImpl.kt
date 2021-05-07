package com.tencent.devops.experience.resources.open

import com.tencent.bkuser.ApiException
import com.tencent.bkuser.api.V1Api
import com.tencent.bkuser.model.ProfileLogin
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.open.OpenExperienceResource
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.pojo.outer.OuterLoginParam
import com.tencent.devops.experience.pojo.outer.OuterProfileVO
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class OpenExperienceResourceImpl @Autowired constructor(
    private val redisOperation: RedisOperation
) : OpenExperienceResource {
    override fun outerLogin(params: OuterLoginParam): Result<String> {
        // IP 限频 TODO
        val data = ProfileLogin()
        data.username = params.username
        data.password = params.password
        try {
            val profile = api.v1LoginLogin(data)
            val outerProfileVO = OuterProfileVO(
                username = profile.username + "_outer",
                logo = "https://www.tencent.com/img/index/tencent_logo.png"
            )

            val token = DigestUtils.md5Hex(profile.username + profile.id + System.currentTimeMillis() + secretKey)
            redisOperation.set(redisKey(token), JsonUtil.toJson(outerProfileVO), 30 * 24 * 60 * 60)
            return Result(token)
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

    override fun outerAuth(token: String): Result<OuterProfileVO> {
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
            val outerProfileVO = JsonUtil.getObjectMapper().readValue(profileStr, OuterProfileVO::class.java)
            return Result(outerProfileVO)
        } catch (e: Exception) {
            logger.warn("decode profile failed , token:{}", token, e)
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ExperienceMessageCode.OUTER_LOGIN_ERROR,
                defaultMessage = "登录过期,请重新登录"
            )
        }
    }

    private val api = V1Api()
    private fun redisKey(token: String?) = "e:out:l:$token"

    companion object {
        private val logger = LoggerFactory.getLogger(OpenExperienceResourceImpl::class.java)
        private const val secretKey = "sd&t6y978*)hU(g9712U^Y&*HJT^G()Yuihyuib{L"
    }
}
