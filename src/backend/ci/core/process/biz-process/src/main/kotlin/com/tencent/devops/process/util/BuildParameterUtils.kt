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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.util

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.utils.PipelineVarUtil
import org.springframework.stereotype.Component
import java.io.File

@Component
class BuildParameterUtils(private val parameterUtils: PswParameterUtils) {

    fun parseStartBuildParameter(
        paramProperties: List<BuildFormProperty>,
        paramValues: Map<String, String>
    ): MutableList<BuildParameters> {
        val startParamsWithType = mutableListOf<BuildParameters>()

        paramProperties.forEach {
            val value: Any
            // 通过对现有Model存在的旧变量替换成新变量， 如果已经是新的会为空，直接为it.id
            val paramKey = PipelineVarUtil.oldVarToNewVar(it.id) ?: it.id
            // 现有用户覆盖定义旧系统变量的，前端无法帮助转换，用户传的仍然是旧变量为key，则用新的Key无法找到，要用旧的id兜底
            val userInputValue = paramValues[paramKey] ?: paramValues[it.id]
            if (userInputValue == null) {
                if (it.required) {
                    throw ErrorCodeException(
                        defaultMessage = "启动时必填变量(${it.id})",
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL, params = arrayOf(it.id)
                    )
                }
                value = when (it.type) {
                    BuildFormPropertyType.PASSWORD -> parameterUtils.decrypt(it.defaultValue.toString())
                    else -> it.defaultValue
                }
            } else {
                value = when (it.type) {
                    BuildFormPropertyType.ARTIFACTORY -> getArtifactoryParamFileName(it.id, userInputValue)
                    BuildFormPropertyType.PASSWORD -> parameterUtils.decrypt(userInputValue)
                    else -> userInputValue
                }
            }
            startParamsWithType.add(BuildParameters(key = paramKey, value = value, valueType = it.type))
        }
        return startParamsWithType
    }

    private fun getArtifactoryParamFileName(paramKey: String, path: String): String {
        if (path.isBlank()) {
            return ""
        }
        try {
            return File(path).name
        } catch (e: Exception) {
            throw ErrorCodeException(defaultMessage = "仓库参数($paramKey)不合法",
                errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
                params = arrayOf(paramKey)
            )
        }
    }
}