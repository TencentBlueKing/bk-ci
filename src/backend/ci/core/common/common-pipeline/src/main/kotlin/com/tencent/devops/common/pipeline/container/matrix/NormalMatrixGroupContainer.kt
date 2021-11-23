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

package com.tencent.devops.common.pipeline.container.matrix

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线模型-普通任务构建矩阵")
data class NormalMatrixGroupContainer(
    @ApiModelProperty("构建容器序号id", required = false, hidden = true)
    override var id: String? = null,
    @ApiModelProperty("容器名称", required = true)
    override var name: String = "",
    @ApiModelProperty("任务集合", required = true)
    override var elements: List<Element> = listOf(),
    @ApiModelProperty("容器状态", required = false, hidden = true)
    override var status: String? = null,
    @ApiModelProperty("系统运行时间", required = false, hidden = true)
    override var startEpoch: Long? = null,
    override var systemElapsed: Long? = null,
    @ApiModelProperty("原子运行时间", required = false, hidden = true)
    override var elementElapsed: Long? = null,
    @ApiModelProperty("是否可重试-仅限于构建详情展示重试，目前未作为编排的选项，暂设置为null不存储", required = false, hidden = true)
    override var canRetry: Boolean? = null,
    override var containerId: String? = null,
    @ApiModelProperty("构建环境启动状态", required = false, hidden = true)
    override var startVMStatus: String? = null,
    @ApiModelProperty("容器运行次数", required = false, hidden = true)
    override var executeCount: Int? = 0,
    @ApiModelProperty("用户自定义ID", required = false, hidden = false)
    override val jobId: String? = null,
    @ApiModelProperty("是否包含post任务标识", required = false, hidden = true)
    override var containPostTaskFlag: Boolean? = null,
    @ApiModelProperty("是否为构建矩阵", required = false, hidden = true)
    override var matrixGroupFlag: Boolean? = false,
    @ApiModelProperty("流程控制选项", required = true)
    var jobControlOption: JobControlOption? = null, // 为了兼容旧数据，所以定义为可空以及var
    @ApiModelProperty("互斥组", required = false)
    var mutexGroup: MutexGroup? = null, // 为了兼容旧数据，所以定义为可空以及var
    // ---构建矩阵特有参数---
    @ApiModelProperty("分裂策略（支持变量、Json、参数映射表）", required = true)
    val strategyStr: String,
    @ApiModelProperty("额外的参数组合（变量名到特殊值映射的数组）", required = false)
    val includeCase: List<Map<String, String>>? = null,
    @ApiModelProperty("排除的参数组合（变量名到特殊值映射的数组）", required = false)
    val excludeCase: List<Map<String, String>>? = null,
    @ApiModelProperty("是否启用容器失败快速终止整个矩阵", required = false)
    val fastKill: Boolean? = false,
    @ApiModelProperty("Job运行的最大并发量", required = false)
    val maxConcurrency: Int? = null,
    @ApiModelProperty("分裂后的容器集合", required = false)
    var groupContainers: MutableList<VMBuildContainer>? = null,
    @ApiModelProperty("正在运行的数量", required = false)
    var totalCount: Int? = null,
    @ApiModelProperty("正在运行的数量", required = false)
    var runningCount: Int? = null
) : Container {
    companion object {
        const val classType = "normalMatrixGroup"
    }

    override fun getClassType() = classType
}
