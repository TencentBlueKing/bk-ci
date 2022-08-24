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
import com.tencent.devops.process.yaml.v2.parsers.template.models.NoReplaceTemplate

@Suppress("ALL")
object TemplateYamlUtil {
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
}
