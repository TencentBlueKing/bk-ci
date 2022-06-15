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
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.webhook.config.WebHookProperties
import com.tencent.bkrepo.webhook.constant.AssociationType
import com.tencent.bkrepo.webhook.dao.WebHookDao
import com.tencent.bkrepo.webhook.model.TWebHook
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.regex.Pattern

/**
 * 事件消息消费者
 */
@Component("artifactEvent")
class ArtifactEventConsumer(
    private val webHookDao: WebHookDao,
    private val webHookExecutor: WebHookExecutor,
    private val webHookProperties: WebHookProperties
) : Consumer<Message<ArtifactEvent>> {

    private val executors = ThreadPoolExecutor(
        100,
        200,
        60,
        TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>(1024),
        ThreadFactoryBuilder().setNameFormat("webhook-event-worker-%d").build(),
        ThreadPoolExecutor.CallerRunsPolicy()
    )

    private val systemWebHookCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<EventType, List<TWebHook>>(CacheLoader.from { key ->
            webHookDao.findByAssociationTypeAndAssociationId(
                AssociationType.SYSTEM, null
            )
        })

    override fun accept(message: Message<ArtifactEvent>) {
        logger.info("accept artifact event: ${message.payload}, header: ${message.headers}")
        val task = Runnable { triggerWebHooks(message.payload) }
        executors.execute(task)
    }

    fun triggerWebHooks(event: ArtifactEvent) {
        val webHookList = mutableListOf<TWebHook>()

        if (!checkIfNeedTrigger(event)) {
            return
        }

        webHookList.addAll(systemWebHookCache.get(event.type))

        if (event.projectId.isNotBlank()) {
            webHookList.addAll(
                webHookDao.findByAssociationTypeAndAssociationId(
                    AssociationType.PROJECT, event.projectId
                )
            )
        }

        if (event.projectId.isNotBlank() && event.repoName.isNotBlank()) {
            val associationId = "${event.projectId}:${event.repoName}"
            webHookList.addAll(
                webHookDao.findByAssociationTypeAndAssociationId(
                    AssociationType.REPO, associationId
                )
            )
        }
        webHookList.filter {
            it.triggers.contains(event.type) && matchResourceKey(
                it.resourceKeyPattern,
                event.resourceKey
            )
        }
        logger.info("event: $event, webHookList: $webHookList")
        webHookExecutor.asyncExecutor(event, webHookList)
    }

    private fun checkIfNeedTrigger(event: ArtifactEvent): Boolean {
        val projectRepoKey = "${event.projectId}:${event.repoName}"
        webHookProperties.filterProjectRepoKey.forEach {
            val regex = Regex(it.replace("*", ".*"))
            if (projectRepoKey.matches(regex)) {
                return false
            }
        }
        return true
    }

    private fun matchResourceKey(resourceKeyPattern: String?, resourceKey: String): Boolean {
        if (resourceKeyPattern.isNullOrBlank()) {
            return true
        }
        return try {
            val pattern = Pattern.compile(resourceKeyPattern)
            val matcher = pattern.matcher(resourceKey)
            matcher.matches()
        } catch (e: Exception) {
            logger.warn("match resourceKey[$resourceKey] by pattern[$resourceKeyPattern] error, $e")
            false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactEventConsumer::class.java)
    }
}
