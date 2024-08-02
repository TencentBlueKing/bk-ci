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

package com.tencent.devops.process.yaml.v2.parsers.template

import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.models.Repositories
import com.tencent.devops.process.yaml.v2.parsers.template.models.Graph

class TemplateGraph(
    private var repoTemplateGraph: Graph<String> = Graph(),
    private var varTemplateGraph: Graph<String> = Graph(),
    private var stageTemplateGraph: Graph<String> = Graph(),
    private var jobTemplateGraph: Graph<String> = Graph(),
    private var stepTemplateGraph: Graph<String> = Graph()
) {
    // 将路径加入图中并且做循环嵌套检测
    fun saveAndCheckCyclicTemplate(
        fromPath: String,
        toPath: String,
        templateType: TemplateType
    ) {
        when (templateType) {
            TemplateType.VARIABLE -> {
                varTemplateGraph.addEdge(fromPath, toPath)
                if (varTemplateGraph.hasCyclic()) {
                    error(Constants.TEMPLATE_CYCLE_ERROR.format(toPath, fromPath, TemplateType.VARIABLE.text))
                }
            }
            TemplateType.STAGE -> {
                stageTemplateGraph.addEdge(fromPath, toPath)
                if (stageTemplateGraph.hasCyclic()) {
                    error(Constants.TEMPLATE_CYCLE_ERROR.format(toPath, fromPath, TemplateType.STAGE.text))
                }
            }
            TemplateType.JOB -> {
                jobTemplateGraph.addEdge(fromPath, toPath)
                if (jobTemplateGraph.hasCyclic()) {
                    error(Constants.TEMPLATE_CYCLE_ERROR.format(toPath, fromPath, TemplateType.JOB.text))
                }
            }
            TemplateType.STEP -> {
                stepTemplateGraph.addEdge(fromPath, toPath)
                if (stepTemplateGraph.hasCyclic()) {
                    error(Constants.TEMPLATE_CYCLE_ERROR.format(toPath, fromPath, TemplateType.STEP.text))
                }
            }
            else -> {
                return
            }
        }
    }

    // 对远程库的循环依赖做检测
    fun saveAndCheckCyclicRepo(
        fromPath: String,
        repo: Repositories?,
        toRepo: Repositories
    ) {
        // 判断是否有库之间的循环依赖
        repoTemplateGraph.addEdge(repo?.name ?: fromPath, toRepo.name)
        if (repoTemplateGraph.hasCyclic()) {
            error(Constants.REPO_CYCLE_ERROR)
        }
    }
}
