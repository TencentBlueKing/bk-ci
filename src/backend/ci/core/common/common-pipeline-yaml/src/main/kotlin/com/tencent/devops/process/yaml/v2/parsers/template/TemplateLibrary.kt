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
import com.tencent.devops.process.yaml.v2.parsers.template.models.GetTemplateParam

data class TemplateLibrary<T>(
    val extraParameters: T,
    // 获取模板文件函数，将模板替换过程与获取文件解耦，方便测试或链接其他代码库
    val getTemplateMethod: (
        param: GetTemplateParam<T>
    ) -> String,
    // 存储当前库的模板信息，减少重复获取 key: templatePath value： template
    var templates: MutableMap<String, String> = mutableMapOf()
)

// 从模板库中获得数据，如果有直接取出，没有则根据保存的库信息从远程仓库拉取，再没有则报错
fun <T> TemplateLibrary<T>.getTemplate(
    path: String,
    templateType: TemplateType?,
    nowRepo: Repositories?,
    toRepo: Repositories?
): String {
    if (templates[path] != null) {
        return templates[path]!!
    }
    //  没有库信息说明是触发库
    val template = if (toRepo == null) {
        getTemplateMethod(
            GetTemplateParam(
                path = path,
                templateType = templateType,
                nowRepoId = nowRepo?.repository,
                targetRepo = null,
                extraParameters = extraParameters
            )
        )
    } else {
        getTemplateMethod(
            GetTemplateParam(
                path = path,
                templateType = templateType,
                nowRepoId = nowRepo?.repository,
                targetRepo = toRepo,
                extraParameters = extraParameters
            )
        )
    }
    setTemplate(path, template)
    return templates[path]!!
}

fun <T> TemplateLibrary<T>.setTemplate(path: String, template: String) {
    templates[path] = template
}
