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

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementCheckResult
import com.tencent.devops.process.engine.atom.plugin.IElementBizPluginService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 子流水线插件扩展点处理类
 */
@Service
class SubPipelineElementBizPluginService @Autowired constructor(
    private val subPipelineRepositoryService: SubPipelineRepositoryService
) : IElementBizPluginService {

    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineElementBizPluginService::class.java)
    }

    override fun supportElement(element: Element): Boolean {
        return subPipelineRepositoryService.supportElement(element)
    }

    override fun afterCreate(
        element: Element,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        userId: String,
        channelCode: ChannelCode,
        create: Boolean,
        container: Container
    ) = Unit

    override fun beforeDelete(element: Element, param: BeforeDeleteParam) = Unit

    @Suppress("UNCHECKED_CAST")
    override fun check(
        projectId: String?,
        userId: String,
        stage: Stage,
        container: Container,
        element: Element,
        contextMap: Map<String, String>,
        appearedCnt: Int,
        isTemplate: Boolean,
        oauthUser: String?
    ): ElementCheckResult {
        logger.info(
            "check the sub-pipeline permissions when deploying pipeline|projectId:$projectId|" +
                    "element:${element.id}|contextMap:$contextMap|appearedCnt:$appearedCnt|isTemplate:$isTemplate|" +
                    "oauthUser:$oauthUser|userId:$userId"
        )
        // 模板保存时不需要校验子流水线权限
        if (isTemplate || projectId.isNullOrBlank()) return ElementCheckResult(true)
        return subPipelineRepositoryService.checkElementPermission(
            projectId = projectId,
            stageName = stage.name ?: "",
            containerName = container.name,
            element = element,
            contextMap = contextMap,
            permission = AuthPermission.EXECUTE,
            userId = oauthUser ?: userId
        ) ?: ElementCheckResult(true)
    }
}
