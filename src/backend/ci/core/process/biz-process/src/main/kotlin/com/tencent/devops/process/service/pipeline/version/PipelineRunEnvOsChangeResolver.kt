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

package com.tencent.devops.process.service.pipeline.version

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.PipelineRunEnvOsChange
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.pojo.AllCreateNodeEnv
import com.tencent.devops.process.service.pipeline.PipelineSettingVersionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 解析本次保存是否变更了流水线运行环境的操作系统。
 */
@Service
class PipelineRunEnvOsChangeResolver @Autowired constructor(
    private val client: Client,
    private val pipelineSettingVersionService: PipelineSettingVersionService
) {

    /**
     * 「运行环境由流水线设置指定」是渠道相关的语义，[resolve] 中的 when 是唯一的渠道扩展点。
     *
     * 普通流水线的运行环境由编排里的 Job 各自指定，不存在设置层面的环境操作系统变更，
     * 直接返回 null 走原有逻辑，既有渠道零额外开销、行为完全不变。
     */
    fun resolve(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        setting: PipelineSetting
    ): PipelineRunEnvOsChange? = when (channelCode) {
        // 创作流的运行环境即创作环境，由 setting.envHashId 指定
        ChannelCode.CREATIVE_STREAM -> resolveByEnvHashId(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            currentEnvHashId = setting.envHashId
        )

        else -> null
    }

    /**
     * 解析本次保存需要校验的运行环境操作系统。
     *
     * 无论本次是否变更过环境都返回校验目标：只在变更时校验会漏掉「环境不变但新增了不适配插件」
     * 以及「新建时所选环境与模板插件不适配」两类场景。
     * [PipelineRunEnvOsChange.previousOs] 仅在本次确实变更了操作系统时才有值，
     * 用于区分报错文案是「由 A 变更为 B」还是「当前环境为 A」。
     */
    private fun resolveByEnvHashId(
        userId: String,
        projectId: String,
        pipelineId: String,
        currentEnvHashId: String?
    ): PipelineRunEnvOsChange? {
        val currentId = currentEnvHashId?.takeIf { it.isNotBlank() } ?: return null
        val currentOs = resolveEnvOs(userId, projectId, currentId) ?: return null
        return PipelineRunEnvOsChange(
            previousOs = resolvePreviousOs(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                currentEnvHashId = currentId,
                currentOs = currentOs
            ),
            currentOs = currentOs
        )
    }

    /**
     * 解析变更前运行环境的操作系统，仅在本次保存确实变更了操作系统时返回，其余场景返回 null。
     *
     * 基线取最新 setting 版本而非主表：草稿保存只写 T_PIPELINE_SETTING_VERSION 不写主表，
     * 若取主表(最后发布版)，草稿里改过一次环境后，之后每次保存都会给出
     * 「由 A 变更为 B」这种用户本次并未做过的变更提示。
     */
    private fun resolvePreviousOs(
        userId: String,
        projectId: String,
        pipelineId: String,
        currentEnvHashId: String,
        currentOs: OS
    ): OS? {
        // 新建流水线无历史 setting，不存在可对比的变更前环境
        val previousId = pipelineSettingVersionService.getLatestSettingVersion(
            projectId = projectId,
            pipelineId = pipelineId
        )?.envHashId?.takeIf { it.isNotBlank() } ?: return null
        // 环境未变更是绝对多数场景，在此提前返回，避免无谓的操作系统解析
        if (previousId == currentEnvHashId) return null
        // 换了环境但操作系统相同(如同为 Linux 的两个环境)时，不属于操作系统变更
        return resolveEnvOs(userId, projectId, previousId)?.takeIf { it != currentOs }
    }

    /**
     * 解析环境的操作系统。内置环境本地即可映射，只有自定义环境才需要查询 environment 服务。
     *
     * 解析失败时返回 null 表示放弃本次校验而不阻断保存：操作系统适配性属于增强校验，
     * 不应因环境服务不可用导致用户无法保存草稿。
     */
    private fun resolveEnvOs(userId: String, projectId: String, envHashId: String): OS? {
        OS.values().find { AllCreateNodeEnv.hashId(it) == envHashId }?.let { return it }
        return try {
            client.get(ServiceEnvironmentResource::class)
                .get(userId = userId, projectId = projectId, envHashId = envHashId, checkPermission = false)
                .data?.os
        } catch (ignored: Throwable) {
            logger.warn("Failed to resolve run env os|$projectId|$envHashId", ignored)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRunEnvOsChangeResolver::class.java)
    }
}
