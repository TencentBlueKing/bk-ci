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

package com.tencent.devops.process.engine.compatibility

import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.utils.PipelineVarUtil

/**
 *
 * 定义流水线变量兼容转换器
 */
interface BuildParametersCompatibilityTransformer {

    /**
     * 解析前端手工启动传入的参数并与triggerContainer的BuildFormProperty替换
     * 前端传入的为正确的新变量
     */
    fun parseTriggerParam(
        userId: String,
        projectId: String,
        pipelineId: String,
        paramProperties: List<BuildFormProperty>,
        paramValues: Map<String, String>
    ): MutableMap<String, BuildParameters>

    /**
     * 解析 webhook 等触发链路的启动参数
     * 在 parseTriggerParam 的基础上补充编排中未声明的参数，并兼容旧变量名
     */
    fun parseStartParams(
        userId: String,
        projectId: String,
        pipelineId: String,
        paramProperties: List<BuildFormProperty>,
        startParams: Map<String, Any>
    ): MutableMap<String, BuildParameters> {
        val pipelineParamMap = mutableMapOf<String, BuildParameters>()
        val paramMap = parseTriggerParam(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            paramProperties = paramProperties,
            paramValues = startParams.mapValues { it.value.toString() }
        )
        pipelineParamMap.putAll(paramMap)
        startParams.forEach {
            if (paramMap.containsKey(it.key)) {
                return@forEach
            }
            // 从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
            val newVarName = PipelineVarUtil.oldVarToNewVar(it.key)
            if (newVarName == null) { // 为空表示该变量是新的，或者不需要兼容，直接加入，能会覆盖旧变量转换而来的新变量
                pipelineParamMap[it.key] = BuildParameters(key = it.key, value = it.value)
            } else if (!pipelineParamMap.contains(newVarName)) { // 新变量还不存在，加入
                pipelineParamMap[newVarName] = BuildParameters(key = newVarName, value = it.value)
            }
        }
        return pipelineParamMap
    }
}
