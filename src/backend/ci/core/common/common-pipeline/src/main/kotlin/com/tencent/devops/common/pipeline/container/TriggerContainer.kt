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

import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模型-构建触发容器")
data class TriggerContainer(
    @get:Schema(title = "构建容器序号id", required = false, readOnly = true)
    override var id: String? = null,
    @get:Schema(title = "容器名称", required = true)
    override var name: String = "",
    @get:Schema(title = "任务集合", required = true)
    override var elements: List<Element> = listOf(),
    @get:Schema(title = "状态", required = true, readOnly = true)
    override var status: String? = null,
    @get:Schema(title = "系统运行时间", required = false)
    @Deprecated("即将被timeCost代替")
    override var startEpoch: Long? = null,
    @get:Schema(title = "系统耗时（开机时间）", required = false, readOnly = true)
    @Deprecated("即将被timeCost代替")
    override var systemElapsed: Long? = null,
    @get:Schema(title = "插件执行耗时", required = false, readOnly = true)
    @Deprecated("即将被timeCost代替")
    override var elementElapsed: Long? = null,
    @get:Schema(title = "参数化构建", required = false)
    var params: List<BuildFormProperty> = listOf(),
    @get:Schema(title = "模板参数构建", required = false)
    var templateParams: List<BuildFormProperty>? = null,
    @get:Schema(title = "构建版本号", required = false)
    var buildNo: BuildNo? = null,
    @get:Schema(title =
        "是否可重试-仅限于构建详情展示重试，目前未作为编排的选项，暂设置为null不存储",
        required = false,
        readOnly = true
    )
    override var canRetry: Boolean? = null,
    @get:Schema(title = "构建容器顺序ID（同id值）", required = false, readOnly = true)
    override var containerId: String? = null,
    @get:Schema(title = "容器唯一ID", required = false, readOnly = true)
    override var containerHashId: String? = null,
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
    override var template: String? = null,
    override var ref: String? = null,
    override var variables: Map<String, String>? = null
) : Container {
    companion object {
        const val classType = "trigger"
    }

    override fun getClassType() = classType

    override fun getContainerById(vmSeqId: String): Container? {
        return if (id == vmSeqId) this else null
    }

    override fun retryFreshMatrixOption() = Unit

    override fun fetchGroupContainers(): List<Container>? = null

    override fun fetchMatrixContext(): Map<String, String>? = null

    override fun containerEnabled(): Boolean {
        return true
    }

    override fun setContainerEnable(enable: Boolean) = Unit

    override fun transformCompatibility() {
        super.transformCompatibility()
    }
}
