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
import com.tencent.devops.process.yaml.v2.exception.YamlFormatException
import com.tencent.devops.process.yaml.v2.models.Repositories
import com.tencent.devops.process.yaml.v2.parameter.Parameters
import com.tencent.devops.process.yaml.v2.parameter.ParametersType
import com.tencent.devops.process.yaml.v2.parsers.template.models.NoReplaceTemplate
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import org.slf4j.LoggerFactory

@Suppress("ALL")
object TemplateYamlUtil {

    private val logger = LoggerFactory.getLogger(TemplateYamlUtil::class.java)

    // 检查是否具有重复的ID，job，variable中使用
    fun checkDuplicateKey(
        filePath: String,
        keys: Set<String>,
        newKeys: Set<String>,
        toPath: String? = null
    ): Boolean {
        val interSet = newKeys intersect keys
        return if (interSet.isEmpty() || (interSet.size == 1 && interSet.last() == Constants.TEMPLATE_KEY)) {
            true
        } else {
            if (toPath == null) {
                throw error(
                    Constants.TEMPLATE_ROOT_ID_DUPLICATE.format(
                        filePath,
                        interSet.filter { it != Constants.TEMPLATE_KEY }
                    )
                )
            } else {
                throw error(
                    Constants.TEMPLATE_ID_DUPLICATE.format(
                        interSet.filter { it != Constants.TEMPLATE_KEY },
                        filePath,
                        toPath
                    )
                )
            }
        }
    }

    // 校验当前模板的远程库信息，每个文件只可以使用当前文件下引用的远程库
    fun <T> checkAndGetRepo(
        fromPath: String,
        repoName: String,
        templateType: TemplateType,
        templateLib: TemplateLibrary<T>,
        nowRepo: Repositories?,
        toRepo: Repositories?
    ): Repositories {
        val repos = YamlObjects.getObjectFromYaml<NoReplaceTemplate>(
            path = fromPath,
            template = templateLib.getTemplate(
                path = fromPath,
                templateType = templateType,
                nowRepo = nowRepo,
                toRepo = toRepo
            )
        ).resources?.repositories

        repos?.forEach {
            if (it.name == repoName) {
                return it
            }
        }

        throw YamlFormatException(Constants.REPO_NOT_FOUND_ERROR.format(fromPath, repoName))
    }

    private fun error(content: String) = YamlFormatException(content)

    /**
     * 为模板中的变量赋值(旧版本只是为了兼容，非必要不要使用)
     * @param fromPath 来自哪个文件
     * @param path 读取的哪个模板文件
     * @param template 被读取的模板文件内容
     * @param templateParameters 被读取的模板文件自带的参数
     * @param parameters 引用模板文件时传入的参数
     */
    @Deprecated("旧版本，只是为了兼容，非必要不要使用")
    fun parseTemplateParametersOld(
        fromPath: String,
        path: String,
        template: String,
        templateParameters: MutableList<Parameters>?,
        parameters: Map<String, Any?>?
    ): String {
        if (!templateParameters.isNullOrEmpty()) {
            templateParameters.forEachIndexed { index, param ->
                if (parameters != null) {
                    val valueName = param.name

                    val newValue = parameters[param.name]
                    if (parameters.keys.contains(valueName)) {
                        if (!param.values.isNullOrEmpty() && !param.values!!.contains(newValue)) {
                            throw error(
                                Constants.VALUE_NOT_IN_ENUM.format(
                                    fromPath,
                                    path,
                                    valueName,
                                    newValue.toString(),
                                    param.values.joinToString(",")
                                )
                            )
                        } else {
                            templateParameters[index] = param.copy(default = newValue)
                        }
                    }
                }
            }
        } else {
            return template
        }
        // 模板替换 先替换调用模板传入的参数，再替换模板的默认参数
        val parametersListMap = templateParameters.filter {
            it.default != null && it.type == ParametersType.ARRAY.value
        }.associate {
            "parameters.${it.name}" to it.default
        }
        val parametersStringMap = templateParameters.filter { it.default != null }.associate {
            "parameters.${it.name}" to if (it.default == null) {
                null
            } else {
                it.default.toString()
            }
        }
        val replacedList = ScriptYmlUtils.parseParameterValue(template, parametersListMap, ParametersType.ARRAY)
        return ScriptYmlUtils.parseParameterValue(replacedList, parametersStringMap, ParametersType.STRING)
    }
}
