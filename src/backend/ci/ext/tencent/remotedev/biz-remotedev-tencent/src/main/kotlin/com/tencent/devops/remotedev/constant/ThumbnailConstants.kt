package com.tencent.devops.remotedev.constant

/**
 * 缩略图Redis Key常量
 */
object ThumbnailRedisKeys {
    const val THUMBNAIL_DOWNLOAD_PREFIX = "remotedev:thumbnail:download:"
    const val THUMBNAIL_UPLOAD_PREFIX = "remotedev:thumbnail:upload:"
    const val THUMBNAIL_UPLOAD_LIMIT_PREFIX = "remotedev:thumbnail:limit:" // 2s内同画质只允许上传一次, 低画质忽略，高画质插队
    const val THUMBNAIL_TTL_SECONDS = 600L // 10分钟
    const val THUMBNAIL_LIMIT_TTL_SECONDS = 2L // 2秒
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
