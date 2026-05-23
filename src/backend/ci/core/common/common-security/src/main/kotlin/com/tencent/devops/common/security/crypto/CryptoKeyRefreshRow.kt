package com.tencent.devops.common.security.crypto

/**
 * 加密密钥刷新任务中的单行数据抽象。
 */
interface CryptoKeyRefreshRow {
    /**
     * 返回数据行的业务唯一标识，用于日志和异常定位。
     */
    fun rowKey(): String

    /**
     * 返回数据行当前记录的密钥指纹。
     */
    fun keySha(): String?
}
