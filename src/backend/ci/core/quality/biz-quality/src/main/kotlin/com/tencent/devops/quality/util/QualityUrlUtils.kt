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

package com.tencent.devops.quality.util

import com.tencent.devops.common.client.Client
import com.tencent.devops.plugin.codecc.CodeccUtils
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.quality.constant.DEFAULT_CODECC_URL
import com.tencent.devops.quality.constant.codeccToolUrlPathMap
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class QualityUrlUtils {

    @Value("\${quality.codecc.host:}")
    private val codeccHost: String = ""

    private val logger = LoggerFactory.getLogger(QualityUrlUtils::class.java)

    fun getCodeCCUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        detail: String?,
        client: Client,
        logPrompt: String?
    ): String {
        val variable = client.get(ServiceVarResource::class).getBuildVar(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            varName = CodeccUtils.BK_CI_CODECC_TASK_ID
        ).data
        var taskId = variable?.get(CodeccUtils.BK_CI_CODECC_TASK_ID)

        return if (detail.isNullOrBlank() || detail!!.split(",").size > 1) {
            "http://$codeccHost/codecc/$projectId/task/$taskId/detail?buildId=$buildId"
        } else {
            var detailValue = logPrompt
            if (detailValue.isNullOrBlank()) {
                detailValue = codeccToolUrlPathMap[detail] ?: DEFAULT_CODECC_URL
            }
            val fillDetailUrl = detailValue.replace("##projectId##", projectId)
                .replace("##taskId##", taskId.toString())
                .replace("##buildId##", buildId)
                .replace("##detail##", detail)
            "http://$codeccHost$fillDetailUrl"
        }
    }
}
