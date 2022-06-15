/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.webhook.executor

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.webhook.dao.WebHookDao
import com.tencent.bkrepo.webhook.model.TWebHook
import org.slf4j.LoggerFactory
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * 事件消息消费者
 */
@Component("artifactEvent")
class ArtifactEventConsumer(
    private val threadPoolTaskExecutor: ThreadPoolTaskExecutor,
    private val webHookDao: WebHookDao,
    private val webHookExecutor: WebHookExecutor
) : Consumer<ArtifactEvent> {

    private val systemWebHookCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<EventType, List<TWebHook>>(CacheLoader.from { key -> webHookDao.findSystemWebHookByEventType(key) })

    override fun accept(event: ArtifactEvent) {
        logger.info("accept artifact event: $event")
        val task = Runnable { triggerWebHooks(event) }
        threadPoolTaskExecutor.execute(task)
    }

    fun triggerWebHooks(event: ArtifactEvent) {
        val webHookList = mutableListOf<TWebHook>()

        webHookList.addAll(systemWebHookCache.get(event.type))

        if (event.projectId.isNotBlank()) {
            webHookList.addAll(webHookDao.findProjectWebHookByEventType(event.projectId, event.type))
        }

        if (event.projectId.isNotBlank() && event.repoName.isNotBlank()) {
            webHookList.addAll(webHookDao.findRepoWebHookByEventType(event.projectId, event.repoName, event.type))
        }
        webHookExecutor.asyncExecutor(event, webHookList)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactEventConsumer::class.java)
    }
}
