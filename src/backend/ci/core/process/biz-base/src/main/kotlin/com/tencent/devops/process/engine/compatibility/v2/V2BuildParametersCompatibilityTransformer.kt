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

package com.tencent.devops.process.engine.compatibility.v2

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.compatibility.BuildParametersCompatibilityTransformer
import com.tencent.devops.process.utils.PipelineVarUtil
import org.slf4j.LoggerFactory

open class V2BuildParametersCompatibilityTransformer : BuildParametersCompatibilityTransformer {

    override fun parseTriggerParam(
        userId: String,
        projectId: String,
        pipelineId: String,
        paramProperties: List<BuildFormProperty>,
        paramValues: Map<String, String>
    ): MutableMap<String, BuildParameters> {

        val paramsMap = HashMap<String, BuildParameters>(paramProperties.size, 1F)

        paramProperties.forEach { param ->
            // 通过对现有Model存在的旧变量替换成新变量， 如果已经是新的会为空，直接为it.id
            val key = PipelineVarUtil.oldVarToNewVar(param.id) ?: param.id

            // 现有用户覆盖定义旧系统变量的，前端无法帮助转换，用户传的仍然是旧变量为key，则用新的Key无法找到，要用旧的id兜底
            // 如果编排中指定为常量，则必须以编排的默认值为准，不支持触发时传参覆盖
            val value = if (param.constant == true) {
                // 常量需要在启动是强制设为只读
                param.readOnly = true
                param.defaultValue
//            } else if (!param.required) {
//                // TODO #8161 没有作为前端可填入参的变量，直接取默认值，不可被覆盖（实施前仅打印日志）
//                param.defaultValue
            } else {
                val overrideValue = paramValues[key] ?: paramValues[param.id]
                if (!param.required && overrideValue != null) {
                    logger.warn(
                        "BKSystemErrorMonitor|parseTriggerParam|$userId|$projectId|$pipelineId|[$key] " +
                            "not required, overrideValue=$overrideValue, defaultValue=${param.defaultValue}"
                    )
                }
                overrideValue ?: param.defaultValue
            }
            if (param.valueNotEmpty == true && value.toString().isEmpty()) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BUILD_START_PARAM_NO_EMPTY,
                    params = arrayOf(param.id)
                )
            }

            paramsMap[key] = BuildParameters(
                key = key,
                value = value,
                valueType = param.type,
                readOnly = param.readOnly,
                desc = param.desc,
                defaultValue = param.defaultValue
            )
        }

        return paramsMap
    }

    companion object {
        private val logger = LoggerFactory.getLogger(V2BuildParametersCompatibilityTransformer::class.java)
    }
}
