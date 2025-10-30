package com.tencent.devops.remotedev.constant

/**
 * 缩略图Redis Key常量
 */
object ThumbnailRedisKeys {
    const val THUMBNAIL_DOWNLOAD_PREFIX = "remotedev:thumbnail:download:"
    const val THUMBNAIL_UPLOAD_PREFIX = "remotedev:thumbnail:upload:"
    const val THUMBNAIL_TTL_SECONDS = 600 // 10分钟
}

/**
 * BkRepo常量
 */
object BkRepoConstants {
    const val REMOTE_DEV_REPO_NAME = "remotedev"
    const val REPO_TYPE = "GENERIC"
    const val REPO_CATEGORY = "LOCAL"
    const val ACCESS_CONTROL_MODE = "STRICT"
    const val TOKEN_EXPIRE_SECONDS = 660
    const val TOKEN_TYPE_DOWNLOAD = "DOWNLOAD"
    const val TOKEN_TYPE_UPLOAD = "UPLOAD"
    const val TOKEN_TYPE_ALL = "ALL"
}
