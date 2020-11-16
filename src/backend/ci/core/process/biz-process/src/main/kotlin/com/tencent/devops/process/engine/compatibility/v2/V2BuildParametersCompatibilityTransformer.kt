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

package com.tencent.devops.process.engine.compatibility.v2

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.process.engine.compatibility.BuildParametersCompatibilityTransformer
import com.tencent.devops.process.util.PswParameterUtils
import com.tencent.devops.process.utils.PipelineVarUtil
import java.io.File

open class V2BuildParametersCompatibilityTransformer constructor(private val pswParameterUtils: PswParameterUtils) :

    BuildParametersCompatibilityTransformer {

    override fun parseManualStartParam(
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
            if (userInputValue == null || userInputValue.isBlank()) { // tip: 这里不优化为 isNullOrBlank() 否则else下需要强指定非空!!
                // #2802 更正： required表示的是变量在“执行时显示”出来供填写，但非必填，未传递参数时，允许使用默认值，即使是空串
//                if (it.required) {
//                    throw ErrorCodeException(
//                        defaultMessage = "启动时必填变量(${it.id})",
//                        errorCode = CommonMessageCode.PARAMETER_IS_NULL, params = arrayOf(it.id)
//                    )
//                }
                value = when (it.type) {
                    BuildFormPropertyType.PASSWORD -> {
                        if (!it.defaultValue.toString().isBlank()) {
                            pswParameterUtils.decrypt(it.defaultValue.toString())
                        } else {
                            it.defaultValue
                        }
                    }
                    else -> it.defaultValue
                }
            } else {
                value = when (it.type) {
                    BuildFormPropertyType.ARTIFACTORY -> getArtifactoryParamFileName(it.id, userInputValue)
                    BuildFormPropertyType.PASSWORD -> pswParameterUtils.decrypt(userInputValue)
                    else -> userInputValue
                }
            }
            startParamsWithType.add(BuildParameters(key = paramKey, value = value, valueType = it.type))
        }

        return startParamsWithType
    }

    /**
     * 转换旧变量为新变量
     *
     * 旧变量： v1旧的命名(不规范）的系统变量
     * 新变量： v2新的命名的系统变量
     * 转换原则： 后出现的旧变量在转换为新变量命名后不允许覆盖前面已经存在的新变量
     *
     * @param paramLists 参数列表，注意顺序和转换原则，后出现的同名变量将被抛异（同名： 旧变量转换为新变即与新变量同名)
     */
    override fun transform(vararg paramLists: List<BuildParameters>): List<BuildParameters> {
        val startParamsWithType = mutableMapOf<String, BuildParameters>()
        paramLists.forEach { paramList ->
            paramList.forEach {
                // 通过对现有Model存在的旧变量替换成新变量， 如果已经是新的会为空，直接为it.key
                it.key = PipelineVarUtil.oldVarToNewVar(it.key) ?: it.key
                startParamsWithType.putIfAbsent(it.key, it)
            }
        }
        return startParamsWithType.values.toList()
    }

    private fun getArtifactoryParamFileName(paramKey: String, path: String): String {
        if (path.isBlank()) {
            return ""
        }
        try {
            return File(path).name
        } catch (e: Exception) {
            throw ErrorCodeException(
                defaultMessage = "仓库参数($paramKey)不合法",
                errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
                params = arrayOf(paramKey)
            )
        }
    }
}
