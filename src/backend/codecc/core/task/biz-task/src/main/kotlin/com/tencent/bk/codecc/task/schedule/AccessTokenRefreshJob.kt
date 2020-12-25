package com.tencent.bk.codecc.task.schedule

import com.tencent.devops.common.auth.api.pojo.external.KEY_BACKEND_ACCESS_TOKEN
import com.tencent.devops.common.redis.lock.RedisLock
import com.tencent.devops.common.util.OkhttpUtils
import org.apache.commons.lang.StringUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration       //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling    // 2.开启定时任务
class AccessTokenRefreshJob @Autowired constructor(
        private val redisTemplate: RedisTemplate<String, String>
){

    @Value("\${auth.url:#{null}}")
    private var authUrl: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(AccessTokenRefreshJob::class.java)

        private val KEY_ACCESS_TOKEN_UPDATE_TIME = "ACCESS_TOKEN_UPDATE_TIME"

        private val TIME_OUT_MILLIS = 1000 * 60 * 5

        private val REFRESH_ACCESS_TOKEN_LOCK_KEY = "REFRESH_ACCESS_TOKEN"
    }

    //3.添加定时任务
    @Scheduled(fixedRate = 1000 * 60 * 5)
    fun initAccessToken() {
        val currentTime = System.currentTimeMillis()

        // 刷新时间未超过5分钟不需要重复刷新
        if (!accessTokenIsTimeOut(currentTime)){
            return
        }

        // 加分布式锁刷新token
        val redisLock = RedisLock(
                redisTemplate,
                REFRESH_ACCESS_TOKEN_LOCK_KEY,
                20
        )
        try {
            if (redisLock.tryLock()) {
                // 刷新时间未超过5分钟不需要重复刷新
                if (!accessTokenIsTimeOut(currentTime)){
                    return
                }

                //蓝盾鉴权中心服务地址
                val app_code = "codecc"
                val app_secret = "SuCVfidl5UVbP14Q7T7ENKz391TI44f7WWxaYpFvO0Kl60J7Iz"
                val env_name = "prod"

                val id_provider_nologin = "client"
                val grant_type_nologin = "client_credentials"
                val reqJson = JSONObject()
                reqJson.put("env_name", env_name)
                reqJson.put("app_code", app_code)
                reqJson.put("app_secret", app_secret)
                reqJson.put("grant_type", grant_type_nologin)
                reqJson.put("id_provider", id_provider_nologin)

                val pass_auth_token_url = "$authUrl/oauth/token"
                val response = OkhttpUtils.doHttpPost(pass_auth_token_url, reqJson.toString(), emptyMap())

                var access_token: String? = null
                try {
                    val rspJson = JSONObject(response)
                    access_token = rspJson.getJSONObject("data").getString("access_token")
                } catch (e: Exception) {
                    logger.error("Get PaaS Access Token Failed!", response)
                }

                if (!StringUtils.isEmpty(access_token)) {
                    // 刷新token
                    redisTemplate.opsForValue().set(KEY_BACKEND_ACCESS_TOKEN, access_token)
                    logger.info("refresh bs access token finished!")

                    // 设置刷新时间
                    redisTemplate.opsForValue().set(KEY_ACCESS_TOKEN_UPDATE_TIME, currentTime.toString())
                } else {
                    logger.info("refresh bs access token failed!")
                }
            }
        }
        finally {
            redisLock.unlock()
        }
    }

    private fun accessTokenIsTimeOut(currentTime: Long): Boolean {
        val accessTokenUpdateTime = redisTemplate.opsForValue().get(KEY_ACCESS_TOKEN_UPDATE_TIME)

        if (accessTokenUpdateTime != null && currentTime - accessTokenUpdateTime.toLong() < TIME_OUT_MILLIS){
            return false
        }
        else {
            return true
        }
    }
}