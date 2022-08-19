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
import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.streamActions.StreamRepoTriggerAction
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerBaseException
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.exception.StreamTriggerThirdException
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.service.StreamEventService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Stream触发阶段统一异常处理
 * 将拿到的异常按规则 做入库，发commit check等操作
 * 因为需要对未知异常全部捕获并保留信息，所以只能使用手动函数去抓
 */
@Suppress("ALL")
@Service
class StreamTriggerExceptionHandler @Autowired constructor(
    private val streamEventService: StreamEventService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamTriggerExceptionHandler::class.java)
    }

    fun <T> handle(
        action: BaseAction,
        f: () -> T?
    ): T? {
        try {
            return f()
        } catch (e: Throwable) {
            return try {
                when (e) {
                    is ErrorCodeException -> handleErrorCodeException(action, e)
                    is StreamTriggerBaseException -> handleStreamTriggerException(e)
                    else -> {
                        logger.warn("StreamTriggerExceptionHandler|Unknown error|action|${action.format()}", e)
                        // 非已知触发异常只保存
                        streamEventService.saveTriggerNotBuildEvent(
                            action = action,
                            reason = TriggerReason.UNKNOWN_ERROR.name,
                            reasonDetail = TriggerReason.UNKNOWN_ERROR.detail.format(e.message)
                        )
                        null
                    }
                }
            } catch (e: Throwable) {
                // 防止Hanlder处理过程中报错，兜底
                logger.error("BKSystemErrorMonitor|StreamTriggerExceptionHandler|action|${action.format()}", e)
                return null
            }
        }
    }

    /**
     * 其他微服务调用的部分异常会被整合为errorCodeEx，这里做保守处理
     */
    private fun handleErrorCodeException(action: BaseAction, e: ErrorCodeException): Nothing? {
        return handleStreamTriggerException(
            StreamTriggerThirdException(
                action = action,
                reasonParams = e.params?.toList(),
                commitCheck = CommitCheck(
                    block = false,
                    state = StreamCommitCheckState.FAILURE
                ),
                errorCode = e.errorCode,
                errorMessage = e.defaultMessage,
                messageParams = e.params?.toList()
            )
        )
    }

    /**
     * 对已知触发异常做逻辑处理
     */
    private fun handleStreamTriggerException(e: StreamTriggerBaseException): Nothing? {
        if (e is StreamTriggerException && e.triggerReason == TriggerReason.UNKNOWN_ERROR) {
            // 对Unknow error 打印日志方便排查
            logger.warn("StreamTriggerExceptionHandler|Unknown error|action|${e.action.format()}", e)
        }
        val action = e.action
        val commitCheck = e.commitCheck

        // 手动触发的异常不需要入库，需要直接返回给用户
        if (e.action.metaData.streamObjectKind == StreamObjectKind.MANUAL) {
            throw e
        }

        val (realReason, realReasonDetail) = StreamTriggerExceptionHandlerUtil.getReason(e)

        // 对于有流水线信息的需要发送commit check, 没有的直接保存即可
        if (action.data.context.pipeline == null) {
            streamEventService.saveTriggerNotBuildEvent(
                action = action,
                reason = realReason,
                reasonDetail = realReasonDetail
            )
        } else {
            streamEventService.saveBuildNotBuildEvent(
                action = action,
                reason = realReason,
                reasonDetail = realReasonDetail,
                sendCommitCheck = commitCheck != null && action !is StreamRepoTriggerAction,
                commitCheckBlock = commitCheck?.block ?: false,
                version = "v2.0"
            )
        }

        return null
    }
}
