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

package com.tencent.devops.process.yaml

import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.process.pojo.pipeline.DeletePipelineResult
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVo
import com.tencent.devops.process.yaml.transfer.aspect.IPipelineTransferAspect
import java.util.LinkedList

/**
 * yaml文件对应的资源操作服务类
 */
interface IPipelineYamlResourceService {
    fun createYamlPipeline(
        userId: String,
        projectId: String,
        yaml: String,
        yamlFileName: String,
        branchName: String,
        isDefaultBranch: Boolean,
        description: String? = null,
        aspects: LinkedList<IPipelineTransferAspect>? = null,
        yamlInfo: PipelineYamlVo? = null
    ): DeployPipelineResult

    fun updateYamlPipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        yaml: String,
        yamlFileName: String,
        branchName: String,
        isDefaultBranch: Boolean,
        description: String? = null,
        aspects: LinkedList<IPipelineTransferAspect>? = null,
        yamlInfo: PipelineYamlVo? = null
    ): DeployPipelineResult

    fun updateBranchVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        branchName: String,
        branchVersionAction: BranchVersionAction,
        releaseBranch: Boolean? = false
    )

    fun deletePipeline(
        userId: String,
        projectId: String,
        pipelineId: String
    ): DeletePipelineResult

    fun getPipelineName(
        projectId: String,
        pipelineId: String
    ): String?

    fun existsReleaseVersion(
        projectId: String,
        pipelineId: String
    ): Boolean
}
