package com.tencent.devops.stream.service

import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.repository.api.github.ServiceGithubOauthResource
import com.tencent.devops.stream.config.StreamLoginConfig
import com.tencent.devops.stream.pojo.enums.StreamLoginType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamLoginService @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val streamLoginConfig: StreamLoginConfig
) {
    companion object {
        private const val BK_LOGIN_CODE_KEY = "bk:login:third:%s:code:%s"
        private const val BK_LOGIN_CODE_KEY_EXPIRED = 60L
        private val logger = LoggerFactory.getLogger(StreamLoginService::class.java)
    }

    fun githubCallback(code: String, state: String?): String {
        logger.info("github callback|code:$code|state:$state")
        val githubCallback =
            client.get(ServiceGithubOauthResource::class)
                .githubCallback(code = code, state = state, channelCode = ChannelCode.GIT.name).data!!
        val authCode = UUIDUtil.generate()
        redisOperation.set(
            key = String.format(BK_LOGIN_CODE_KEY, StreamLoginType.GITHUB.value, githubCallback.userId),
            value = authCode,
            expiredInSecond = BK_LOGIN_CODE_KEY_EXPIRED
        )
        return String.format(
            streamLoginConfig.githubRedirectUrl,
            authCode,
            githubCallback.userId,
            StreamLoginType.GITHUB.value,
            githubCallback.email
        )
    }

    fun loginUrl(type: String): String {
        return when (type) {
            StreamLoginType.GITHUB.value ->
                client.get(ServiceGithubOauthResource::class).oauthUrl(
                    streamLoginConfig.githubRedirectUrl, null
                ).data!!
            else ->
                throw ClientException(message = "stream login not support $type type")
        }
    }
}
