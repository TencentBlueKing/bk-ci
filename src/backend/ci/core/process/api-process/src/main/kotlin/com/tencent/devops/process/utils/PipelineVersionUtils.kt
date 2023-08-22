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

package com.tencent.devops.process.utils

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import org.json.JSONObject

object PipelineVersionUtils {

    fun getVersionName(
        pipelineVersion: Int?,
        triggerVersion: Int?,
        settingVersion: Int?
    ): String {
        return if (pipelineVersion == null || triggerVersion == null || settingVersion == null) {
            "init"
        } else "P$pipelineVersion.T$triggerVersion.$settingVersion"
    }

    /**
     * 根据当前版本号[currVersion], 原编排[originModel], 新编排[originModel]差异计算后得到新版本号
     */
    fun getTriggerVersion(
        currVersion: Int,
        originModel: Model,
        newModel: Model
    ): Int {
        val originTriggerJson = JSONObject(originModel.stages.first())
        val triggerJson = JSONObject(newModel.stages.first())
        return if (!originTriggerJson.similar(triggerJson)) currVersion + 1 else currVersion
    }

    /**
     * 根据当前版本号[currVersion], 原编排[originModel], 新编排[originModel]差异计算后得到新版本号
     */
    fun getPipelineVersion(
        currVersion: Int,
        originModel: Model,
        newModel: Model
    ): Int {
        val originPipelineJson = JSONObject(originModel.stages.slice(1 until originModel.stages.size))
        val pipelineJson = JSONObject(newModel.stages.slice(1 until newModel.stages.size))
        return if (!originPipelineJson.similar(pipelineJson)) currVersion + 1 else currVersion
    }

    /**
     * 根据当前版本号[currVersion], 原设置[originSetting], 新设置[newSetting]差异计算后得到新版本号
     */
    fun getSettingVersion(
        currVersion: Int,
        originSetting: PipelineSetting,
        newSetting: PipelineSetting
    ): Int {
        val originJson = JSONObject(originSetting)
        val currentJson = JSONObject(newSetting)
        return if (!originJson.similar(currentJson)) currVersion + 1 else currVersion
    }
}
