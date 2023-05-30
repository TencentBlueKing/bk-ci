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

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 *  互斥组
 */
@ApiModel("互斥组模型")
data class MutexGroup(
    @ApiModelProperty("是否启用", required = false)
    val enable: Boolean,
    @ApiModelProperty("互斥组名称", required = false)
    val mutexGroupName: String? = "",
    @ApiModelProperty("是否排队", required = false)
    val queueEnable: Boolean,
    @ApiModelProperty("排队等待时间（分钟）0表示不等待直接失败", required = false)
    var timeout: Int = 0,
    @ApiModelProperty("支持变量解析的timeout，变量值非数字则会改取timeout值", required = false)
    var timeoutVar: String? = null,
    @ApiModelProperty("排队队列大小", required = false)
    val queue: Int = 0,
    @ApiModelProperty("运行时实际互斥锁名称（有值则已初始化）", required = false)
    var runtimeMutexGroup: String? = null,
    @ApiModelProperty("占用锁定的信息用于日志提示", required = false)
    var linkTip: String? = null // #5454 占用锁定的信息用于日志提示/不写入到Model，仅在构建开始时产生
) {
    fun fetchRuntimeMutexGroup() = runtimeMutexGroup ?: mutexGroupName ?: ""

    fun genMutexLockKey(projectId: String): String {
        val mutexGroupName = fetchRuntimeMutexGroup()
        return "lock:container:mutex:$projectId:$mutexGroupName:lock"
    }

    fun genMutexQueueKey(projectId: String): String {
        val mutexGroupName = fetchRuntimeMutexGroup()
        return "lock:container:mutex:$projectId:$mutexGroupName:queue"
    }

    fun genMutexLinkTipKey(containerMutexId: String): String {
        return "linkTip:$containerMutexId"
    }
}
