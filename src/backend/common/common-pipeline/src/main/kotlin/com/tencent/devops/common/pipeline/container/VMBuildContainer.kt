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

import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.type.DispatchType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线模型-虚拟机构建容器")
data class VMBuildContainer(
    @ApiModelProperty("构建容器序号id", required = false, hidden = true)
    override var id: String? = null,
    @ApiModelProperty("容器名称", required = true)
    override var name: String = "构建环境",
    @ApiModelProperty("任务集合", required = true)
    override val elements: List<Element> = listOf(),
    override var status: String? = null,
    override var startEpoch: Long? = null,
    @ApiModelProperty("系统运行时间", required = false, hidden = true)
    override var systemElapsed: Long? = null,
    @ApiModelProperty("插件运行时间", required = false, hidden = true)
    override var elementElapsed: Long? = null,
    @ApiModelProperty("VM基础操作系统", required = true)
    val baseOS: VMBaseOS,
    @ApiModelProperty("预指定VM名称列表", required = true)
    val vmNames: Set<String> = setOf(),
    @ApiModelProperty("排队最长时间(分钟)", required = true)
    val maxQueueMinutes: Int? = 60,
    @ApiModelProperty("运行最长时间(分钟)", required = true)
    val maxRunningMinutes: Int = 480,
    @ApiModelProperty("构建机环境变量", required = false)
    val buildEnv: Map<String, String>?,
    @ApiModelProperty("用户自定义环境变量", required = false)
    val customBuildEnv: Map<String, String>?,
    @ApiModelProperty("第三方构建Hash ID", required = false)
    val thirdPartyAgentId: String?,
    @ApiModelProperty("第三方构建环境ID", required = false)
    val thirdPartyAgentEnvId: String?,
    @ApiModelProperty("第三方构建环境工作空间", required = false)
    val thirdPartyWorkspace: String?,
    @ApiModelProperty("Docker构建机", required = false)
    val dockerBuildVersion: String?,
    @ApiModelProperty("新的选择构建机环境", required = false)
    val dispatchType: DispatchType?,
    @ApiModelProperty("是否可重试", required = false, hidden = true)
    override var canRetry: Boolean? = false,
    @ApiModelProperty("是否访问外网", required = false, hidden = true)
    var enableExternal: Boolean? = false,
    override var containerId: String? = null,
    @ApiModelProperty("流程控制选项", required = true)
    var jobControlOption: JobControlOption? = null, // 为了兼容旧数据，所以定义为可空以及var
    @ApiModelProperty("互斥组", required = false)
    var mutexGroup: MutexGroup? = null // 为了兼容旧数据，所以定义为可空以及var
) : Container {
    companion object {
        const val classType = "vmBuild"
    }

    override fun getClassType() = classType
}
