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

package com.tencent.devops.process.service.webhook

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.permission.PipelinePermissionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SamplePipelineBuildWebhookService : PipelineBuildWebhookService() {

    @Autowired
    private lateinit var pipelinePermissionService: PipelinePermissionService

    override fun checkPermission(userId: String, projectId: String, pipelineId: String) {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EXECUTE,
            message = I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.USER_NO_PIPELINE_PERMISSION_UNDER_PROJECT,
                params = arrayOf(userId, projectId, AuthPermission.EXECUTE.getI18n(I18nUtil.getLanguage(userId)))
            )
        )
    }

    @BkTimed // 要aop生效必须在子类上拦截
    override fun webhookTriggerPipelineBuild(
        projectId: String,
        pipelineId: String,
        codeRepositoryType: String,
        matcher: ScmWebhookMatcher
    ): Boolean {
        return super.webhookTriggerPipelineBuild(projectId, pipelineId, codeRepositoryType, matcher)
    }
}
