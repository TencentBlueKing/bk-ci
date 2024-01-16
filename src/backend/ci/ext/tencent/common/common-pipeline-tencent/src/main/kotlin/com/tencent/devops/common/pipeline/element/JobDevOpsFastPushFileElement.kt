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

package com.tencent.devops.common.pipeline.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "作业平台-构件分发")
data class JobDevOpsFastPushFileElement(
    @Schema(description = "任务名称", required = true)
    override val name: String = "JOB快速执行脚本",
    @Schema(description = "id", required = false)
    override var id: String? = null,
    @Schema(description = "状态", required = false)
    override var status: String? = null,
    @Schema(description = "源类型", required = false)
    val srcType: String = "",
    @Schema(description = "源路径", required = false)
    val srcPath: String = "",
    @Schema(description = "源节点id", required = false)
    val srcNodeId: String = "",
    @Schema(description = "源服务器账户", required = false)
    val srcAccount: String = "",
    @Schema(description = "目标路径", required = false)
    val targetPath: String = "",
    @Schema(description = "目标账户", required = false)
    val targetAccount: String = "",
    @Schema(description = "目标节点id列表", required = false)
    val targetNodeId: List<String>?,
    @Schema(description = "目标环境id列表", required = false)
    val targetEnvId: List<String>?,
    @Schema(description = "目标环境名称列表", required = false)
    val targetEnvName: List<String>?,
    @Schema(description = "目标环境类型", required = false)
    val targetEnvType: String = "",
    @Schema(description = "超时时间", required = true)
    val timeout: Int? = 600
) : Element(name, id, status) {
    companion object {
        const val classType = "jobDevOpsFastPushFile"
    }

    override fun getTaskAtom(): String {
        return "jobDevOpsFastPushFileTaskAtom"
    }

    override fun getClassType() = classType
}
