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
 *
 */

package com.tencent.devops.process.yaml

import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_HASH_ID
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.service.pipeline.PipelineYamlVersionResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineYamlFacadeService @Autowired constructor(
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineYamlVersionResolver: PipelineYamlVersionResolver
) {

    fun getPipelineYamlVo(
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineYamlVo? {
        return pipelineYamlService.getPipelineYamlVo(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }

    fun yamlExistInDefaultBranch(
        projectId: String,
        pipelineId: String
    ): Boolean {
        return pipelineYamlService.yamlExistInDefaultBranch(
            projectId = projectId,
            pipelineIds = listOf(pipelineId)
        )[pipelineId] ?: false
    }

    /**
     * 构建yaml流水线触发变量
     */
    fun buildYamlManualParamMap(projectId: String, pipelineId: String): Map<String, BuildParameters>? {
        val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId, pipelineId = pipelineId
        ) ?: return null
        return mutableMapOf(
            BK_REPO_WEBHOOK_HASH_ID to BuildParameters(BK_REPO_WEBHOOK_HASH_ID, pipelineYamlInfo.repoHashId),
            PIPELINE_WEBHOOK_BRANCH to BuildParameters(
                PIPELINE_WEBHOOK_BRANCH, pipelineYamlInfo.defaultBranch ?: ""
            )
        )
    }

    /**
     * 获取pac流水线指定分支的版本信息
     * 通过解析分支下文件md5值获取对应的版本信息
     */
    fun getPipelineYamlVersion(
        projectId: String,
        pipelineId: String,
        branch: String,
        yamlParams: MutableMap<String, BuildParameters> = mutableMapOf()
    ): Int? {
        // 不是PAC流水线
        val yamlInfo = pipelineYamlService.getPipelineYamlInfo(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: return null
        return pipelineYamlVersionResolver.resolvePipelineRefVersion(
            projectId = projectId,
            repoHashId = yamlInfo.repoHashId,
            filePath = yamlInfo.filePath,
            ref = branch
        ).let {
            // 记录当前分支信息
            yamlParams[BK_REPO_GIT_WEBHOOK_BRANCH] = BuildParameters(key = BK_REPO_GIT_WEBHOOK_BRANCH, value = branch)
            it
        }
    }

    fun getPipelineYamlInfo(
        projectId: String,
        pipelineId: String
    ) = pipelineYamlService.getPipelineYamlInfo(
        projectId = projectId,
        pipelineId = pipelineId
    )
}
