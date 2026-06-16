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

package com.tencent.devops.process.service.pipeline.version.processor

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.PipelineAiSummaryEvent
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 流水线AI摘要版本后置处理器
 * 在流水线版本发布后，针对 CREATIVE_STREAM 渠道的流水线发送MQ事件，
 * 由 misc/biz-gpt 模块消费该事件并调用AI生成摘要
 */
@Service
class PipelineAiSummaryVersionPostProcessor constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : PipelineVersionCreatePostProcessor {

    override fun postProcessAfterVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting
    ) {
        with(context) {
            // 仅对已发布的版本且渠道为 CREATIVE_STREAM 的流水线触发AI摘要生成
            if (pipelineResourceVersion.status != VersionStatus.RELEASED) {
                return
            }
            if (pipelineBasicInfo.channelCode != ChannelCode.CREATIVE_STREAM) {
                return
            }

            logger.info(
                "Dispatching AI summary event for CREATIVE_STREAM pipeline[$pipelineId] " +
                    "project[$projectId] version[${pipelineResourceVersion.version}]"
            )

            val modelJson = JsonUtil.toJson(
                pipelineResourceWithoutVersion.model, formatted = false
            )

            pipelineEventDispatcher.dispatch(
                PipelineAiSummaryEvent(
                    source = "pipeline_version_create",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    userId = userId,
                    modelJson = modelJson,
                    pipelineResourceVersion = pipelineResourceVersion.version
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineAiSummaryVersionPostProcessor::class.java)
    }
}
