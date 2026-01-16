/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.mq

import com.tencent.devops.common.event.listener.EventListener
import com.tencent.devops.common.pipeline.ModelHandleService
import com.tencent.devops.common.pipeline.ModelVarReferenceHandleContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ModelVarReferenceListener @Autowired constructor(
    private val modelHandleService: ModelHandleService
) : EventListener<ModelVarReferenceEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(ModelVarReferenceListener::class.java)
    }

    override fun execute(event: ModelVarReferenceEvent) {
        try {
            logger.info(
                "Start processing variable reference event: userId=${event.userId}, " +
                "projectId=${event.projectId}, resourceId=${event.resourceId}, " +
                "resourceType=${event.resourceType}, resourceVersion=${event.resourceVersion}"
            )

            // 调用 handleModelVarReferences 方法处理变量引用
            modelHandleService.handleModelVarReferences(
                userId = event.userId,
                context = ModelVarReferenceHandleContext(
                    projectId = event.projectId,
                    resourceId = event.resourceId,
                    resourceType = event.resourceType,
                    resourceVersion = event.resourceVersion
                )
            )

            logger.info(
                "Successfully processed variable reference event: resourceId=${event.resourceId}, " +
                "resourceType=${event.resourceType}, resourceVersion=${event.resourceVersion}"
            )
        } catch (e: Throwable) {
            logger.warn(
                "Failed to process variable reference event: resourceId=${event.resourceId}, " +
                "resourceType=${event.resourceType}, resourceVersion=${event.resourceVersion}",
                e
            )
        }
    }
}