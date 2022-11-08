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
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Suppress("UNUSED")
@ApiModel("CodeCC代码检查任务(service端)", description = LinuxCodeCCScriptElement.classType)
open class LinuxCodeCCScriptElement(
    @ApiModelProperty("任务名称", required = true)
    override var name: String = "执行Linux脚本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("脚本类型", required = true)
    open var scriptType: BuildScriptType = BuildScriptType.SHELL,
    @ApiModelProperty("脚本内容", required = true)
    open var script: String = "",
    @ApiModelProperty("CodeCC Task Name", required = false, hidden = true)
    open var codeCCTaskName: String? = null,
    @ApiModelProperty("CodeCC Task CN Name", required = false, hidden = true)
    open var codeCCTaskCnName: String? = null,
    @ApiModelProperty("工程语言", required = true)
    open var languages: List<ProjectLanguage> = listOf(),
    @ApiModelProperty("是否异步", required = false)
    open var asynchronous: Boolean? = false,
    @ApiModelProperty("扫描类型（0：全量, 1：增量）", required = false)
    open var scanType: String? = "",
    @ApiModelProperty("代码存放路径", required = false)
    open var path: String? = null,
    @ApiModelProperty("codecc原子执行环境，例如WINDOWS，LINUX，MACOS等", required = true)
    var compilePlat: String? = null,
    @ApiModelProperty(
        "JSONArray格式的字符串\n" +
            "eg：\"[\"COVERITY\",\"CPPLINT\",\"PYLINT\",\"TSCLUA\",\"CCN\",\"DUPC\",\"ESLINT\",\"GOML\"" +
            ",\"KLOCWORK\"]\"，其中\n COVERITY：Coverity工具\n" +
            "CPPLINT：cpplint工具\n" +
            "PYLINT：pylint工具\n" +
            "TSCLUA：TSCLUA工具\n" +
            "CCN：圈复杂度工具\n" +
            "DUPC：重复率工具\n" +
            "GOML：go语言检查工具\n" +
            "KLOCWORK：KLOCWORK工具\n" +
            "CHECKSTYLE: CHECKSTYLE工具" +
            "STYLECOP: STYLECOP工具", required = true
    )
    var tools: List<String>? = null,
    @ApiModelProperty(
        "非必填，当tools列表中有PYLINT时必填；值类型有且仅有两种：“py2”、“py3”，\n" +
            "其中“py2”表示使用python2版本，“py3”表示使用python3版本", required = false
    )
    var pyVersion: String? = null,
    @ApiModelProperty("eslint项目框架, React, Vue, Other", required = false)
    var eslintRc: String? = null,
    @ApiModelProperty("PHP标准", required = false)
    var phpcsStandard: String? = null,
    @ApiModelProperty("go语言WORKSPACE下相对路径", required = false)
    var goPath: String? = null,
    @ApiModelProperty("spotbugs相关参数", required = false)
    var projectBuildType: String? = null,
    @ApiModelProperty("spotbugs相关参数", required = false)
    var projectBuildCommand: String? = null,
    @ApiModelProperty("圈复杂度阈值", required = false)
    var ccnThreshold: String? = null,
    @ApiModelProperty("是否隐藏代码内容，字符串的false和true", required = false)
    var needCodeContent: String? = null,
    var coverityToolSetId: String? = null,
    var klocworkToolSetId: String? = null,
    var cpplintToolSetId: String? = null,
    var eslintToolSetId: String? = null,
    var pylintToolSetId: String? = null,
    var gometalinterToolSetId: String? = null,
    var checkStyleToolSetId: String? = null,
    var styleCopToolSetId: String? = null,
    var detektToolSetId: String? = null,
    var phpcsToolSetId: String? = null,
    var sensitiveToolSetId: String? = null,
    var occheckToolSetId: String? = null,
    var gociLintToolSetId: String? = null,
    var woodpeckerToolSetId: String? = null,
    var horuspyToolSetId: String? = null,
    var pinpointToolSetId: String? = null
) : Element(name, id, status) {

    companion object {
        const val classType = "linuxCodeCCScript"
    }

    override fun getClassType() = classType

    constructor(): this(
        name = "",
        id = "",
        status = "",
        scriptType = BuildScriptType.SHELL,
        script = "",
        codeCCTaskName = "",
        codeCCTaskCnName = "",
        languages = listOf(),
        asynchronous = true,
        scanType = "",
        path = "",
        compilePlat = "",
        tools = listOf(),
        pyVersion = null,
        eslintRc = null,
        phpcsStandard = null,
        goPath = null,
        projectBuildType = null,
        projectBuildCommand = null,
        ccnThreshold = null,
        needCodeContent = null,
        coverityToolSetId = null,
        klocworkToolSetId = null,
        cpplintToolSetId = null,
        eslintToolSetId = null,
        pylintToolSetId = null,
        gometalinterToolSetId = null,
        checkStyleToolSetId = null,
        styleCopToolSetId = null,
        detektToolSetId = null,
        phpcsToolSetId = null,
        sensitiveToolSetId = null,
        occheckToolSetId = null,
        gociLintToolSetId = null,
        woodpeckerToolSetId = null,
        horuspyToolSetId = null
    )
}
