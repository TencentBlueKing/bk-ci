/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.event.listener

import com.tencent.bkrepo.common.artifact.constant.DEFAULT_STORAGE_KEY
import com.tencent.bkrepo.common.artifact.event.ArtifactReceivedEvent
import com.tencent.bkrepo.common.artifact.event.ArtifactResponseEvent
import com.tencent.bkrepo.common.artifact.metrics.ArtifactMetrics
import com.tencent.bkrepo.common.artifact.metrics.ArtifactTransferRecord
import com.tencent.bkrepo.common.artifact.metrics.InfluxMetricsExporter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue

/**
 * 构件传输事件监听器
 */
class ArtifactTransferListener(
    private val artifactMetrics: ArtifactMetrics,
    private val influxMetricsExporter: ObjectProvider<InfluxMetricsExporter>
) {

    private var queue = LinkedBlockingQueue<ArtifactTransferRecord>(QUEUE_LIMIT)

    @EventListener(ArtifactReceivedEvent::class)
    fun listen(event: ArtifactReceivedEvent) {
        with(event) {
            artifactMetrics.uploadedSizeCounter.increment(throughput.bytes.toDouble())
            artifactMetrics.uploadedConsumeTimer.record(throughput.duration)
            logger.info("Receive artifact file, $throughput.")

            val record = ArtifactTransferRecord(
                time = Instant.now(),
                type = ArtifactTransferRecord.RECEIVE,
                elapsed = throughput.time,
                bytes = throughput.bytes,
                average = throughput.average(),
                storage = storageCredentials?.key ?: DEFAULT_STORAGE_KEY,
                sha256 = artifactFile.getFileSha256()
            )
            queue.offer(record)
        }
    }

    @EventListener(ArtifactResponseEvent::class)
    fun listen(event: ArtifactResponseEvent) {
        with(event) {
            artifactMetrics.downloadedSizeCounter.increment(throughput.bytes.toDouble())
            artifactMetrics.downloadedConsumeTimer.record(throughput.duration)
            logger.info("Response artifact file, $throughput.")

            val record = ArtifactTransferRecord(
                time = Instant.now(),
                type = ArtifactTransferRecord.RESPONSE,
                elapsed = throughput.time,
                bytes = throughput.bytes,
                average = throughput.average(),
                storage = storageCredentials?.key ?: DEFAULT_STORAGE_KEY,
                sha256 = artifactResource.node?.sha256.orEmpty()
            )
            queue.offer(record)
        }
    }

    @Scheduled(fixedDelay = FIXED_DELAY, initialDelay = FIXED_DELAY)
    fun export() {
        val current = queue
        queue = LinkedBlockingQueue(QUEUE_LIMIT)
        influxMetricsExporter.ifAvailable?.export(current)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactTransferListener::class.java)

        /**
         * 队列大小限制
         */
        private const val QUEUE_LIMIT = 4096

        /**
         * 30s
         */
        private const val FIXED_DELAY = 30 * 1000L
    }
}
