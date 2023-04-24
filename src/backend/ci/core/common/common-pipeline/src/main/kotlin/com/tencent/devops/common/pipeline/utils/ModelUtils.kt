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

package com.tencent.devops.common.pipeline.utils

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ReflectUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement

@Suppress("ComplexMethod")
object ModelUtils {

    /**
     * 兼容旧的数据结构
     */
    @Suppress("ALL")
    fun initContainerOldData(c: Container) {
        if (c is NormalContainer) {
            if (c.jobControlOption == null) {

                c.jobControlOption = JobControlOption(
                    enable = true,
                    timeout = c.maxRunningMinutes,
                    timeoutVar = c.maxRunningMinutes.toString(),
                    runCondition = if (c.enableSkip == true) {
                        if (c.conditions?.isNotEmpty() == true) {
                            JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
                        } else {
                            JobRunCondition.STAGE_RUNNING
                        }
                    } else {
                        JobRunCondition.STAGE_RUNNING
                    },
                    customVariables = c.conditions
                )
            }
        } else if (c is VMBuildContainer) {
            if (c.jobControlOption == null) {
                c.jobControlOption = JobControlOption(
                    enable = true,
                    timeout = c.maxRunningMinutes,
                    timeoutVar = c.maxRunningMinutes.toString(),
                    runCondition = JobRunCondition.STAGE_RUNNING
                )
            }
        }
    }

    fun canManualStartup(triggerContainer: TriggerContainer): Boolean {
        triggerContainer.elements.forEach {
            if (it is ManualTriggerElement && it.isElementEnable()) {
                return true
            }
        }
        return false
    }

    fun canRemoteStartup(triggerContainer: TriggerContainer): Boolean {
        triggerContainer.elements.forEach {
            if (it is RemoteTriggerElement && it.isElementEnable()) {
                return true
            }
        }
        return false
    }

    fun stageNeedPause(triggerContainer: TriggerContainer): Boolean {
        triggerContainer.elements.forEach {
            if (it is RemoteTriggerElement && it.isElementEnable()) {
                return true
            }
        }
        return false
    }

    fun refreshCanRetry(model: Model) {
        model.stages.forEach { s ->
            val stageStatus = BuildStatus.parse(s.status)
            s.canRetry = stageStatus.isFailure() || stageStatus.isCancel()
            s.containers.forEach { c ->
                initContainerOldData(c)
                val jobStatus = BuildStatus.parse(c.status)
                c.canRetry = jobStatus.isFailure() || jobStatus.isCancel()
                if (c.canRetry == true) {
                    refreshContainer(c)
                }
            }
        }
    }

    private fun refreshContainer(container: Container) {
        val failElements = mutableListOf<Element>()
        container.elements.forEach { e ->
            refreshElement(element = e, failElements = failElements)
        }
    }

    private fun refreshElement(element: Element, failElements: MutableList<Element>) {

        val additionalOptions = element.additionalOptions
        if (additionalOptions == null || !additionalOptions.enable) {
            return
        }

        val taskStatus = BuildStatus.parse(element.status)
        if (!taskStatus.isFailure() && !taskStatus.isCancel()) {
            element.canRetry = null // 只为了减少传输数据，置空不会被序列化出字段
            element.canSkip = null
            return
        }

        element.canRetry = additionalOptions.manualRetry

        if (additionalOptions.continueWhenFailed) { // 开启了自动跳过
            if (additionalOptions.manualSkip == true) { // 开启了手动跳过 会覆盖自动跳过
                element.canSkip = true
            } else {
                element.canRetry = null // 自动跳过的不能手动重试
            }
        } else if (additionalOptions.runCondition == RunCondition.PRE_TASK_FAILED_ONLY ||
            additionalOptions.runCondition == RunCondition.PRE_TASK_FAILED_BUT_CANCEL ||
            additionalOptions.runCondition == RunCondition.PRE_TASK_FAILED_EVEN_CANCEL
        ) {
            // 前面有失败的插件时也要运行的插件，将前面的失败插件置为不可重试和跳过
            element.canRetry = null
            element.canSkip = null
            failElements.forEach { // 只为了减少传输数据，置空不会被序列化出字段
                it.canSkip = null
                it.canRetry = null
            }
        }

        if (element.canRetry == true) { // 先记录可重试的执行失败插件
            failElements.add(element)
        }
    }

    /**
     * 根据流水线基础模型map集合和构建变量模型map集合生成完整的流水线构建模型
     * @param baseModelMap 流水线基础模型map集合
     * @param modelFieldRecordMap 构建变量模型map集合
     * @return 完整的流水线构建模型
     */
    fun generatePipelineBuildModel(
        baseModelMap: MutableMap<String, Any>,
        modelFieldRecordMap: Map<String, Any>
    ): Model {
        val modelStr = JsonUtil.toJson(generateBuildModelDetail(baseModelMap, modelFieldRecordMap), false)
        return JsonUtil.to(modelStr, Model::class.java)
    }

    /**
     * 根据流水线基础模型map集合和构建变量模型map集合生成完整的构建模型map集合
     * @param baseModelMap 流水线基础模型map集合
     * @param modelFieldRecordMap 构建变量模型map集合
     * @return 完整的构建模型map集合
     */
    @Suppress("UNCHECKED_CAST")
    fun generateBuildModelDetail(
        baseModelMap: MutableMap<String, Any>,
        modelFieldRecordMap: Map<String, Any>
    ): MutableMap<String, Any> {
        // 遍历变量字段map集合
        modelFieldRecordMap.forEach { (fieldRecordName, fieldRecordValue) ->
            if (!ReflectUtil.isCollectionType(fieldRecordValue)) {
                // 如果字段不是集合类型，则直接替换流水线基础模型同字段的值
                baseModelMap[fieldRecordName] = fieldRecordValue
            } else if (baseModelMap[fieldRecordName] == null) {
                // 如果基础模型字段值为空，则直接拿变量集合中的字段值覆盖
                baseModelMap[fieldRecordName] = fieldRecordValue
            } else {
                if (fieldRecordValue is Map<*, *> && fieldRecordValue.isNotEmpty()) {
                    // 如果变量字段类型为map，则进行递归合并
                    val baseDataMap = baseModelMap[fieldRecordName] as MutableMap<String, Any>
                    val varDataMap = fieldRecordValue as MutableMap<String, Any>
                    baseModelMap[fieldRecordName] = generateBuildModelDetail(baseDataMap, varDataMap)
                } else if (fieldRecordValue is List<*> && fieldRecordValue.isNotEmpty()) {
                    // 如果变量字段类型为list，则遍历list进行递归合并
                    val baseDataList = baseModelMap[fieldRecordName] as MutableList<Any>
                    val varDataList = fieldRecordValue as MutableList<Any>
                    handleListFieldMergeBus(baseDataList, varDataList)
                }
            }
        }
        return baseModelMap
    }

    /**
     * 处理list变量合并逻辑
     * @param baseDataList 流水线基础模型list集合
     * @param recordDataList 构建变量模型list集合
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleListFieldMergeBus(baseDataList: MutableList<Any>, recordDataList: MutableList<Any>) {
        recordDataList.forEachIndexed { index, listItemObj ->
            // 判断构建变量模型list集合中的对象是否是集合类型
            if (!ReflectUtil.isCollectionType(listItemObj)) {
                if (index > baseDataList.size - 1) {
                    // 如果基础模型list集合中没有该对象则直接添加
                    baseDataList.add(listItemObj)
                } else {
                    // 如果基础模型list集合中有该对象则直接覆盖
                    baseDataList[index] = listItemObj
                }
            } else {
                if (listItemObj is Map<*, *> && listItemObj.isNotEmpty()) {
                    val baseListItemDataMap = if (index > baseDataList.size - 1) {
                        // 如果基础模型list集合中没有该对象则在添加一个空map集合用于合并
                        val emptyMap = mutableMapOf<String, Any>()
                        baseDataList.add(emptyMap)
                        emptyMap
                    } else {
                        baseDataList[index] as MutableMap<String, Any>
                    }
                    val varListItemDataMap = listItemObj as MutableMap<String, Any>
                    // 对map类型对象进行递归合并
                    baseDataList[index] = generateBuildModelDetail(baseListItemDataMap, varListItemDataMap)
                } else if (listItemObj is List<*> && listItemObj.isNotEmpty()) {
                    val baseListItemDataList = if (index > baseDataList.size - 1) {
                        // 如果基础模型list集合中没有该对象则在添加一个空list集合用于合并
                        val emptyList = mutableListOf<Any>()
                        baseDataList.add(emptyList)
                        emptyList
                    } else {
                        baseDataList[index] as MutableList<Any>
                    }
                    val varListItemDataList = listItemObj as MutableList<Any>
                    // 对list类型对象进行递归合并
                    handleListFieldMergeBus(baseListItemDataList, varListItemDataList)
                }
            }
        }
    }
}
