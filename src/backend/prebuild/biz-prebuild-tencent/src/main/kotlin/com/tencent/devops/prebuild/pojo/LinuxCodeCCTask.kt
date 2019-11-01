package com.tencent.devops.prebuild.pojo

import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.coverity.ProjectLanguage
import com.tencent.devops.common.pipeline.pojo.element.atom.LinuxCodeCCScriptElement
import com.tencent.devops.prebuild.service.PreBuildConfig
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CodeCC代码检查任务(service端)")
open class LinuxCodeCCTask(
    @ApiModelProperty("id", required = false)
    override var displayName: String,
    @ApiModelProperty("入参", required = true)
    override val input: LinuxCodeCCInput
) : AbstractTask(displayName, input) {
    companion object {
        const val classType = "linuxCodeCCScript"
    }

    override fun getClassType() = classType

    override fun covertToElement(config: PreBuildConfig): LinuxCodeCCScriptElement {
        return LinuxCodeCCScriptElement(
                displayName,
                null,
                null,
                input.scriptType,
                input.script,
                null,
                null,
                input.languages,
                input.asynchronous,
                input.scanType,
                input.path,
                input.compilePlat,
                input.tools,
                input.pyVersion,
                input.eslintRc,
                input.phpcsStandard,
                input.goPath
        )
    }
}

@ApiModel("CodeCC代码检查任务(service端)")
open class LinuxCodeCCInput(
    @ApiModelProperty("脚本类型", required = true)
    open val scriptType: BuildScriptType,
    @ApiModelProperty("脚本内容", required = true)
    open val script: String = "",
    @ApiModelProperty("工程语言", required = true)
    open val languages: List<ProjectLanguage>,
    @ApiModelProperty("是否异步", required = false)
    open val asynchronous: Boolean? = false,
    @ApiModelProperty("扫描类型（0：全量, 1：增量）", required = false)
    open var scanType: String? = "",
    @ApiModelProperty("代码存放路径", required = false)
    open val path: String? = null,
    @ApiModelProperty("codecc原子执行环境，例如WINDOWS，LINUX，MACOS等", required = false)
    val compilePlat: String? = null,
    @ApiModelProperty("JSONArray格式的字符串\n" +
            "eg：\"[\"COVERITY\",\"CPPLINT\",\"PYLINT\",\"TSCLUA\",\"CCN\",\"DUPC\",\"ESLINT\",\"GOML\",\"KLOCWORK\"]\"，其中\n" +
            "COVERITY：Coverity工具\n" +
            "CPPLINT：cpplint工具\n" +
            "PYLINT：pylint工具\n" +
            "TSCLUA：TSCLUA工具\n" +
            "CCN：圈复杂度工具\n" +
            "DUPC：重复率工具\n" +
            "GOML：go语言检查工具\n" +
            "KLOCWORK：KLOCWORK工具\n" +
            "CHECKSTYLE: CHECKSTYLE工具" +
            "STYLECOP: STYLECOP工具", required = false)
    var tools: List<String>? = null,
    @ApiModelProperty("非必填，当tools列表中有PYLINT时必填；值类型有且仅有两种：“py2”、“py3”，\n" +
            "其中“py2”表示使用python2版本，“py3”表示使用python3版本", required = false)
    val pyVersion: String? = null,
    @ApiModelProperty("eslint项目框架, React, Vue, Other", required = false)
    val eslintRc: String? = null,
    @ApiModelProperty("PHP标准", required = false)
    val phpcsStandard: String? = null,
    @ApiModelProperty("go语言WORKSPACE下相对路径", required = false)
    val goPath: String? = null
) : AbstractInput()