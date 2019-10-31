package com.tencent.devops.gitci.pojo.task

import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.coverity.ProjectLanguage
import com.tencent.devops.common.pipeline.pojo.element.atom.LinuxPaasCodeCCScriptElement
import com.tencent.devops.gitci.service.BuildConfig
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CodeCC代码检查任务(service端)")
open class LinuxCodeCCTask(
    @ApiModelProperty("displayName", required = false)
    override var displayName: String?,
    @ApiModelProperty("入参", required = true)
    override val inputs: LinuxPassCodeCCInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {
    companion object {
        const val taskType = "linuxCodeCCScript"
        const val taskVersion = "@latest"
    }

    override fun getTaskType() = taskType
    override fun getTaskVersion() = taskVersion

    override fun covertToElement(config: BuildConfig): LinuxPaasCodeCCScriptElement {
        val element = LinuxPaasCodeCCScriptElement(
                displayName ?: "CodeCCScript",
                null,
                null,
                inputs.scriptType,
                inputs.script,
                null,
                null,
                null,
                inputs.asynchronous,
                inputs.scanType,
                inputs.path,
                inputs.languages
        )
        element.compilePlat = inputs.compilePlat
        element.tools = inputs.tools
        element.pyVersion = inputs.pyVersion
        element.eslintRc = inputs.eslintRc
        element.phpcsStandard = inputs.phpcsStandard
        element.goPath = inputs.goPath
        element.ccnThreshold = inputs.ccnThreshold
        element.needCodeContent = inputs.needCodeContent
        return element
    }
}

@ApiModel("CodeCC代码检查任务(service端)")
open class LinuxPassCodeCCInput(
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
    val goPath: String? = null,
    @ApiModelProperty("圈复杂度阈值", required = false)
    val ccnThreshold: Int? = null,
    @ApiModelProperty("是否隐藏代码内容，字符串的false和true", required = false)
    val needCodeContent: String? = null
) : AbstractInput()