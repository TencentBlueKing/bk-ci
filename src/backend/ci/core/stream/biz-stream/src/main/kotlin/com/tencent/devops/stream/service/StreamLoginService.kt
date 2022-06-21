package com.tencent.devops.stream.service

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.repository.api.github.ServiceGithubOauthResource
import com.tencent.devops.stream.config.StreamLoginConfig
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
    }

    fun githubCallback(code: String, state: String): String {
        val githubCallback =
            client.get(ServiceGithubOauthResource::class)
                .githubCallback(code = code, state = state, channelCode = ChannelCode.GIT.name).data!!
        val code = UUIDUtil.generate()
        redisOperation.set(
            key = String.format(BK_LOGIN_CODE_KEY, "github", githubCallback.userId),
            value = code,
            expiredInSecond = BK_LOGIN_CODE_KEY_EXPIRED
        )
        return String.format(streamLoginConfig.githubRedirectUrl, code, githubCallback.userId, "github")
    }
}
