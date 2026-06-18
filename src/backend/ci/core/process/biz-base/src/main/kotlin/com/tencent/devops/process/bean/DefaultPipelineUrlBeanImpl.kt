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

package com.tencent.devops.process.bean

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil

class DefaultPipelineUrlBeanImpl constructor(private val commonConfig: CommonConfig) : PipelineUrlBean {

    companion object {
        // 创作流渠道 URL 前缀
        private const val CREATIVE_STREAM_PREFIX = "creative-stream"
        // 流水线渠道 URL 前缀
        private const val PIPELINE_PREFIX = "pipeline"
    }

    /**
     * 根据渠道获取 URL 中的模块路径前缀
     */
    private fun getModulePrefix(channelCode: ChannelCode?): String {
        return if (channelCode == ChannelCode.CREATIVE_STREAM) {
            CREATIVE_STREAM_PREFIX
        } else {
            PIPELINE_PREFIX
        }
    }

    /**
     * 根据渠道生成构建详情路径
     * 流水线渠道: /console/pipeline/{projectCode}/{pipelineId}/detail/{buildId}
     * 创作流渠道: /console/creative-stream/{projectCode}/flow/{pipelineId}/execute/{buildId}/execute-detail
     */
    private fun genDetailPath(
        projectCode: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode?
    ): String {
        val prefix = getModulePrefix(channelCode)
        return if (channelCode == ChannelCode.CREATIVE_STREAM) {
            "/console/$prefix/$projectCode/flow/$pipelineId/execute/$buildId/execute-detail"
        } else {
            "/console/$prefix/$projectCode/$pipelineId/detail/$buildId"
        }
    }

    @Suppress("LongParameterList")
    override fun genBuildDetailUrl(
        projectCode: String,
        pipelineId: String,
        buildId: String,
        position: String?,
        stageId: String?,
        needShortUrl: Boolean,
        channelCode: ChannelCode?
    ): String {
        return "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}${
            genDetailPath(projectCode, pipelineId, buildId, channelCode)
        }"
    }

    override fun genAppBuildDetailUrl(
        projectCode: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode?
    ): String {
        return "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}${
            genDetailPath(projectCode, pipelineId, buildId, channelCode)
        }"
    }

    override fun genBuildReviewUrl(
        projectCode: String,
        pipelineId: String,
        buildId: String,
        stageSeq: Int?,
        taskId: String?,
        needShortUrl: Boolean,
        channelCode: ChannelCode?
    ): String {
        val baseUrl = "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}${
            genDetailPath(projectCode, pipelineId, buildId, channelCode)
        }"

        val queryParams = mutableListOf<String>()
        stageSeq?.let { queryParams.add("reviewStageSeq=$it") }
        taskId?.let { queryParams.add("reviewTaskId=$it") }

        return if (queryParams.isNotEmpty()) {
            "$baseUrl?${queryParams.joinToString("&")}"
        } else {
            baseUrl
        }
    }
}
