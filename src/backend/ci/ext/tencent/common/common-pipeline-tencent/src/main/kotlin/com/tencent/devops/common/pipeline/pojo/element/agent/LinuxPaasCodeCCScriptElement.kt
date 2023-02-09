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

package com.tencent.devops.common.pipeline.pojo.element.agent

import com.tencent.devops.common.pipeline.enums.BuildScriptType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Suppress("UNUSED")
@ApiModel("CodeCC代码检查任务", description = LinuxPaasCodeCCScriptElement.classType)
data class LinuxPaasCodeCCScriptElement(
    @ApiModelProperty("任务名称", required = true)
    override var name: String = "执行Linux脚本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("脚本类型", required = true)
    override var scriptType: BuildScriptType = BuildScriptType.SHELL,
    @ApiModelProperty("脚本内容", required = true)
    override var script: String = "",
    @ApiModelProperty("CodeCC Task Name", required = false, hidden = true)
    override var codeCCTaskName: String? = null,
    @ApiModelProperty("CodeCC Task CN Name", required = false, hidden = true)
    override var codeCCTaskCnName: String? = null,
    @ApiModelProperty("CodeCC Task Id", required = false, hidden = true)
    var codeCCTaskId: String? = null,
    @ApiModelProperty("是否异步", required = false)
    override var asynchronous: Boolean? = false,
    @ApiModelProperty("扫描类型（0：全量, 1：增量）", required = false)
    override var scanType: String? = null,
    @ApiModelProperty("代码存放路径", required = false)
    override var path: String? = null,
    @ApiModelProperty("工程语言", required = true)
    override var languages: List<ProjectLanguage> = listOf()
) : LinuxCodeCCScriptElement(
    name,
    id,
    status,
    scriptType,
    script,
    codeCCTaskName,
    codeCCTaskCnName,
    languages,
    asynchronous,
    scanType,
    path
) {

    companion object {
        const val classType = "linuxPaasCodeCCScript"
    }

    override fun cleanUp() {
        codeCCTaskId = null
        codeCCTaskName = null
    }

    override fun getClassType() =
        classType

    constructor(): this(
        name = "",
        id = "",
        status = null,
        scriptType = BuildScriptType.SHELL,
        script = "",
        codeCCTaskName = "",
        codeCCTaskCnName = "",
        codeCCTaskId = "",
        asynchronous = true,
        scanType = "",
        path = "",
        languages = listOf()
    )
}
