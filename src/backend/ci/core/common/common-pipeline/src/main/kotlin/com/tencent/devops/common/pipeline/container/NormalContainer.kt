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

package com.tencent.devops.common.pipeline.container

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线模型-普通任务容器")
data class NormalContainer(
    @ApiModelProperty("构建容器序号id", required = false, hidden = true)
    override var id: String? = null,
    @ApiModelProperty("容器名称", required = true)
    override var name: String = "",
    @ApiModelProperty("任务集合", required = true)
    override var elements: MutableList<Element> = mutableListOf(),
    @ApiModelProperty("容器状态", required = false, hidden = true)
    override var status: String? = null,
    @ApiModelProperty("系统运行时间", required = false, hidden = true)
    override var startEpoch: Long? = null,
    override var systemElapsed: Long? = null,
    @ApiModelProperty("原子运行时间", required = false, hidden = true)
    override var elementElapsed: Long? = null,
    @ApiModelProperty("允许可跳过", required = false)
    @Deprecated(message = "do not use", replaceWith = ReplaceWith("JobControlOption.runCondition"))
    val enableSkip: Boolean? = false,
    @ApiModelProperty("触发条件", required = false)
    @Deprecated(message = "do not use", replaceWith = ReplaceWith("@see JobControlOption.customVariables"))
    val conditions: List<NameAndValue>? = null,
    @ApiModelProperty("是否可重试", required = false, hidden = true)
    override var canRetry: Boolean? = false,
    override var containerId: String? = null,
    @ApiModelProperty("无构建环境-等待运行环境启动的排队最长时间(分钟)", required = false)
    @Deprecated(message = "do not use")
    val maxQueueMinutes: Int = 60,
    @ApiModelProperty("无构建环境-运行最长时间(分钟)", required = false)
    @Deprecated(message = "@see JobControlOption.timeout")
    val maxRunningMinutes: Int = 1440,
    @ApiModelProperty("流程控制选项", required = true)
    var jobControlOption: JobControlOption? = null, // 为了兼容旧数据，所以定义为可空以及var
    @ApiModelProperty("互斥组", required = false)
    var mutexGroup: MutexGroup? = null, // 为了兼容旧数据，所以定义为可空以及var
    @ApiModelProperty("构建环境启动状态", required = false, hidden = true)
    override var startVMStatus: String? = null,
    @ApiModelProperty("容器运行次数", required = false, hidden = true)
    override var executeCount: Int? = 0,
    @ApiModelProperty("用户自定义ID", required = false, hidden = false)
    override val jobId: String? = null
) : Container {
    companion object {
        const val classType = "normal"
    }

    override fun getClassType() = classType
}
