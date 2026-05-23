package com.tencent.devops.common.security.crypto

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 加密密钥刷新任务配置。
 *
 * @property initialDelayMs 应用启动完成后延迟执行刷新任务的毫秒数。
 * @property batchSize 每批最多处理的数据行数。
 * @property sleepMsBetweenBatch 每批处理完成后的休眠毫秒数，用于控制迁移速率。
 */
@ConfigurationProperties(prefix = "aes.refresh")
data class CryptoKeyRefreshProperties(
    val initialDelayMs: Long = 10000L,
    val batchSize: Int = 500,
    val sleepMsBetweenBatch: Long = 0L
)
