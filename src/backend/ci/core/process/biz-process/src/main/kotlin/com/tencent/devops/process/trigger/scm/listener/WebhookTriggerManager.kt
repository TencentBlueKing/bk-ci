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

package com.tencent.devops.process.trigger.scm.listener

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WebhookTriggerManager @Autowired constructor(
    private val webhookTriggerListeners: List<WebhookTriggerListener>,
    private val pipelineYamlChangeListeners: List<PipelineYamlChangeListener>
) {

    fun fireBuildSuccess(context: WebhookTriggerContext) {
        webhookTriggerListeners.forEach { listener ->
            listener.onBuildSuccess(context = context)
        }
    }

    fun fireError(context: WebhookTriggerContext, exception: Exception) {
        webhookTriggerListeners.forEach { listener ->
            listener.onError(context = context, exception = exception)
        }
    }

    fun fireMatchFailed(context: WebhookTriggerContext) {
        webhookTriggerListeners.forEach { listener ->
            listener.onMatchFailed(context = context)
        }
    }

    fun fireChangeSuccess(context: PipelineYamlChangeContext) {
        pipelineYamlChangeListeners.forEach { listener ->
            listener.onChangeSuccess(context = context)
        }
    }

    fun fireChangeError(context: PipelineYamlChangeContext, exception: Exception) {
        pipelineYamlChangeListeners.forEach { listener ->
            listener.onChangeError(context = context, exception = exception)
        }
    }
}
