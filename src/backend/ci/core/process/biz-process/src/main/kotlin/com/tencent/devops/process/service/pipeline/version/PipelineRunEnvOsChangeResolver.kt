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
import com.tencent.devops.process.engine.atom.AtomUtils
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
     * 「运行环境由流水线设置指定」是渠道相关的语义，[isRunEnvSpecifiedBySetting] 是唯一的渠道扩展点。
     *
     * 普通流水线的运行环境由编排里的 Job 各自指定，不存在设置层面的环境操作系统变更，在此返回 null，
     * 不产生任何查询；其插件适配度改由编排校验按 Job 声明的构建环境操作系统逐个比对
     * (见 AtomUtils.resolveJobRunEnvOs)，并非不校验。
     *
     * [channelCode] 须是流水线自身所属渠道。对已存在的流水线不要传请求上下文里的渠道：
     * openapi 的请求渠道由网关部署标签决定(见 ApiGatewayUtil.getChannelCode)，与流水线无关。
     */
    fun resolve(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        setting: PipelineSetting
    ): PipelineRunEnvOsChange? {
        if (!isRunEnvSpecifiedBySetting(channelCode)) return null
        return resolveByEnvHashId(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            currentEnvHashId = setting.envHashId,
            channelCode = channelCode
        )
    }

    /**
     * 该渠道的运行环境是否由流水线设置指定。
     *
     * 供调用方在「取设置、查编排」这类开销之前先行判断，避免为不适用的渠道做无谓查询。
     * 判定与保存校验侧同源([AtomUtils.isRunEnvSpecifiedBySetting])，不在此另立口径：
     * 两侧一旦分叉，本方法放过的渠道会在校验侧被当成普通流水线，退而取 Job 的 baseOS 做比对。
     */
    fun isRunEnvSpecifiedBySetting(channelCode: ChannelCode) = AtomUtils.isRunEnvSpecifiedBySetting(channelCode)

    /**
     * 解析本次保存需要校验的运行环境操作系统。
     *
     * 无论本次是否变更过环境都返回校验目标：只在变更时校验会漏掉「环境不变但新增了不适配插件」
     * 以及「新建时所选环境与模板插件不适配」两类场景。
     */
    private fun resolveByEnvHashId(
        userId: String,
        projectId: String,
        pipelineId: String,
        currentEnvHashId: String?,
        channelCode: ChannelCode
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
            currentOs = currentOs,
            // 在此按流水线自身渠道解析并随校验目标一同下传，校验方无需(也不该)再自行判定渠道
            osJobTypeName = AtomUtils.resolveOsJobType(channelCode).name
        )
    }

    /**
     * 解析变更前运行环境的操作系统，语义见 [PipelineRunEnvOsChange.previousOs]。
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
        // 变更前没有运行环境：新建流水线无历史 setting，已有流水线也可能是首次指定环境。
        // 此时返回 null 而非 currentOs，两者不可混同：后者会让调用方以为本次没换过系统
        val previousId = pipelineSettingVersionService.getLatestSettingVersion(
            projectId = projectId,
            pipelineId = pipelineId
        )?.envHashId?.takeIf { it.isNotBlank() } ?: return null
        // 环境未变更是绝对多数场景，同一环境的操作系统必然相同，在此提前返回，避免无谓的解析
        if (previousId == currentEnvHashId) return currentOs
        // 换了环境，其操作系统可能与当前相同(如同为 Linux 的两个环境)，由调用方按需比较。
        // 解析失败时按「与当前相同」处理而不返回 null：这属于环境服务不可用等外部原因，
        // 与「变更前没有运行环境」是两回事，混同会把存量编排判成本次新引入而阻断保存
        return resolveEnvOs(userId, projectId, previousId) ?: currentOs
    }

    /**
     * 解析环境的操作系统。内置环境本地即可映射，只有自定义环境才需要查询 environment 服务。
     *
     * 解析失败时返回 null 表示放弃本次校验而不阻断保存：操作系统适配性属于增强校验，
     * 不应因环境服务不可用导致用户无法保存草稿。
     */
    private fun resolveEnvOs(userId: String, projectId: String, envHashId: String): OS? {
        OS.entries.find { AllCreateNodeEnv.hashId(it) == envHashId }?.let { return it }
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
