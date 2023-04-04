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
 *
 */

package com.tencent.devops.common.auth.utils

import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.expression.ExpressionDTO
import com.tencent.devops.common.auth.api.AuthResourceType

@Suppress("ALL")
object AuthUtils {

    fun getProjects(content: ExpressionDTO): List<String> {
        // todo 得考虑新增contain表达有无影响
        if (content.field != "project.id") {
            if (content.operator != ExpressionOperationEnum.ANY &&
                content.operator != ExpressionOperationEnum.OR) {
                return emptyList()
            }
        }
        val projectList = mutableListOf<String>()
        when (content.operator) {
            ExpressionOperationEnum.ANY -> projectList.add("*")
            ExpressionOperationEnum.EQUAL -> projectList.add(content.value.toString())
            ExpressionOperationEnum.IN -> projectList.addAll(StringUtils.obj2List(content.value.toString()))
            ExpressionOperationEnum.OR -> content.content.forEach {
                projectList.addAll(getProjects(it))
            }
            else -> {
            }
        }
        return projectList
    }

    fun getResourceInstance(
        content: List<ExpressionDTO>,
        projectId: String,
        resourceType: AuthResourceType
    ): Set<String> {
        val instantList = mutableSetOf<String>()
        content.map {
            val field = it.field
            val op = it.operator
            if (!field.contains("_bk_iam_path_") || op != ExpressionOperationEnum.START_WITH) {
                return@map
            }
            val value = it.value.toString().split(",")
            if (value[0] != "/project") {
                return@map
            }

            // 选中了项目下 “无限制”选项
            if (value.size == 2) {
                if (value[1].substringBefore("/") == projectId) {
                    return setOf("*")
                } else {
                    return@map
                }
            }

            // 选中了项目下某资源的 特定实例
            // 如 /project,projectA/pipeline,pipelineB/
            if (value[1].substringBefore("/") != projectId || value[1].substringAfter("/") != resourceType.value) {
                return@map
            }
            val instance = value[2].substringBefore("/")
            if (instance == "*") {
                return setOf("*")
            } else {
                instantList.add(value[2].substringBefore("/"))
            }
        }
        return instantList
    }

    // 无content怎么处理 一层怎么处理,二层怎么处理。 默认只有两层。
    fun getResourceInstance(expression: ExpressionDTO, projectId: String, resourceType: String): Set<String> {
        val instantList = mutableSetOf<String>()
        // 项目下无限制 {"field":"pipeline._bk_iam_path_","op":"starts_with","value":"/project,test1/"}
        if (expression.content == null || expression.content.isEmpty()) {
            instantList.addAll(getInstanceByField(expression, projectId, resourceType))
        } else {
            instantList.addAll(getInstanceByContent(expression.content, expression, projectId, resourceType))
        }

        // 单个项目下有特定资源若干实例
        // [{"field":"pipeline.id","op":"in","value":["p-098b68a251ae4ec4b6f4fde87767387f",
        // "p-12b2c343109f43a58a79dcb9e3721c1b",
        // "p-54a8619d1f754d32b5b2bc249a74f26c"]},{"field":"pipeline._bk_iam_path_","op":"starts_with",
        // "value":"/project,demo/"}]

        // 多个项目下有特定资源若干实例
        // [{"content":[{"field":"pipeline.id","op":"in","value":["p-0d1fff4dabca4fc282e5ff63644bd339",
        // "p-54fb8b6562584df4b3693f7c787c105a"]},{"field":"pipeline._bk_iam_path_","op":"starts_with",
        // "value":"/project,v3test/"}],"op":"AND"},{"content":[{"field":"pipeline.id","op":"in",
        // "value":["p-098b68a251ae4ec4b6f4fde87767387f","p-12b2c343109f43a58a79dcb9e3721c1b",
        // "p-54a8619d1f754d32b5b2bc249a74f26c"]},{"field":"pipeline._bk_iam_path_",
        // "op":"starts_with","value":"/project,demo/"}],"op":"AND"}]

        // 多个项目下有特定资源权限,且有项目勾选任意
        // [{"field":"pipeline._bk_iam_path_","op":"starts_with","value":"/project,demo/"},
        // {"content":[{"field":"pipeline.id","op":"in","value":["p-0d1fff4dabca4fc282e5ff63644bd339",
        // "p-54fb8b6562584df4b3693f7c787c105a"]},{"field":"pipeline._bk_iam_path_","op":"starts_with",
        // "value":"/project,v3test/"}],"op":"AND"}]
        return instantList
    }

    private fun getInstanceByContent(
        childExpression: List<ExpressionDTO>,
        parentExpression: ExpressionDTO,
        projectId: String,
        resourceType: String
    ): Set<String> {
        return getInstanceByContent(
            childExpression = childExpression,
            projectId = projectId,
            resourceType = resourceType,
            type = parentExpression.operator
        )
    }

    private fun getInstanceByContent(
        childExpression: List<ExpressionDTO>,
        projectId: String,
        resourceType: String,
        type: ExpressionOperationEnum
    ): Set<String> {
        var cacheList = mutableSetOf<String>()
        run content@{
            childExpression.map {
                if (it.content != null && it.content.isNotEmpty()) {
                    val childInstanceList = getInstanceByContent(it.content, projectId, resourceType, it.operator)
                    if (childInstanceList.isNotEmpty()) {
                        // 如果有策略解析出来为“*”,则直接返回
                        if (childInstanceList.contains("*")) {
                            return childInstanceList
                        }
                        cacheList.addAll(childInstanceList)
                    } else {
                        // 若表达式为AND拼接,检测到不满足条件则直接返回空数组
                        if (!andCheck(cacheList, type)) {
                            return emptySet()
                        }
                    }
                    return@map
                }

                if (!checkField(it.field, resourceType) && !checkField(it.value.toString(), resourceType)) {
                    if (!andCheck(cacheList, type)) {
                        return emptySet()
                    }
                    return@map
                }
                when (it.operator) {
                    ExpressionOperationEnum.ANY -> {
                        cacheList.add("*")
                        return@content
                    }
                    ExpressionOperationEnum.IN -> {
                        cacheList.addAll(StringUtils.obj2List(it.value.toString()))
                        StringUtils.removeAllElement(cacheList)
                    }
                    ExpressionOperationEnum.EQUAL -> {
                        cacheList.add(it.value.toString())
                        StringUtils.removeAllElement(cacheList)
                    }
                    ExpressionOperationEnum.START_WITH -> {
                        // 两种情况: 1. 跟IN,EQUAL组合成项目下的某实例 2. 单指某个项目下的所有实例
                        val startWithPair = checkProject(projectId, it)
                        // 跟IN,EQUAL结合才会出现type = AND
                        if (type == ExpressionOperationEnum.AND) {
                            // 项目未命中直接返回空数组
                            // 命中则引用IN,EQUAL的数据,不将“*”加入到cacheList
                            if (!startWithPair.first) {
                                return emptySet()
                            }
                        } else {
                            // 若为项目级别,直接按projectCheck结果返回
                            cacheList.addAll(startWithPair.second)
                        }
                    }
                    else -> cacheList = emptySet<String>() as MutableSet<String>
                }
                if (!andCheck(cacheList, type)) {
                    return emptySet()
                }

                // 如果有“*” 表示项目下所有的实例都有权限,直接按“*”返回
                if (cacheList.contains("*")) {
                    return cacheList
                }
            }
        }

        return cacheList
    }

    fun getInstanceByField(expression: ExpressionDTO, projectId: String, resourceType: String): Set<String> {
        val instanceList = mutableSetOf<String>()
        val value = expression.value

        // 如果权限为整个项目, 直接返回
        if (expression.value == projectId && expression.operator == ExpressionOperationEnum.EQUAL) {
            instanceList.add("*")
            return instanceList
        }

        if (!checkField(expression.field, resourceType)) {
            return emptySet()
        }

        when (expression.operator) {
            ExpressionOperationEnum.ANY -> instanceList.add("*")
            ExpressionOperationEnum.EQUAL -> instanceList.add(value.toString())
            ExpressionOperationEnum.IN -> instanceList.addAll(StringUtils.obj2List(value.toString()))
            ExpressionOperationEnum.START_WITH -> {
                instanceList.addAll(checkProject(projectId, expression).second)
            }
            else -> {
            }
        }

        return instanceList
    }

    private fun checkProject(projectId: String, expression: ExpressionDTO): Pair<Boolean, Set<String>> {
        val instanceList = mutableSetOf<String>()
        val values = expression.value.toString().split(",")
        if (values[0] != "/project") {
            return Pair(false, emptySet())
        }
        if (values[1].substringBefore("/") != projectId) {
            return Pair(false, emptySet())
        }
        instanceList.add("*")
        return Pair(true, instanceList)
    }

    private fun checkField(field: String, resourceType: String): Boolean {
        if (field.contains(resourceType)) {
            return true
        }
        return false
    }

    private fun andCheck(instanceList: Set<String>, op: ExpressionOperationEnum): Boolean {
        if (op == ExpressionOperationEnum.AND) {
            if (instanceList.isEmpty()) {
                return false
            }
            return true
        }
        return true
    }
}
