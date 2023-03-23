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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.constant.I18NConstant.BK_QUICK_APPROVAL_MOA
import com.tencent.devops.common.api.constant.I18NConstant.BK_QUICK_APPROVAL_PC
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.pojo.pipeline.ExtServiceMoaWorkItemReq
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.support.api.service.ServiceMessageApproveResource
import com.tencent.devops.support.model.approval.CompleteMoaWorkItemRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TXPipelineMoaService @Autowired constructor(
    private var client: Client,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService
) {
    private val logger = LoggerFactory.getLogger(TXPipelineMoaService::class.java)

    @Value("\${esb.appSecret}")
    private val appSecret = ""

    private val ignoredMoaMessage = listOf(
        MessageUtil.getMessageByLocale(
            messageCode = BK_QUICK_APPROVAL_MOA,
            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
        ),
        MessageUtil.getMessageByLocale(
            messageCode = BK_QUICK_APPROVAL_PC,
            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
        ))

    /**
     * 保证回调一次之后就结单
     */
    fun manualReviewMoaApprove(extServiceMoaWorkItemReq: ExtServiceMoaWorkItemReq): Result<Boolean> {
        val callBackData = extServiceMoaWorkItemReq.data.associate { it.key to it.value.first() }
        return try {
            doManualReviewMoaApprove(extServiceMoaWorkItemReq, callBackData)
        } catch (ignore: Throwable) {
            logger.info("manual review moa approve failed:${ignore.message}")
            Result(false)
        } finally {
            // 结单
            val reviewUsers = callBackData.getOrDefault("reviewUsers", "").split(",")
            reviewUsers.forEach {
                client.get(ServiceMessageApproveResource::class).createMoaWorkItemMessageComplete(
                    with(extServiceMoaWorkItemReq) {
                        CompleteMoaWorkItemRequest(
                            activity = activity,
                            category = category,
                            handler = it,
                            processInstId = processInstId,
                            processName = processName
                        )
                    }
                )
            }
        }
    }

    /**
     * moa 回调处理人工审核插件
     */
    @Suppress("NestedBlockDepth")
    fun doManualReviewMoaApprove(
        extServiceMoaWorkItemReq: ExtServiceMoaWorkItemReq,
        callBackData: Map<String, String>
    ): Result<Boolean> {
        logger.info("manualReviewMoaApprove|callBack=$extServiceMoaWorkItemReq")
        // 处理返回数据

        val projectId = callBackData.getOrDefault("projectId", "")
        val pipelineId = callBackData.getOrDefault("pipelineId", "")
        val buildId = callBackData.getOrDefault("buildId", "")
        val elementId = callBackData.getOrDefault("elementId", "")
        val callbackSignature = callBackData.getOrDefault("signature", "")
        checkParameter(projectId, pipelineId, buildId, elementId, callbackSignature)
        // 鉴定签名
        val signature = ShaUtils.sha256(projectId + buildId + elementId + appSecret)
        if (!ShaUtils.isEqual(signature, callbackSignature)) {
            logger.warn(
                "manual review MOA call back signature($callbackSignature) and " +
                    "generate signature ($signature) not match, ignore it."
            )
            return Result(false)
        }

        // 获取task状态
        val status = pipelineTaskService.getTaskStatus(
            projectId = projectId,
            buildId = buildId,
            taskId = elementId
        )
        if (status?.isRunning() == false) {
            logger.info(
                "manual review MOA call back task status($status) not REVIEWING, ignore it."
            )
            return Result(false)
        }
        val params = with(extServiceMoaWorkItemReq) {
            pipelineBuildFacadeService.goToReview(
                userId = handler,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                elementId = elementId
            ).apply {
                suggest = checkSuggest(submitOpinion)
                params.forEach {
                    val reviewValue = submitForm[checkParamName(it.chineseName) ?: it.key]
                    it.value = when (it.valueType) {
                        ManualReviewParamType.BOOLEAN -> reviewValue.toBoolean()
                        ManualReviewParamType.STRING -> reviewValue?.replace("\n", "\\n")
                        else -> reviewValue
                    }
                }
                this.status = when (submitAction) {
                    "agree" -> ManualReviewAction.PROCESS
                    "reject" -> ManualReviewAction.ABORT
                    else -> throw IllegalArgumentException("action $submitAction not support, ignore it.")
                }
            }
        }
        // 执行人工审核插件
        pipelineBuildFacadeService.buildManualReview(
            userId = extServiceMoaWorkItemReq.handler,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            elementId = elementId,
            params = params,
            channelCode = ChannelCode.BS,
            checkPermission = ChannelCode.isNeedAuth(ChannelCode.BS)
        )
        return Result(true)
    }

    private fun checkParameter(vararg args: String) {
        args.find { it.isBlank() }?.let { throw IllegalArgumentException("parameter is null or blank, ignore it.") }
    }

    private fun checkParamName(name: String?) = if (name.isNullOrBlank()) null else name

    private fun checkSuggest(suggest: String): String {
        var str = suggest
        for (s in ignoredMoaMessage) {
            str = str.replace(s, "")
        }
        return str
    }
}
