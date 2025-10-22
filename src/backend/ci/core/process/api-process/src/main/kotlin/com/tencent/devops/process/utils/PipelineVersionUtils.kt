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

package com.tencent.devops.process.utils

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.pojo.setting.PipelineSettingVersion
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

@Suppress("ComplexMethod", "ComplexCondition")
object PipelineVersionUtils {

    fun getVersionNameByModel(
        currPipelineVersion: Int,
        currTriggerVersion: Int,
        settingVersion: Int,
        versionNum: Int,
        originModel: Model,
        newModel: Model
    ): String {
        val pipelineVersion = getPipelineVersion(currPipelineVersion, originModel, newModel)
            .coerceAtLeast(1)
        val triggerVersion = getTriggerVersion(currTriggerVersion, originModel, newModel)
            .coerceAtLeast(1)
        return "V$versionNum(P$pipelineVersion.T$triggerVersion.$settingVersion)"
    }

    fun getVersionName(
        versionNum: Int?,
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?
    ): String? {
        val isNull = versionNum == null || pipelineVersion == null || triggerVersion == null || settingVersion == null
        return if (isNull) {
            null
        } else {
            "V$versionNum(P$pipelineVersion.T$triggerVersion.$settingVersion)"
        }
    }

    /**
     * 根据当前版本号[currVersion], 原编排[originModel], 新编排[originModel]差异计算后得到新版本号
     */
    fun getTriggerVersion(
        currVersion: Int,
        originModel: Model,
        newModel: Model
    ): Int {
        return try {
            var changed = false
            val originTrigger = (originModel.stages.first().containers.first() as TriggerContainer)
                .copy(params = emptyList())
            val newTrigger = (newModel.stages.first().containers.first() as TriggerContainer)
                .copy(params = emptyList())
            if (originTrigger == newTrigger) {
                originTrigger.elements.forEachIndexed { index, origin ->
                    val new = newTrigger.elements[index]
                    if (origin != new) changed = true
                    if (origin.elementEnabled() != new.elementEnabled()) changed = true
                }
            } else {
                changed = true
            }
            if (changed) currVersion + 1 else currVersion
        } catch (ignore: Throwable) {
            currVersion + 1
        }
    }

    /**
     * 根据当前版本号[currVersion], 原编排[originModel], 新编排[originModel]差异计算后得到新版本号
     */
    fun getPipelineVersion(
        currVersion: Int,
        originModel: Model,
        newModel: Model
    ): Int {
        val originStages = originModel.stages.drop(1)
        val newStages = newModel.stages.drop(1)
        val originParams = (originModel.stages.first().containers.first() as TriggerContainer).params
        val newParams = (newModel.stages.first().containers.first() as TriggerContainer).params
        return if (originStages.differ(newStages) && originParams == newParams) {
            currVersion
        } else {
            currVersion + 1
        }
    }

    /**
     * 根据当前版本号[currVersion], 原设置[originSetting], 新设置[newSetting]差异计算后得到新版本号
     */
    fun getSettingVersion(
        currVersion: Int,
        originSetting: PipelineSettingVersion,
        newSetting: PipelineSettingVersion
    ): Int {
        return if (originSetting == newSetting) currVersion else currVersion + 1
    }

    private fun List<Stage>.differ(other: List<Stage>): Boolean {
        if (this != other && this.size != other.size) return false
        this.forEachIndexed { sIndex, thisStage ->
            val otherStage = other[sIndex]
            if (
                thisStage != otherStage || thisStage.containers.size != otherStage.containers.size ||
                thisStage.checkIn != otherStage.checkIn || thisStage.checkOut != otherStage.checkOut ||
                thisStage.stageControlOption != otherStage.stageControlOption
                ) {
                return false
            }
            thisStage.containers.forEachIndexed { cIndex, thisContainer ->
                val otherContainer = otherStage.containers[cIndex]
                if (thisContainer != otherContainer && thisContainer.elements.size != otherContainer.elements.size) {
                    return false
                }
                if (thisContainer is VMBuildContainer && otherContainer is VMBuildContainer) {
                    if (thisContainer != otherContainer || thisContainer.dispatchType != otherContainer.dispatchType ||
                        thisContainer.jobControlOption != otherContainer.jobControlOption
                    ) return false
                } else if (thisContainer is NormalContainer && otherContainer is NormalContainer) {
                    if (thisContainer != otherContainer ||
                        thisContainer.jobControlOption != otherContainer.jobControlOption
                    ) return false
                } else {
                    return false
                }
                thisContainer.elements.forEachIndexed { eIndex, thisElement ->
                    val otherElement = otherContainer.elements[eIndex]
                    if (thisElement != otherElement) return false
                    if (thisElement.additionalOptions != otherElement.additionalOptions) return false
                    if (thisElement.differ(otherElement)) return false
                }
            }
        }
        return true
    }

    fun Element.differ(other: Element): Boolean {
        if (this::class != other::class) {
            return true
        }

        val v1Properties = this.javaClass.kotlin.declaredMemberProperties
        val v2Properties = other.javaClass.kotlin.declaredMemberProperties
        if (v1Properties.size != v2Properties.size) {
            return true
        }

        val v1Map = v1Properties.associate {
            it.isAccessible = true
            it.name to it.get(this)
        }

        val v2Map = v2Properties.associate {
            it.isAccessible = true
            it.name to it.get(other)
        }

        if (v1Map.size != v2Map.size) {
            return true
        }

        for ((key, value) in v1Map) {
            if (!v2Map.containsKey(key)) {
                return true
            }
            if (v2Map[key] != value) {
                return true
            }
        }
        return false
    }
}
