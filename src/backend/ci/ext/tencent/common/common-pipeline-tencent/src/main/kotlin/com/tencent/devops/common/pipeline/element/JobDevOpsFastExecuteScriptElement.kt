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

@Schema(title = "作业平台-脚本执行")
data class JobDevOpsFastExecuteScriptElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "JOB快速执行脚本",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "脚本内容", required = true)
    val content: String = "",
    @get:Schema(title = "超时时间", required = true)
    val scriptTimeout: Int,
    @get:Schema(title = "脚本参数", required = true)
    val scriptParams: String? = null,
    @get:Schema(title = "脚本参数", required = true)
    val paramSensitive: Boolean,
    @get:Schema(title = "脚本类型", required = true)
    val type: Int,
    @get:Schema(title = "选择环境类型", required = true)
    val envType: String = "",
    @get:Schema(title = "环境ID", required = false)
    val envId: List<String>?,
    @get:Schema(title = "环境名称", required = false)
    val envName: List<String>?,
    @get:Schema(title = "节点ID", required = false)
    val nodeId: List<String>?,
    @get:Schema(title = "目标机器账户名", required = true)
    val account: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "jobDevOpsFastExecuteScript"
    }

    override fun getTaskAtom(): String {
        return "jobDevOpsFastExecuteScriptTaskAtom"
    }

    override fun getClassType() = classType
}
