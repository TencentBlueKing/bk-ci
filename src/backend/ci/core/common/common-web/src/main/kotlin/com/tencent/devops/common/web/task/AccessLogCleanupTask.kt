package com.tencent.devops.common.web.task

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

import java.nio.file.Paths
import java.util.stream.Collectors

/**
 * 定时清理访问日志
 */
@SuppressWarnings("NestedBlockDepth")
class AccessLogCleanupTask(
    logDirectory: String
) {
    private val logDirectory: Path

    init {
        this.logDirectory = Paths.get(logDirectory)
    }

    @Scheduled(cron = "0 0 1 * * ?")
    fun cleanupLogs() {
        try {
            Files.list(logDirectory).use { stream ->
                val logFiles = stream.filter { path ->
                    val fileName = path.fileName.toString()
                    fileName.startsWith("access_log") && fileName != "access_log.log"
                }.sorted(Comparator.comparing { path -> -path.toFile().lastModified() }).collect(Collectors.toList())

                if (logFiles.size > MAX_LOG_FILES) {
                    for (i in MAX_LOG_FILES until logFiles.size) {
                        Files.delete(logFiles[i])
                        logger.info("Deleted old access log file: {}", logFiles[i])
                    }
                }
            }
        } catch (e: IOException) {
            logger.error("Error cleaning up access log files", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AccessLogCleanupTask::class.java)
        private const val MAX_LOG_FILES = 3 // 保留个数
    }
}
