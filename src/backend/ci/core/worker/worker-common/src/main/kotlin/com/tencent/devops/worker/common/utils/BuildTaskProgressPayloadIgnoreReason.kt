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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.worker.common.constants.WorkerMessageCode
import com.tencent.devops.worker.common.env.AgentEnv

/**
 * `::set-progress-rate` 非法 payload 的忽略原因，与 [WorkerMessageCode] 中的 i18n key 一一对应。
 */
enum class BuildTaskProgressPayloadIgnoreReason(
    val messageCode: String
) {
    EMPTY_PAYLOAD(WorkerMessageCode.BK_PROGRESS_RATE_EMPTY_PAYLOAD),
    PAYLOAD_TOO_LARGE(WorkerMessageCode.BK_PROGRESS_RATE_PAYLOAD_TOO_LARGE),
    INVALID_NUMBER(WorkerMessageCode.BK_PROGRESS_RATE_INVALID_NUMBER),
    INVALID_NUMBER_RANGE(WorkerMessageCode.BK_PROGRESS_RATE_INVALID_NUMBER_RANGE),
    INVALID_JSON_OR_SCHEMA(WorkerMessageCode.BK_PROGRESS_RATE_INVALID_JSON_OR_SCHEMA),
    INVALID_FORMAT(WorkerMessageCode.BK_PROGRESS_RATE_INVALID_FORMAT);

    fun localizedMessage(): String =
        MessageUtil.getMessageByLocale(messageCode, AgentEnv.getLocaleLanguage())
}
