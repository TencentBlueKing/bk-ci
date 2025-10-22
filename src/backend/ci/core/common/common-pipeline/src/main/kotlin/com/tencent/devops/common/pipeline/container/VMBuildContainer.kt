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

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import com.tencent.devops.common.pipeline.type.DispatchType
import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ReturnCount")
@Schema(title = "流水线模型-虚拟机构建容器")
data class VMBuildContainer(
    @get:Schema(title = "构建容器序号id", required = false, readOnly = true)
    override var id: String? = null,
    @get:Schema(title = "容器名称", required = true)
    override var name: String = "构建环境",
    @get:Schema(title = "任务集合", required = true)
    override var elements: List<Element> = listOf(),
    @get:Schema(title = "容器状态", required = false, readOnly = true)
    override var status: String? = null,
    @get:Schema(title = "系统运行时间", required = false, readOnly = true)
    @Deprecated("即将被timeCost代替")
    override var startEpoch: Long? = null,
    @get:Schema(title = "系统耗时（开机时间）", required = false, readOnly = true)
    @Deprecated("即将被timeCost代替")
    override var systemElapsed: Long? = null,
    @get:Schema(title = "插件执行耗时", required = false, readOnly = true)
    @Deprecated("即将被timeCost代替")
    override var elementElapsed: Long? = null,
    @get:Schema(title = "VM基础操作系统", required = true)
    val baseOS: VMBaseOS,
    @get:Schema(title = "预指定VM名称列表", required = true)
    val vmNames: Set<String> = setOf(),
    @get:Schema(title = "排队最长时间(分钟)", required = true)
    @Deprecated(message = "do not use")
    val maxQueueMinutes: Int? = 60,
    @get:Schema(title = "运行最长时间(分钟)", required = true)
    @Deprecated(message = "@see JobControlOption.timeout")
    val maxRunningMinutes: Int = 480,
    @get:Schema(title = "构建机环境变量（容器启动时写入环境）", required = false)
    val buildEnv: Map<String, String>? = null,
    @get:Schema(title = "用户自定义环境变量（Agent启动时写入环境）", required = false)
    @Deprecated("使用新字段customEnv代替，数据结构有变化")
    val customBuildEnv: Map<String, String>? = null,
    @get:Schema(title = "用户自定义环境变量（Agent启动时写入环境）", required = false)
    val customEnv: List<NameAndValue>? = null,
    @get:Schema(title = "第三方构建Hash ID", required = false)
    val thirdPartyAgentId: String? = null,
    @get:Schema(title = "第三方构建环境ID", required = false)
    val thirdPartyAgentEnvId: String? = null,
    @get:Schema(title = "第三方构建环境工作空间", required = false)
    val thirdPartyWorkspace: String? = null,
    @get:Schema(title = "Docker构建机", required = false)
    val dockerBuildVersion: String? = null,
    @get:Schema(title = "TStack Hash Id", required = false)
    @Deprecated("do not used")
    val tstackAgentId: String? = null,
    @get:Schema(title = "新的选择构建机环境", required = false)
    val dispatchType: DispatchType? = null,
    @get:Schema(title = "是否显示构建资源信息", required = false)
    var showBuildResource: Boolean? = false,
    @get:Schema(title =
        "是否可重试-仅限于构建详情展示重试，目前未作为编排的选项，暂设置为null不存储",
        required = false,
        readOnly = true
    )
    override var canRetry: Boolean? = null,
    @get:Schema(title = "是否访问外网", required = false, readOnly = true)
    var enableExternal: Boolean? = false,
    @get:Schema(title = "构建容器顺序ID（同id值）", required = false, readOnly = true)
    override var containerId: String? = null,
    @get:Schema(title = "容器唯一ID", required = false, readOnly = true)
    override var containerHashId: String? = null,
    @get:Schema(title = "流程控制选项", required = true)
    var jobControlOption: JobControlOption? = null, // 为了兼容旧数据，所以定义为可空以及var
    @get:Schema(title = "互斥组", required = false)
    var mutexGroup: MutexGroup? = null, // 为了兼容旧数据，所以定义为可空以及var
    @get:Schema(title = "构建环境启动状态", required = false, readOnly = true)
    override var startVMStatus: String? = null,
    @get:Schema(title = "容器运行次数", required = false, readOnly = true)
    override var executeCount: Int? = null,
    @get:Schema(title = "用户自定义ID", required = false, hidden = false)
    override var jobId: String? = null,
    @get:Schema(title = "是否包含post任务标识", required = false, readOnly = true)
    override var containPostTaskFlag: Boolean? = null,
    @get:Schema(title = "是否为构建矩阵", required = false, readOnly = true)
    override var matrixGroupFlag: Boolean? = false,
    @get:Schema(title = "各项耗时", required = true)
    override var timeCost: BuildRecordTimeCost? = null,
    @get:Schema(title = "开机任务序号", required = false, readOnly = true)
    override var startVMTaskSeq: Int? = null,
    @get:Schema(title = "构建矩阵配置项", required = false)
    var matrixControlOption: MatrixControlOption? = null,
    @get:Schema(title = "所在构建矩阵组的containerHashId（分裂后的子容器特有字段）", required = false)
    var matrixGroupId: String? = null,
    @get:Schema(title = "当前矩阵子容器的上下文组合（分裂后的子容器特有字段）", required = false)
    var matrixContext: Map<String, String>? = null,
    @get:Schema(title = "分裂后的容器集合（分裂后的父容器特有字段）", required = false)
    var groupContainers: MutableList<VMBuildContainer>? = null,
    override var template: String? = null,
    override var ref: String? = null,
    override var variables: Map<String, String>? = null
) : Container {
    companion object {
        const val classType = "vmBuild"
    }

    @get:Schema(title = "nfs挂载开关", required = false, readOnly = true)
    var nfsSwitch: Boolean? = null
        get() {
            return if (null == field) true else field
        }

    override fun getClassType() = classType

    override fun getContainerById(vmSeqId: String): Container? {
        if (id == vmSeqId || containerId == vmSeqId) return this
        fetchGroupContainers()?.forEach {
            if (it.id == vmSeqId || containerId == vmSeqId) return it
        }
        return null
    }

    override fun retryFreshMatrixOption() {
        groupContainers = mutableListOf()
        matrixControlOption?.finishCount = null
        matrixControlOption?.totalCount = null
    }

    override fun fetchGroupContainers(): List<Container>? {
        return groupContainers?.toList()
    }

    override fun fetchMatrixContext(): Map<String, String>? {
        return matrixContext
    }

    override fun containerEnabled(): Boolean {
        return jobControlOption?.enable ?: true
    }

    override fun setContainerEnable(enable: Boolean) {
        jobControlOption = jobControlOption?.copy(enable = enable) ?: JobControlOption(enable)
    }

    override fun transformCompatibility() {
        if (jobControlOption?.timeoutVar.isNullOrBlank()) {
            jobControlOption?.timeoutVar = jobControlOption?.timeout.toString()
        }
        if (mutexGroup?.timeoutVar.isNullOrBlank()) {
            mutexGroup?.timeoutVar = mutexGroup?.timeout.toString()
        }
        super.transformCompatibility()
    }
}
