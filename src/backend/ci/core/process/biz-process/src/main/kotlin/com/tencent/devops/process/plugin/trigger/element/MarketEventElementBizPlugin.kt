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

package com.tencent.devops.process.plugin.trigger.element

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.market.MarketEventAtomElement
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz
import com.tencent.devops.process.plugin.trigger.service.PipelineTimerService
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import org.slf4j.LoggerFactory

@ElementBiz
class MarketEventElementBizPlugin constructor(
    private val pipelineTimerService: PipelineTimerService
) : ElementBizPlugin<MarketEventAtomElement> {

    override fun elementClass(): Class<MarketEventAtomElement> {
        return MarketEventAtomElement::class.java
    }

    override fun check(element: MarketEventAtomElement, appearedCnt: Int) = Unit

    override fun beforeDelete(element: MarketEventAtomElement, param: BeforeDeleteParam) {
        if (param.pipelineId.isNotBlank()) {
            with(param) {
                val taskId = element.id ?: ""
                pipelineTimerService.deleteTimer(projectId, pipelineId, userId, taskId).let {
                    logger.info("beforeDelete|$pipelineId|delete [${element.id}] timer|result=$it")
                }
            }
        }
    }

    override fun afterCreate(
        element: MarketEventAtomElement,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        userId: String,
        channelCode: ChannelCode,
        create: Boolean,
        container: Container,
        yamlInfo: PipelineYamlVo?
    ) = Unit

    companion object {
        private val logger = LoggerFactory.getLogger(MarketEventElementBizPlugin::class.java)
    }
}
