package com.tencent.devops.common.security.crypto

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory

/**
 * 加密密钥刷新执行器。
 */
class CryptoKeyRefreshExecutor(
    private val redisOperation: RedisOperation,
    private val properties: CryptoKeyRefreshProperties
) {
    /**
     * 在分布式锁保护下执行所有刷新器，直到所有待刷新数据处理完成。
     *
     * @param applicationName 当前应用名称，用于生成分布式锁 key。
     * @param writers 当前应用注册的加密密钥刷新器列表。
     */
    fun runUntilAllDone(applicationName: String, writers: List<CryptoKeyRefreshWriter>) {
        RedisLock(
            redisOperation = redisOperation,
            lockKey = "crypto:key:refresh:$applicationName",
            expiredTimeInSeconds = LOCK_SECONDS
        ).use { lock ->
            if (!lock.tryLock()) {
                logger.info("Crypto key refresh is already running|applicationName=$applicationName")
                return
            }
            writers.forEach { writer ->
                logger.info("Crypto key refresh writer started|applicationName=$applicationName|writer=${writer.name}")
                runWriterUntilDone(applicationName = applicationName, writer = writer)
            }
        }
    }

    /**
     * 循环执行单个刷新器，直到该刷新器不再返回待处理数据。
     */
    private fun runWriterUntilDone(applicationName: String, writer: CryptoKeyRefreshWriter) {
        var page = 0
        var totalSuccess = 0
        var totalFailed = 0
        while (true) {
            val rows = writer.fetchBatch(properties.batchSize)
            if (rows.isEmpty()) {
                logger.info(
                    "Crypto key refresh writer done|applicationName=$applicationName|writer=${writer.name}|" +
                        "pages=$page|success=$totalSuccess|failed=$totalFailed"
                )
                return
            }
            page++
            var batchSuccess = 0
            var batchFailed = 0
            rows.forEach { row ->
                try {
                    writer.updateRow(row)
                    batchSuccess++
                } catch (e: Throwable) {
                    batchFailed++
                    logger.error(
                        "Crypto key refresh row failed|applicationName=$applicationName|" +
                            "writer=${writer.name}|row=${row.rowKey()}",
                        e
                    )
                }
            }
            totalSuccess += batchSuccess
            totalFailed += batchFailed
            logger.info(
                "Crypto key refresh batch done|applicationName=$applicationName|writer=${writer.name}|" +
                    "page=$page|rows=${rows.size}|success=$batchSuccess|failed=$batchFailed|" +
                    "totalSuccess=$totalSuccess|totalFailed=$totalFailed"
            )
            if (batchSuccess == 0) {
                logger.error(
                    "Crypto key refresh writer stopped without progress|applicationName=$applicationName|" +
                        "writer=${writer.name}|page=$page|rows=${rows.size}|totalFailed=$totalFailed"
                )
                return
            }
            if (properties.sleepMsBetweenBatch > 0) {
                Thread.sleep(properties.sleepMsBetweenBatch)
            }
        }
    }

    companion object {
        private const val LOCK_SECONDS = 600L
        private val logger = LoggerFactory.getLogger(CryptoKeyRefreshExecutor::class.java)
    }
}
