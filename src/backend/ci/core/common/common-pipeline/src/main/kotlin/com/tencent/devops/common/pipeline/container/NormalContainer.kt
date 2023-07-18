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

package com.tencent.devops.common.pipeline.container

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Suppress("ReturnCount")
@ApiModel("流水线模型-普通任务容器")
data class NormalContainer(
    @ApiModelProperty("构建容器序号id", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    override var id: String? = null,
    @ApiModelProperty("容器名称", required = true)
    override var name: String = "",
    @ApiModelProperty("任务集合", required = true)
    override var elements: List<Element> = listOf(),
    @ApiModelProperty("容器状态", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    override var status: String? = null,
    @ApiModelProperty("系统运行时间", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Deprecated("即将被timeCost代替")
    override var startEpoch: Long? = null,
    @ApiModelProperty("系统耗时（开机时间）", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Deprecated("即将被timeCost代替")
    override var systemElapsed: Long? = null,
    @ApiModelProperty("插件执行耗时", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Deprecated("即将被timeCost代替")
    override var elementElapsed: Long? = null,
    @ApiModelProperty("允许可跳过", required = false)
    @Deprecated(message = "do not use", replaceWith = ReplaceWith("JobControlOption.runCondition"))
    val enableSkip: Boolean? = false,
    @ApiModelProperty("触发条件", required = false)
    @Deprecated(message = "do not use", replaceWith = ReplaceWith("@see JobControlOption.customVariables"))
    val conditions: List<NameAndValue>? = null,
    @ApiModelProperty(
        "是否可重试-仅限于构建详情展示重试，目前未作为编排的选项，暂设置为null不存储",
        required = false,
        accessMode = ApiModelProperty.AccessMode.READ_ONLY
    )
    override var canRetry: Boolean? = null,
    @ApiModelProperty("构建容器顺序ID（同id值）", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    override var containerId: String? = null,
    @ApiModelProperty("容器唯一ID", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    override var containerHashId: String? = null,
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
    @ApiModelProperty("构建环境启动状态", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    override var startVMStatus: String? = null,
    @ApiModelProperty("容器运行次数", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    override var executeCount: Int? = null,
    @ApiModelProperty("用户自定义ID", required = false, hidden = false)
    override val jobId: String? = null,
    @ApiModelProperty("是否包含post任务标识", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    override var containPostTaskFlag: Boolean? = null,
    @ApiModelProperty("是否为构建矩阵", required = false, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    override var matrixGroupFlag: Boolean? = false,
    @ApiModelProperty("各项耗时", required = true)
    override var timeCost: BuildRecordTimeCost? = null,
    @ApiModelProperty("构建矩阵配置项", required = false)
    var matrixControlOption: MatrixControlOption? = null,
    @ApiModelProperty("所在构建矩阵组的containerHashId（分裂后的子容器特有字段）", required = false)
    var matrixGroupId: String? = null,
    @ApiModelProperty("当前矩阵子容器的上下文组合（分裂后的子容器特有字段）", required = false)
    var matrixContext: Map<String, String>? = null,
    @ApiModelProperty("分裂后的容器集合（分裂后的父容器特有字段）", required = false)
    var groupContainers: MutableList<NormalContainer>? = null
) : Container {
    companion object {
        const val classType = "normal"
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
