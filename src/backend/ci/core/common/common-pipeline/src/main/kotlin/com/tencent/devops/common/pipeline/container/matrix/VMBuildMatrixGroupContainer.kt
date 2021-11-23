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
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.type.DispatchType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线模型-虚拟机构建矩阵")
data class VMBuildMatrixGroupContainer(
    @ApiModelProperty("构建容器序号id", required = false, hidden = true)
    override var id: String? = null,
    @ApiModelProperty("容器名称", required = true)
    override var name: String = "构建环境",
    @ApiModelProperty("任务集合", required = true)
    override var elements: List<Element> = listOf(),
    override var status: String? = null,
    override var startEpoch: Long? = null,
    @ApiModelProperty("系统运行时间", required = false, hidden = true)
    override var systemElapsed: Long? = null,
    @ApiModelProperty("插件运行时间", required = false, hidden = true)
    override var elementElapsed: Long? = null,
    @ApiModelProperty("构建环境启动状态", required = false, hidden = true)
    override var startVMStatus: String? = null,
    @ApiModelProperty("容器运行次数", required = false, hidden = true)
    override var executeCount: Int? = 0,
    @ApiModelProperty("用户自定义ID", required = false, hidden = false)
    override val jobId: String? = null,
    @ApiModelProperty("是否包含post任务标识", required = false, hidden = true)
    override var containPostTaskFlag: Boolean? = null,
    @ApiModelProperty("是否可重试-仅限于构建详情展示重试，目前未作为编排的选项，暂设置为null不存储", required = false, hidden = true)
    override var canRetry: Boolean? = null,
    @ApiModelProperty("构建容器ID", required = false, hidden = true)
    override var containerId: String? = null,
    @ApiModelProperty("是否为构建矩阵", required = false, hidden = true)
    override var matrixGroupFlag: Boolean = true,
    @ApiModelProperty("构建机环境变量", required = false)
    val buildEnv: Map<String, String>? = null,
    @ApiModelProperty("用户自定义环境变量", required = false)
    val customBuildEnv: Map<String, String>? = null,
    @ApiModelProperty("流程控制选项", required = true)
    var jobControlOption: JobControlOption? = null,
    @ApiModelProperty("互斥组", required = false)
    var mutexGroup: MutexGroup? = null,
    // ---灵活控制构建环境的占位符---
    // 如果前端指定了baseOS和dispatchType则直接沿用，否则基于runsOn生成他们
    @ApiModelProperty("VM基础操作系统", required = false)
    val baseOS: VMBaseOS? = null,
    @ApiModelProperty("构建机环境选择", required = false)
    val dispatchType: DispatchType? = null,
    @ApiModelProperty("自定义编译环境", required = true)
    val runsOn: String,
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
        const val classType = "vmBuildMatrixGroup"
    }

    @ApiModelProperty("nfs挂载开关", required = false, hidden = true)
    var nfsSwitch: Boolean? = null
        get() {
            return if (null == field) true else field
        }

    override fun getClassType() = classType
}
