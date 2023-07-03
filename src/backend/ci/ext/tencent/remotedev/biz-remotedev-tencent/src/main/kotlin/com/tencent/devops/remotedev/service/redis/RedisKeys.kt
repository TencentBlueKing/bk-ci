package com.tencent.devops.remotedev.service.redis

object RedisKeys {
    const val REDIS_CALL_LIMIT_KEY_PREFIX = "remotedev:callLimit"
    const val REDIS_OP_HISTORY_KEY_PREFIX = "remotedev:opHistory:"
    const val WORKSPACE_CACHE_KEY_PREFIX = "remotedev:workspaceCache:"

    // redis 选填项
    const val REDIS_DISCOUNT_TIME_KEY = "remotedev:discountTime" // 10000
    const val REDIS_DEFAULT_MAX_RUNNING_COUNT = "remotedev:defaultMaxRunningCount" // 1
    const val REDIS_DEFAULT_MAX_HAVING_COUNT = "remotedev:defaultMaxHavingCount" // 3
    const val REDIS_REMOTEDEV_GRAY_VERSION = "remotedev:gray:version"
    const val REDIS_REMOTEDEV_PROD_VERSION = "remotedev:prod:version"
    const val REDIS_REMOTEDEV_INACTIVE_TIME = "remotedev:inactiveTime"
    const val REDIS_WHITELIST_PERIOD = "remotedev:whitelistPeriod"
    const val REDIS_WHITE_LIST_GPU_KEY = "remotedev:whiteListGPU" // 云桌面创建限制
    const val REDIS_NOTICE_AHEAD_OF_TIME = "remotedev:noticeAheadOfTime" // 云桌面过期通知提前时间 默认60分钟
    const val REDIS_DESTRUCTION_RETENTION_TIME = "remotedev:destructionRetentionTime" // 云桌面销毁保留时间 默认3天

    // redis必填项
    const val REDIS_OFFICIAL_DEVFILE_KEY = "remotedev:devfile"
    const val REDIS_CHECKOUT_TEMPLATE_ID = "remotedev:checkoutTID" // 拉代码流水线模板id
    const val REDIS_WHITE_LIST_KEY = "remotedev:whiteList"
    const val REDIS_IP_LIST_KEY = "remotedev:ipSubnets"
    const val REDIS_DEFAULT_IMAGES_KEY = "remotedev:defaultImages"
}
