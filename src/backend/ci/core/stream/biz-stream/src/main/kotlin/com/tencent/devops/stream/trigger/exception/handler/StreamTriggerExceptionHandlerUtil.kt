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

package com.tencent.devops.stream.trigger.exception.handler

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.exception.StreamTriggerBaseException
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.exception.StreamTriggerThirdException

@Suppress("ALL")
object StreamTriggerExceptionHandlerUtil {

    fun handleManualTrigger(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            val (errorCode, message) = when (e) {
                is OauthForbiddenException -> {
                    throw e
                }
                is ErrorCodeException -> {
                    Pair(ErrorCodeEnum.MANUAL_TRIGGER_THIRD_PARTY_ERROR, e.defaultMessage)
                }
                is StreamTriggerThirdException -> {
                    Pair(ErrorCodeEnum.MANUAL_TRIGGER_THIRD_PARTY_ERROR, e.message?.format(e.messageParams))
                }
                is StreamTriggerBaseException -> {
                    val (reason, realReasonDetail) = getReason(e)
                    if (reason == TriggerReason.UNKNOWN_ERROR.name) {
                        Pair(ErrorCodeEnum.MANUAL_TRIGGER_SYSTEM_ERROR, realReasonDetail)
                    } else {
                        Pair(ErrorCodeEnum.MANUAL_TRIGGER_USER_ERROR, realReasonDetail)
                    }
                }
                else -> {
                    Pair(ErrorCodeEnum.MANUAL_TRIGGER_SYSTEM_ERROR, e.message)
                }
            }
            throw ErrorCodeException(
                errorCode = errorCode.errorCode.toString(),
                defaultMessage = message
            )
        }
    }

    fun getReason(triggerE: StreamTriggerBaseException): Pair<String, String> {
        return when (triggerE) {
            is StreamTriggerException -> {
                Pair(
                    triggerE.triggerReason.name,
                    try {
                        // format在遇到不可解析的问题可能会报错
                        triggerE.triggerReason.detail.format(triggerE.reasonParams)
                    } catch (ignore: Throwable) {
                        triggerE.triggerReason.detail
                    }

                )
            }
            is StreamTriggerThirdException -> {
                val error = try {
                    val code = triggerE.errorCode.toInt()
                    ErrorCodeEnum.get(code)
                } catch (e: NumberFormatException) {
                    null
                }
                if (error == null) {
                    Pair("", triggerE.errorMessage ?: "")
                } else {
                    Pair(
                        error.name,
                        if (triggerE.errorMessage.isNullOrBlank()) {
                            error.getErrorMessage()
                        } else {
                            try {
                                triggerE.errorMessage.format(triggerE.messageParams)
                            } catch (ignore: Throwable) {
                                triggerE.errorMessage
                            }
                        }
                    )
                }
            }
            else -> Pair("", "")
        }
    }
}
