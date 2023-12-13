package com.tencent.devops.scm.constant

import java.util.concurrent.TimeUnit

/**
 * Stream 工蜂超级Token一些Redis常量，方便不同的业务服务共用
 */
object SteamGitTokenConstant {
    private const val STREAM_GIT_TOKEN_UPDATE_LOCK_PREFIX = "stream:git:token:lock:key:"
    private const val STREAM_GIT_TOKEN_PROJECT_PREFIX = "stream:git:project:token:"
    fun getGitTokenKey(gitProjectId: String) = STREAM_GIT_TOKEN_PROJECT_PREFIX + gitProjectId
    fun getGitTokenLockKey(gitProjectId: String) = STREAM_GIT_TOKEN_UPDATE_LOCK_PREFIX + gitProjectId

    // 工蜂超级token有效时间为8个小时，我们redis存7.5个小时，提前半个小时，这里token的使用都在初始化时间完成，不会超过30分钟
    val validTime = TimeUnit.MINUTES.toSeconds(30) + TimeUnit.HOURS.toSeconds(7)
}
