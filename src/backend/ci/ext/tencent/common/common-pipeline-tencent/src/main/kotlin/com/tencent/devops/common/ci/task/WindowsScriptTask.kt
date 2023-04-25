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

package com.tencent.devops.common.ci.task

import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.CharsetType
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * WindowsScriptTask
 */
@ApiModel("脚本任务（win环境）")
data class WindowsScriptTask(
    @ApiModelProperty("displayName", required = false)
    override var displayName: String?,
    @ApiModelProperty("入参", required = true)
    override val inputs: WindowsScriptInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {
    companion object {
        const val taskType = "windowsScript"
        const val taskVersion = "@latest"
    }

    override fun covertToElement(config: CiBuildConfig): WindowsScriptElement {
        return WindowsScriptElement(
            name = displayName ?: "WindowsScript",
            id = null,
            status = null,
            script = inputs.content,
            scriptType = inputs.scriptType ?: BuildScriptType.BAT,
            charsetType = inputs.charsetType ?: CharsetType.DEFAULT
        )
    }
}

@ApiModel("脚本任务（win环境）")
data class WindowsScriptInput(
    @ApiModelProperty("脚本内容", required = true)
    val content: String,
    @ApiModelProperty("脚本类型", required = true)
    val scriptType: BuildScriptType?,
    @ApiModelProperty("字符集类型", required = false)
    val charsetType: CharsetType?

) : AbstractInput()
