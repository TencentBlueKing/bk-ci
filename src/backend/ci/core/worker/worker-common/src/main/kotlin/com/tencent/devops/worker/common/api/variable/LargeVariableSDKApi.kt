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

package com.tencent.devops.worker.common.api.variable

import com.tencent.devops.process.utils.BuildVarOverflowUtils.LargeVarRef
import com.tencent.devops.worker.common.api.WorkerRestApiSDK

/**
 * 大变量文件在制品库中的归属：按构建隔离，以发起人身份读写。
 *
 * @property vmBuildEnv 是否为构建环境 Job；走文件网关（临时令牌）时据此选择 devnet / idc 网关，与构件归档一致
 * @property pipelineName / buildNum 用于给流水线仓库中的流水线、构建目录设置展示名，与构件归档一致
 */
data class LargeVariableScope(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val userId: String,
    val vmBuildEnv: Boolean,
    val pipelineName: String? = null,
    val buildNum: String? = null
)

/**
 * Worker 直连制品库 variable 仓库读写大变量文件（路径见 BuildVarOverflowUtils.blobPath）。
 *
 * 与构件归档（ArchiveUtils.archivePipelineFiles）的制品库对接方式保持一致：
 * 配置了制品库文件网关时用临时令牌经文件网关读写，否则经构建网关直连。
 */
interface LargeVariableSDKApi : WorkerRestApiSDK {

    /** 当前制品库是否支持存放大变量（仅 bkrepo）；不支持时按 4K 截断的历史行为处理。 */
    fun isSupported(): Boolean

    /** 上传 [ref] 对应的文件，失败抛异常。 */
    fun upload(scope: LargeVariableScope, ref: LargeVarRef, content: ByteArray)

    /** 文件不存在返回 null；内容超过大变量上限时抛异常。 */
    fun download(scope: LargeVariableScope, ref: LargeVarRef): ByteArray?
}
