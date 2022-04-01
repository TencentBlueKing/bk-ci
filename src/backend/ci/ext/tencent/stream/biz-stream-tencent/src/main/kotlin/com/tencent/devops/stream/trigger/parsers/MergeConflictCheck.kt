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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.common.exception.TriggerException
import com.tencent.devops.stream.common.exception.TriggerThirdException
import com.tencent.devops.stream.dao.GitRequestEventNotBuildDao
import com.tencent.devops.stream.mq.streamMrConflict.GitCIMrConflictCheckDispatcher
import com.tencent.devops.stream.mq.streamMrConflict.GitCIMrConflictCheckEvent
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.enums.GitCiMergeStatus
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.stream.trigger.GitCIEventService
import com.tencent.devops.stream.trigger.exception.TriggerExceptionService
import com.tencent.devops.stream.v2.service.StreamScmService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MergeConflictCheck @Autowired constructor(
    private val dslContext: DSLContext,
    private val rabbitTemplate: RabbitTemplate,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val streamScmService: StreamScmService,
    private val gitCIEventService: GitCIEventService,
    private val triggerExceptionService: TriggerExceptionService
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
    @Throws(TriggerException::class, TriggerThirdException::class)
    fun checkMrConflict(
        projectId: Long,
        gitRequestEventForHandle: GitRequestEventForHandle,
        event: GitEvent,
        path2PipelineExists: Map<String, GitProjectPipeline>,
        gitProjectConf: GitCIBasicSetting,
        gitToken: String
    ): Boolean {
        logger.info("get token form scm, token: $gitToken")

        val mrRequestId = (event as GitMergeRequestEvent).object_attributes.id

        val mrInfo = triggerExceptionService.handleErrorCode(
            request = gitRequestEventForHandle,
            action = {
                streamScmService.getMergeInfo(
                    gitProjectId = projectId,
                    mergeRequestId = mrRequestId,
                    token = gitToken
                )
            }
        )!!
        // 通过查询当前merge请求的状态，unchecked说明未检查完，进入延迟队列
        when (mrInfo.mergeStatus) {
            GitCiMergeStatus.MERGE_STATUS_UNCHECKED.value -> {
                // 第一次未检查完则改变状态为正在检查供用户查看
                val recordId = gitCIEventService.saveTriggerNotBuildEvent(
                    userId = gitRequestEventForHandle.userId,
                    eventId = gitRequestEventForHandle.id!!,
                    reason = TriggerReason.CI_MERGE_CHECKING.name,
                    reasonDetail = TriggerReason.CI_MERGE_CHECKING.detail,
                    gitProjectId = gitRequestEventForHandle.gitProjectId,
                    branch = gitRequestEventForHandle.branch
                )

                dispatchMrConflictCheck(
                    GitCIMrConflictCheckEvent(
                        token = gitToken,
                        gitRequestEventForHandle = gitRequestEventForHandle,
                        event = event,
                        path2PipelineExists = path2PipelineExists,
                        gitProjectConf = gitProjectConf,
                        notBuildRecordId = recordId
                    )
                )
                return false
            }
            GitCiMergeStatus.MERGE_STATUS_CAN_NOT_BE_MERGED.value -> {
                logger.warn("git ci mr request has conflict , git project id: $projectId, mr request id: $mrRequestId")
                TriggerException.triggerError(
                    request = gitRequestEventForHandle,
                    reason = TriggerReason.CI_MERGE_CONFLICT
                )
            }
            // 没有冲突则触发流水线
            else -> return true
        }
    }

    // 检查是否存在冲突，供Rabbit Listener使用
    // todo: 由于是update所以先不用handle做异常统一处理，后续优化
    fun checkMrConflictByListener(
        token: String,
        gitRequestEventForHandle: GitRequestEventForHandle,
        event: GitEvent,
        path2PipelineExists: Map<String, GitProjectPipeline>,
        gitProjectConf: GitCIBasicSetting,
        // 是否是最后一次的检查
        isEndCheck: Boolean = false,
        notBuildRecordId: Long
    ): Pair<Boolean, Boolean> {
        var isFinish: Boolean
        var isTrigger: Boolean
        val projectId = gitRequestEventForHandle.gitRequestEvent.gitProjectId
        val mrRequestId = (event as GitMergeRequestEvent).object_attributes.id
        val mrInfo = try {
            streamScmService.getMergeInfo(
                gitProjectId = projectId,
                mergeRequestId = mrRequestId,
                token = token
            )
        } catch (e: ErrorCodeException) {
            isFinish = true
            isTrigger = false
            gitRequestEventNotBuildDao.updateNoBuildReasonByRecordId(
                dslContext = dslContext,
                recordId = notBuildRecordId,
                reason = ErrorCodeEnum.GET_GIT_MERGE_INFO.name,
                reasonDetail = if (e.defaultMessage.isNullOrBlank()) {
                    ErrorCodeEnum.GET_GIT_MERGE_INFO.formatErrorMessage
                } else {
                    e.defaultMessage!!
                }
            )
            return Pair(isFinish, isTrigger)
        }
        when (mrInfo.mergeStatus) {
            GitCiMergeStatus.MERGE_STATUS_UNCHECKED.value -> {
                isFinish = false
                isTrigger = false
                // 如果最后一次检查还未检查完就是检查超时
                if (isEndCheck) {
                    // 第一次之后已经在not build中有数据了，修改构建原因
                    gitRequestEventNotBuildDao.updateNoBuildReasonByRecordId(
                        dslContext = dslContext,
                        recordId = notBuildRecordId,
                        reason = TriggerReason.CI_MERGE_CHECK_TIMEOUT.name,
                        reasonDetail = TriggerReason.CI_MERGE_CHECK_TIMEOUT.detail
                    )
                    isFinish = true
                    isTrigger = false
                }
            }
            GitCiMergeStatus.MERGE_STATUS_CAN_NOT_BE_MERGED.value -> {
                logger.warn("git ci mr request has conflict , git project id: $projectId, mr request id: $mrRequestId")
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

    private fun dispatchMrConflictCheck(event: GitCIMrConflictCheckEvent) {
        GitCIMrConflictCheckDispatcher.dispatch(rabbitTemplate, event)
    }
}
