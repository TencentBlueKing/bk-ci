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

package com.tencent.devops.stream.trigger.parsers

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubEvent
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitRequestEventNotBuildDao
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.streamActions.StreamMrAction
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitMrStatus
import com.tencent.devops.stream.trigger.mq.streamMrConflict.StreamMrConflictCheckDispatcher
import com.tencent.devops.stream.trigger.mq.streamMrConflict.StreamMrConflictCheckEvent
import com.tencent.devops.stream.trigger.service.StreamEventService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MergeConflictCheck @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val rabbitTemplate: RabbitTemplate,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val streamEventService: StreamEventService,
    private val streamGitConfig: StreamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MergeConflictCheck::class.java)
    }

    /**
     * 检查请求中是否有冲突
     * - 冲突通过请求详情获取，冲突检查为异步，需要通过延时队列轮训冲突检查结果
     * - 有冲突，不触发
     * - 没有冲突，进行后续操作
     */
    fun checkMrConflict(
        action: StreamMrAction,
        path2PipelineExists: Map<String, StreamTriggerPipeline>
    ): Boolean {
        val projectId = action.data.eventCommon.gitProjectId

        val mrInfo = action.tryGetMrInfoFromCache() ?: action.api.getMrInfo(
            gitProjectId = action.getGitProjectIdOrName(projectId),
            mrId = action.getMrId().toString(),
            cred = action.getGitCred(),
            retry = ApiRequestRetryInfo(retry = true)
        )!!

        // 通过查询当前merge请求的状态，unchecked说明未检查完，进入延迟队列
        // TODO: 其他源接入时再看检查冲突的逻辑，目前按照stream 逻辑
        when (mrInfo.mergeStatus) {
            TGitMrStatus.MERGE_STATUS_UNCHECKED.value -> {
                // 第一次未检查完则改变状态为正在检查供用户查看
                val recordId = streamEventService.saveTriggerNotBuildEvent(
                    action = action,
                    reason = TriggerReason.CI_MERGE_CHECKING.name,
                    reasonDetail = TriggerReason.CI_MERGE_CHECKING.detail
                )

                when (streamGitConfig.getScmType()) {
                    ScmType.CODE_GIT -> dispatchMrConflictCheck(
                        event = StreamMrConflictCheckEvent(
                            eventStr = objectMapper.writeValueAsString(action.data.event as GitEvent),
                            actionCommonData = action.data.eventCommon,
                            actionContext = action.data.context,
                            actionSetting = action.data.setting,
                            path2PipelineExists = path2PipelineExists,
                            notBuildRecordId = recordId
                        )
                    )
                    ScmType.GITHUB -> dispatchMrConflictCheck(
                        event = StreamMrConflictCheckEvent(
                            eventStr = objectMapper.writeValueAsString(action.data.event as GithubEvent),
                            actionCommonData = action.data.eventCommon,
                            actionContext = action.data.context,
                            actionSetting = action.data.setting,
                            path2PipelineExists = path2PipelineExists,
                            notBuildRecordId = recordId
                        )
                    )
                    else -> TODO("对接其他Git平台时需要补充")
                }

                return false
            }
            TGitMrStatus.MERGE_STATUS_CAN_NOT_BE_MERGED.value -> {
                logger.warn(
                    "MergeConflictCheck|checkMrConflict|git ci mr request has conflict" +
                        "|git project id|$projectId|mr request id|${action.getMrId()}"
                )
                throw StreamTriggerException(action, TriggerReason.CI_MERGE_CONFLICT)
            }
            // 没有冲突则触发流水线
            else -> return true
        }
    }

    // 检查是否存在冲突，供Rabbit Listener使用
    // 需要抓住里面的异常防止mq不停消费
    fun checkMrConflictByListener(
        action: StreamMrAction,
        // 是否是最后一次的检查
        isEndCheck: Boolean = false,
        notBuildRecordId: Long
    ): Pair<Boolean, Boolean> {
        var isFinish: Boolean
        var isTrigger: Boolean
        val projectId = action.data.eventCommon.gitProjectId
        val mrInfo = try {
            action.tryGetMrInfoFromCache() ?: action.api.getMrInfo(
                gitProjectId = action.getGitProjectIdOrName(projectId),
                mrId = action.getMrId().toString(),
                cred = action.getGitCred(),
                retry = ApiRequestRetryInfo(retry = true)
            )!!
        } catch (e: ErrorCodeException) {
            isFinish = true
            isTrigger = false
            gitRequestEventNotBuildDao.updateNoBuildReasonByRecordId(
                dslContext = dslContext,
                recordId = notBuildRecordId,
                reason = ErrorCodeEnum.GET_GIT_MERGE_INFO.name,
                reasonDetail = if (e.defaultMessage.isNullOrBlank()) {
                    ErrorCodeEnum.GET_GIT_MERGE_INFO.getErrorMessage()
                } else {
                    e.defaultMessage!!
                }
            )
            return Pair(isFinish, isTrigger)
        }
        when (mrInfo.mergeStatus) {
            TGitMrStatus.MERGE_STATUS_UNCHECKED.value -> {
                isFinish = false
                isTrigger = false
                // 如果最后一次检查还未检查完就是检查超时
                if (isEndCheck) {
                    // 超时走正常触发流程，以兼容工蜂unchecked 状态异常
                    gitRequestEventNotBuildDao.deleteNoBuildsById(
                        dslContext = dslContext,
                        recordId = notBuildRecordId
                    )
                    isFinish = true
                    isTrigger = true
                }
            }
            TGitMrStatus.MERGE_STATUS_CAN_NOT_BE_MERGED.value -> {
                logger.warn(
                    "MergeConflictCheck|checkMrConflictByListener|git ci mr request has conflict" +
                        "|git project id|$projectId|mr request id|${action.getMrId()}"
                )
                gitRequestEventNotBuildDao.updateNoBuildReasonByRecordId(
                    dslContext = dslContext,
                    recordId = notBuildRecordId,
                    reason = TriggerReason.CI_MERGE_CONFLICT.name,
                    reasonDetail = TriggerReason.CI_MERGE_CONFLICT.detail
                )
                isFinish = true
                isTrigger = false
            }
            else -> {
                gitRequestEventNotBuildDao.deleteNoBuildsById(
                    dslContext = dslContext,
                    recordId = notBuildRecordId
                )
                isFinish = true
                isTrigger = true
            }
        }
        return Pair(isFinish, isTrigger)
    }

    private fun dispatchMrConflictCheck(event: StreamMrConflictCheckEvent) {
        StreamMrConflictCheckDispatcher.dispatch(rabbitTemplate, event)
    }
}
