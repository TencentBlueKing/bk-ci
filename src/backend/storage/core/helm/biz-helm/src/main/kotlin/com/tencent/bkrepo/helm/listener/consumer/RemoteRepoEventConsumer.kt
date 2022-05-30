/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.helm.listener.consumer

import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import java.util.function.Consumer
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

/**
 * 构件事件消费者，用于实时同步
 * 对应destination为对应ArtifactEvent.topic
 */
@Component("remoteRepo")
class RemoteRepoEventConsumer(
    private val remoteEventJobExecutor: RemoteEventJobExecutor
) : Consumer<Message<ArtifactEvent>> {

    /**
     * 允许接收的事件类型
     */
    private val acceptTypes = setOf(
        EventType.REPO_CREATED,
        EventType.REPO_UPDATED,
        EventType.REPO_REFRESHED
    )

    override fun accept(message: Message<ArtifactEvent>) {
        if (!acceptTypes.contains(message.payload.type)) {
            return
        }
        // TODO 针对有些特定场景需要验证消息是否重复消费
        logger.info("current helm message header is ${message.headers}")
        remoteEventJobExecutor.execute(message.payload)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteRepoEventConsumer::class.java)
    }
}
