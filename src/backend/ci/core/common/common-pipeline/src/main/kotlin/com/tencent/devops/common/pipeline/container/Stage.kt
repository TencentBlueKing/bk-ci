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

package com.tencent.devops.common.pipeline.container

import com.tencent.devops.common.pipeline.IModelTemplate
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模型-阶段")
data class Stage(
    @get:Schema(title = "容器集合", required = true)
    val containers: List<Container> = listOf(),
    @get:Schema(title = "阶段ID (系统标识，用户不可编辑)", required = false)
    var id: String?,
    @get:Schema(title = "阶段名称", required = true)
    var name: String? = "",
    @get:Schema(title = "阶段ID (用户可编辑)", required = false)
    var stageIdForUser: String? = null,
    @get:Schema(title = "阶段标签", required = false, readOnly = true)
    var tag: List<String>? = null,
    @get:Schema(title = "阶段状态", required = false, readOnly = true)
    var status: String? = null,
    @get:Schema(title = "阶段启动时间", required = false, readOnly = true)
    @Deprecated("即将被timeCost代替")
    var startEpoch: Long? = null,
    @get:Schema(title = "容器运行时间", required = false, readOnly = true)
    @Deprecated("即将被timeCost代替")
    var elapsed: Long? = null,
    @get:Schema(title = "用户自定义环境变量", required = false)
    val customBuildEnv: Map<String, String>? = null,
    @get:Schema(title = "是否启用容器失败快速终止阶段", required = false)
    val fastKill: Boolean? = false,
    @get:Schema(title = "标识是否为FinallyStage，每个Model只能包含一个FinallyStage，并且处于最后位置", required = false)
    val finally: Boolean = false,
    @get:Schema(title = "当前Stage是否能重试", required = false)
    var canRetry: Boolean? = null,
    @get:Schema(title = "流程控制选项", required = true)
    var stageControlOption: StageControlOption? = null, // 为了兼容旧数据，所以定义为可空以及var
    @get:Schema(title = "stage准入配置", required = false)
    var checkIn: StagePauseCheck? = null, // stage准入配置
    @get:Schema(title = "stage准出配置", required = false)
    var checkOut: StagePauseCheck? = null, // stage准出配置
    @get:Schema(title = "步骤运行次数", required = false, readOnly = true)
    var executeCount: Int? = null,
    @get:Schema(title = "各项耗时", required = true)
    var timeCost: BuildRecordTimeCost? = null,
    override var template: String? = null,
    override var ref: String? = null,
    override var variables: Map<String, String>? = null
) : IModelTemplate {
    /**
     * 刷新stage的所有配置，如果是初始化则重置所有历史数据
     */
    fun resetBuildOption(init: Boolean? = false) {
        if (init == true) {
            status = null
            startEpoch = null
            elapsed = null
        }
        checkIn?.fixReviewGroups(init == true)
        checkOut?.fixReviewGroups(init == true)
        if (stageControlOption?.manualTrigger == true && checkIn == null) {
            checkIn = StagePauseCheck.convertControlOption(stageControlOption!!)
        }
        if (finally) canRetry = false
    }

    fun getContainer(vmSeqId: String): Container? {
        containers.forEach { container ->
            return container.getContainerById(vmSeqId) ?: return@forEach
        }
        return null
    }

    /**
     * 兼容性初始化等处理
     */
    fun transformCompatibility() {
        containers.forEach { container ->
            container.transformCompatibility()
        }
    }

    fun stageEnabled(): Boolean {
        return stageControlOption?.enable ?: true
    }
}
