package com.tencent.devops.remotedev.service.redis

object RedisKeys {
    const val REDIS_CALL_LIMIT_KEY_PREFIX = "remotedev:callLimit"
    const val REDIS_OP_HISTORY_KEY_PREFIX = "remotedev:opHistory:"

    // redis 选填项
    const val REDIS_DISCOUNT_TIME_KEY = "remotedev:discountTime" // 10560
    const val REDIS_DEFAULT_MAX_RUNNING_COUNT = "remotedev:defaultMaxRunningCount" // 1
    const val REDIS_DEFAULT_MAX_HAVING_COUNT = "remotedev:defaultMaxHavingCount" // 3
    const val REDIS_REMOTEDEV_GRAY_VERSION = "remotedev:gray:version"
    const val REDIS_REMOTEDEV_PROD_VERSION = "remotedev:prod:version"
    const val REDIS_REMOTEDEV_INACTIVE_TIME = "remotedev:inactiveTime"

    // redis必填项
    const val REDIS_OFFICIAL_DEVFILE_KEY = "remotedev:devfile"
    const val REDIS_WHITE_LIST_KEY = "remotedev:whiteList"
    const val REDIS_IP_LIST_KEY = "remotedev:ipSubnets"
}
