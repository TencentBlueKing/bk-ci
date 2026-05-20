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

package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.model.notify.tables.records.TCommonNotifyMessageTemplateRecord
import com.tencent.devops.notify.pojo.ImateBizContext
import com.tencent.devops.notify.pojo.ImateSendMessageRequest
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.service.ImateService
import com.tencent.devops.notify.service.ImateTemplateRenderer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * IMate 通知器：把 stream 后台的通知请求翻译成 IMate API 入参。
 *
 * 关键设计：
 * - **模板源在 stream 后台**（classpath HTML），由 [ImateTemplateRenderer] 本地渲染好最终内容后发送；
 * - 业务上下文（projectId/pipelineId/buildId/stageId/groupId/executeCount）通过 bodyParams 中的
 *   `IMATE_CTX_*` keys 透传，再填入 [ImateSendMessageRequest.bizContext]，
 *   IMate 必须保存并在审核按钮回调中原样回传给 stream 后台的 Open 接口；
 * - templateCode 优先从 bodyParams[IMATE_TEMPLATE_CODE_KEY] 取（允许业务方按场景覆盖），
 *   缺失时回退到 SendNotifyMessageTemplateRequest.templateCode。
 */
@Component
class ImateNotifier @Autowired constructor(
    private val imateService: ImateService,
    private val imateTemplateRenderer: ImateTemplateRenderer
) : INotifier {

    override fun type(): NotifyType = NotifyType.IMATE

    override fun send(
        request: SendNotifyMessageTemplateRequest,
        commonNotifyMessageTemplateRecord: TCommonNotifyMessageTemplateRecord
    ) {
        if (!imateService.enabled()) {
            logger.info("[IMATE] notifier disabled (no api url configured), skip.")
            return
        }
        val sessions = parseSessions(request.bodyParams)
        if (sessions.isEmpty()) {
            logger.info("[IMATE] no sessionId in bodyParams, skip. templateCode=${request.templateCode}")
            return
        }

        val templateCode = request.bodyParams?.get(NotifyUtils.IMATE_TEMPLATE_CODE_KEY)
            ?.takeIf { it.isNotBlank() }
            ?: request.templateCode
        val bizContext = buildBizContext(request.bodyParams)
        if (bizContext == null) {
            logger.warn(
                "[IMATE] missing biz context (projectId/pipelineId/buildId), skip. templateCode=$templateCode"
            )
            return
        }

        // 本地渲染：把 titleParams + bodyParams 合并后做 {{var}} 替换；缺失模板时直接放弃发送
        val mergedParams: MutableMap<String, String?> = mutableMapOf()
        request.titleParams?.forEach { (k, v) -> if (k !in INTERNAL_KEYS) mergedParams[k] = v }
        request.bodyParams?.forEach { (k, v) -> if (k !in INTERNAL_KEYS) mergedParams[k] = v }
        val body = imateTemplateRenderer.render(templateCode, mergedParams)
        if (body.isNullOrBlank()) {
            logger.warn("[IMATE] template render failed/empty, skip. templateCode=$templateCode")
            return
        }

        sessions.forEach { sessionId ->
            val payload = ImateSendMessageRequest(
                sessionId = sessionId,
                bizType = "CREATIVE_STREAM",
                templateCode = templateCode,
                title = commonNotifyMessageTemplateRecord.templateName,
                body = body,
                bizContext = bizContext
            )
            imateService.send(payload)
        }
    }

    private fun parseSessions(bodyParams: Map<String, String>?): List<String> {
        val raw = bodyParams?.get(NotifyUtils.IMATE_SESSION_ID_KEY).orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split("[,;]".toRegex()).map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun buildBizContext(bodyParams: Map<String, String>?): ImateBizContext? {
        val projectId = bodyParams?.get(NotifyUtils.IMATE_CTX_PROJECT_ID)?.takeIf { it.isNotBlank() }
            ?: return null
        val pipelineId = bodyParams[NotifyUtils.IMATE_CTX_PIPELINE_ID]?.takeIf { it.isNotBlank() }
            ?: return null
        val buildId = bodyParams[NotifyUtils.IMATE_CTX_BUILD_ID]?.takeIf { it.isNotBlank() }
            ?: return null
        return ImateBizContext(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = bodyParams[NotifyUtils.IMATE_CTX_STAGE_ID]?.takeIf { it.isNotBlank() },
            groupId = bodyParams[NotifyUtils.IMATE_CTX_GROUP_ID]?.takeIf { it.isNotBlank() },
            executeCount = bodyParams[NotifyUtils.IMATE_CTX_EXECUTE_COUNT]?.toIntOrNull()
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ImateNotifier::class.java)

        // bodyParams 中的内部占位 key，不应作为模板变量参与 {{var}} 替换
        private val INTERNAL_KEYS = setOf(
            NotifyUtils.IMATE_SESSION_ID_KEY,
            NotifyUtils.IMATE_TEMPLATE_CODE_KEY,
            NotifyUtils.IMATE_CTX_PROJECT_ID,
            NotifyUtils.IMATE_CTX_PIPELINE_ID,
            NotifyUtils.IMATE_CTX_BUILD_ID,
            NotifyUtils.IMATE_CTX_STAGE_ID,
            NotifyUtils.IMATE_CTX_GROUP_ID,
            NotifyUtils.IMATE_CTX_EXECUTE_COUNT,
            NotifyUtils.WEWORK_GROUP_KEY
        )
    }
}
