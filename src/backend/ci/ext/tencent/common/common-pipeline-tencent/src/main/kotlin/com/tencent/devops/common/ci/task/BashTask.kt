package com.tencent.devops.common.ci.task

import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * BashTask
 */
@ApiModel("脚本任务（linux和macOS环境）")
data class BashTask(
    @ApiModelProperty("displayName", required = false)
    override var displayName: String?,
    @ApiModelProperty("入参", required = true)
    override val inputs: LinuxScriptInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {
    companion object {
        const val taskType = "bash"
        const val taskVersion = "@latest"
    }

    override fun getTaskType() = taskType
    override fun getTaskVersion() = taskVersion

    override fun covertToElement(config: CiBuildConfig): LinuxScriptElement {
        return LinuxScriptElement(
                displayName ?: "Bash",
                null,
                null,
                inputs.scriptType ?: BuildScriptType.SHELL,
                inputs.content,
                inputs.continueOnError ?: false
        )
    }
}

@ApiModel("脚本任务（linux和macOS环境）")
data class LinuxScriptInput(
    @ApiModelProperty("脚本类型", required = true)
    val scriptType: BuildScriptType?,
    @ApiModelProperty("脚本内容", required = true)
    val content: String,
    @ApiModelProperty("某次执行为非0时（失败）是否继续执行脚本", required = false)
    val continueOnError: Boolean?
) : AbstractInput()