package com.tencent.bkrepo.common.artifact.health

import com.tencent.bkrepo.common.api.util.HumanReadable
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@EnableConfigurationProperties(DiskSpaceHealthIndicatorProperties::class)
@Primary
@Component("diskSpaceHealthIndicator")
class LocalDiskSpaceHealthIndicator(properties: DiskSpaceHealthIndicatorProperties) :
    AbstractHealthIndicator(CHECK_FAILED_MESSAGE) {

    private val path = properties.path

    private val threshold = properties.threshold

    private val logger = LoggerFactory.getLogger(LocalDiskSpaceHealthIndicator::class.java)

    override fun doHealthCheck(builder: Health.Builder) {
        val diskFreeInBytes = path.usableSpace
        if (diskFreeInBytes >= threshold.toBytes()) {
            builder.up()
        } else {
            logger.error(
                String.format(
                    "Free disk space below threshold. Available: %d bytes (threshold: %s)",
                    diskFreeInBytes,
                    this.threshold
                )
            )
            builder.down()
        }
        builder.withDetail("total", HumanReadable.size(path.totalSpace))
            .withDetail("free", HumanReadable.size(diskFreeInBytes))
            .withDetail("threshold", HumanReadable.size(threshold.toBytes()))
            .withDetail("exists", path.exists())
            .withDetail("path", path)
    }

    companion object {
        private const val CHECK_FAILED_MESSAGE = "DiskSpace health check failed"
    }
}
