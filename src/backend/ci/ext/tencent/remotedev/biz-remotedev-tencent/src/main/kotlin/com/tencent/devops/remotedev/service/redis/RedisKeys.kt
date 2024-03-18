package com.tencent.devops.remotedev.service.redis

object RedisKeys {
    const val REDIS_CALL_LIMIT_KEY_PREFIX = "remotedev:callLimit"
    const val REDIS_OP_HISTORY_KEY_PREFIX = "remotedev:opHistory:"
    const val WORKSPACE_CACHE_KEY_PREFIX = "remotedev:workspaceCache:"
    fun notifyWinBeforeSleep(userId: String) = "remotedev:notifyWinBeforeSleep:$userId"

    // redis 选填项
    const val REDIS_DISCOUNT_TIME_KEY = "remotedev:discountTime" // 10000
    const val REDIS_DEFAULT_MAX_RUNNING_COUNT = "remotedev:defaultMaxRunningCount" // 1
    const val REDIS_DEFAULT_MAX_HAVING_COUNT = "remotedev:defaultMaxHavingCount" // 3
    const val REDIS_REMOTEDEV_GRAY_VERSION = "remotedev:gray:version"
    const val REDIS_REMOTEDEV_PROD_VERSION = "remotedev:prod:version"
    const val REDIS_REMOTEDEV_INACTIVE_TIME = "remotedev:inactiveTime"
    const val REDIS_WHITELIST_PERIOD = "remotedev:whitelistPeriod"
    const val REDIS_1PASSWORD_EXPIRED_SECOND = "remotedev:1passwordExpiredSecond" // 一次性密钥过期时间，默认5秒
    const val REDIS_RUNS_ON_OS_KEY = "remotedev:runsOnOS" // 云桌面创建限制
    const val REDIS_NOTICE_AHEAD_OF_TIME = "remotedev:noticeAheadOfTime" // 云桌面过期通知提前时间 默认60分钟
    const val REDIS_DEFAULT_AVAILABLE_TIME = "remotedev:defaultAvailableTime" // 云桌面默认可用时间 默认24h
    const val REDIS_DESTRUCTION_RETENTION_TIME = "remotedev:destructionRetentionTime" // 云桌面销毁保留时间 默认3天
    const val REDIS_PROJECT_WIN_COUNT_LIMIT = "remotedev:projectWinCountLimit" // 团队空间云桌面创建数量限制
    const val REDIS_CLIENT_INSTALL_URL = "remotedev:clientInstallUrl" // 蓝盾客户端更新地址
    const val REDIS_CLIENT_VERSION_CHECK = "remotedev:clientVersionCheck" // 是否校验蓝盾客户端版本
    const val REDIS_WORKING_ON_WEEKEND_DAY = "remotedev:holiday:workingDays" // 调休上班时间
    const val REDIS_HOLIDAY = "remotedev:holiday:holidays" // 休假时间
    const val REDIS_WORKSPACE_AUTO_DELETE_WHITE_LIST_PROJECT = "remotedev:autoDeleteWhiteListProject" // 云桌面自动销毁白名单
    const val REDIS_WINDOWS_HIGH_END_MODEL = "remotedev:windowsHighEndModel" // 云桌面高配机型白名单

    // redis必填项
    const val REDIS_OFFICIAL_DEVFILE_KEY = "remotedev:devfile"
    const val REDIS_CHECKOUT_TEMPLATE_ID = "remotedev:checkoutTID" // 拉代码流水线模板id
    const val REDIS_IP_LIST_KEY = "remotedev:ipSubnets"
    const val REDIS_DEFAULT_IMAGES_KEY = "remotedev:defaultImages"
}
