package com.tencent.devops.remotedev.service.redis

object RedisKeys {
    const val REDIS_CALL_LIMIT_KEY_PREFIX = "remotedev:callLimit"
    const val REDIS_OP_HISTORY_KEY_PREFIX = "remotedev:opHistory:"

    // redis 选填项
    const val REDIS_DISCOUNT_TIME_KEY = "remotedev:discountTime" // 10000
    const val REDIS_1PASSWORD_EXPIRED_SECOND = "remotedev:1passwordExpiredSecond" // 一次性密钥过期时间，默认5秒
    const val REDIS_DEFAULT_AVAILABLE_TIME = "remotedev:defaultAvailableTime" // 云桌面默认可用时间 默认24h
    const val REDIS_PROJECT_WIN_COUNT_LIMIT = "remotedev:projectWinCountLimit" // 团队空间云桌面创建数量限制
    const val REDIS_CLIENT_INSTALL_URL = "remotedev:clientInstallUrl" // 蓝盾客户端更新地址
    const val REDIS_CLIENT_VERSION_CHECK = "remotedev:clientVersionCheck" // 是否校验蓝盾客户端版本
    const val REDIS_WORKING_ON_WEEKEND_DAY = "remotedev:holiday:workingDays" // 调休上班时间
    const val REDIS_HOLIDAY = "remotedev:holiday:holidays" // 休假时间

    const val REDIS_IP_LIST_KEY = "remotedev:ipSubnets"
}
