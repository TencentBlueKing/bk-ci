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

package com.tencent.bkrepo.replication.job

import com.tencent.bkrepo.replication.config.DEFAULT_REPLICA_SOURCE
import com.tencent.bkrepo.replication.config.DEFAULT_REPLICA_STREAM
import com.tencent.bkrepo.replication.handler.NodeEventConsumer
import com.tencent.bkrepo.replication.model.TOperateLog
import com.tencent.bkrepo.repository.pojo.log.OperateType
import com.tencent.bkrepo.repository.pojo.log.ResourceType
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer
import org.springframework.data.mongodb.core.messaging.MessageListener
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer
import org.springframework.data.mongodb.core.messaging.TailableCursorRequest
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * real time job tail from the caped collection
 */
@Service
class RealTimeJob {

    @Autowired
    private lateinit var template: MongoTemplate

    @Autowired
    private lateinit var nodeConsumer: NodeEventConsumer

    private lateinit var container: MessageListenerContainer

    @Value("\${replication.source:'log'}")
    private var source: String = DEFAULT_REPLICA_SOURCE

    @Scheduled(cron = "00 */30 * * * ?")
    fun ping() {
        if (!container.isRunning) {
            logger.error("container is not running")
        }
    }

    @Scheduled(initialDelay = 1000 * 40, fixedDelay = Long.MAX_VALUE)
    @SchedulerLock(name = "RealTimeJob", lockAtMostFor = "PT1H")
    fun run() {
        var isRunning = false
        while (true) {
            try {
                if (!isRunning) {
                    container = DefaultMessageListenerContainer(template)
                    if (source == DEFAULT_REPLICA_STREAM) {
                        val request = getChangeStreamRequest()
                        container.register(request, TOperateLog::class.java)
                    } else {
                        val request = getTailCursorRequest()
                        container.register(request, TOperateLog::class.java)
                    }
                    container.start()
                    logger.info("try to start status :[${container.isRunning}]")
                }
            } catch (ignored: Exception) {
                logger.error("fail to register container [${ignored.message}]")
            } finally {
                logger.info("container running status :[${container.isRunning}]")
                // get container running status an sleep try
                isRunning = container.isRunning
                if (isRunning) return
                Thread.sleep(retryInterVal)
            }
        }
    }

    private fun getChangeStreamRequest(): ChangeStreamRequest<Any> {
        val listener = buildListener()
        // val pipeline = Pipeline.build
        return ChangeStreamRequest.builder()
            .collection(collectionName)
            .publishTo(listener)
            .filter()
            .build()
    }

    private fun getTailCursorRequest(): TailableCursorRequest<Any> {
        val query = buildQuery()
        val listener = buildListener()
        return TailableCursorRequest.builder()
            .collection(collectionName)
            .filter(query)
            .publishTo(listener)
            .build()
    }

    private fun buildQuery(): Query {
        return Query.query(
            Criteria.where(TOperateLog::createdDate.name).gte(LocalDateTime.now()).and(TOperateLog::resourceType.name)
                .`is`(ResourceType.NODE)
        )
    }

    private fun buildListener(): MessageListener<Document, TOperateLog> {
        return MessageListener<Document, TOperateLog> {
            val body = it.body
            body?.let {
                when (body.operateType) {
                    OperateType.CREATE -> {
                        nodeConsumer.dealWithNodeCreateEvent(body.description)
                    }
                    OperateType.RENAME -> {
                        nodeConsumer.dealWithNodeRenameEvent(body.description)
                    }
                    OperateType.COPY -> {
                        nodeConsumer.dealWithNodeCopyEvent(body.description)
                    }
                    OperateType.MOVE -> {
                        nodeConsumer.dealWithNodeMoveEvent(body.description)
                    }
                    OperateType.DELETE -> {
                        nodeConsumer.dealWithNodeDeleteEvent(body.description)
                    }
                    OperateType.UPDATE -> {
                        nodeConsumer.dealWithNodeUpdateEvent(body.description)
                    }
                }
            }
        }
    }

    fun getContainerStatus(): Boolean {
        return this.container.isRunning
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RealTimeJob::class.java)
        const val collectionName = "operation_log"
        const val retryInterVal = 30000L
    }
}
