package com.tencent.devops.prebuild.pojo

import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.prebuild.service.PreBuildConfig
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * LinuxScriptElement
 */
@ApiModel("脚本任务（linux和macOS环境）")
data class LinuxScriptTask(
    @ApiModelProperty("id", required = false)
    override var displayName: String,
    @ApiModelProperty("入参", required = true)
    override val input: LinuxScriptInput
) : AbstractTask(displayName, input) {
    companion object {
        const val classType = "linuxScriptTask"
    }

    override fun getClassType() = classType

    override fun covertToElement(config: PreBuildConfig): LinuxScriptElement {
        return LinuxScriptElement(
                displayName,
                null,
                null,
                input.scriptType,
                input.script,
                false
        )
    }
}

@ApiModel("脚本任务（linux和macOS环境）")
data class LinuxScriptInput(
    @ApiModelProperty("脚本类型", required = true)
    val scriptType: BuildScriptType,
    @ApiModelProperty("脚本内容", required = true)
    val script: String,
    @ApiModelProperty("某次执行为非0时（失败）是否继续执行脚本", required = false)
    val continueNoneZero: Boolean?
) : AbstractInput()