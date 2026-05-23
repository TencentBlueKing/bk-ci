package com.tencent.devops.common.security.crypto

import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener

/**
 * 加密密钥刷新启动监听器。
 */
class CryptoKeyRefreshStartup(
    private val applicationName: String,
    private val properties: CryptoKeyRefreshProperties,
    private val executor: CryptoKeyRefreshExecutor,
    private val writers: List<CryptoKeyRefreshWriter>
) : ApplicationListener<ApplicationReadyEvent> {
    private val started = AtomicBoolean(false)

    /**
     * 在应用启动完成后启动后台守护线程执行加密密钥刷新任务。
     */
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        if (!started.compareAndSet(false, true)) {
            return
        }
        Thread {
            try {
                Thread.sleep(properties.initialDelayMs)
                executor.runUntilAllDone(applicationName = applicationName, writers = writers)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Throwable) {
                logger.error("Crypto key refresh failed|applicationName=$applicationName", e)
            }
        }.apply {
            name = "crypto-key-refresh-$applicationName"
            isDaemon = true
            start()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CryptoKeyRefreshStartup::class.java)
    }
}
