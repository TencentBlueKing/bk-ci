package com.tencent.bk.codecc.quartz.job

import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.devops.common.auth.api.pojo.external.KEY_BACKEND_ACCESS_TOKEN
import com.tencent.devops.common.util.OkhttpUtils
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang.StringUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate

@Slf4j
class RefreshAccessTokenScheduleTask @Autowired constructor(
        private val redisTemplate: RedisTemplate<String, String>
) : IScheduleTask {
    //蓝盾鉴权中心服务地址
    private val app_code = "codecc"
    private val app_secret = "SuCVfidl5UVbP14Q7T7ENKz391TI44f7WWxaYpFvO0Kl60J7Iz"
    private val env_name = "prod"

    private val id_provider_nologin = "client"
    private val grant_type_nologin = "client_credentials"

    override fun executeTask(quartzJobContext: QuartzJobContext) {
        val reqJson = JSONObject()
        reqJson.put("env_name", env_name)
        reqJson.put("app_code", app_code)
        reqJson.put("app_secret", app_secret)
        reqJson.put("grant_type", grant_type_nologin)
        reqJson.put("id_provider", id_provider_nologin)

        val jobCustomParam = quartzJobContext.jobCustomParam
        if (null == jobCustomParam) {
            logger.info("job custom param is null!")
            return
        }
        val authUrl = jobCustomParam["authUrl"] as String
        val pass_auth_token_url = authUrl + "/oauth/token"
        val response = OkhttpUtils.doHttpPost(pass_auth_token_url, reqJson.toString(), emptyMap())

        var access_token: String? = null
        try {
            val rspJson = JSONObject(response)
            access_token = rspJson.getJSONObject("data").getString("access_token")
        } catch (e: Exception) {
            logger.error("Get PaaS Access Token Failed!", response)
        }

        if (!StringUtils.isEmpty(access_token)) {
            redisTemplate.opsForValue().set(KEY_BACKEND_ACCESS_TOKEN, access_token!!)
            logger.info("refresh bs access token finished!")
        } else {
            logger.info("refresh bs access token failed!")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RefreshAccessTokenScheduleTask::class.java)
    }
}