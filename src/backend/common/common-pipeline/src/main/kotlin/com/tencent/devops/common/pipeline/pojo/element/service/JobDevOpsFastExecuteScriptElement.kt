package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("作业平台-脚本执行", description = JobDevOpsFastExecuteScriptElement.classType)
data class JobDevOpsFastExecuteScriptElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "JOB快速执行脚本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("脚本内容", required = true)
    val content: String = "",
    @ApiModelProperty("超时时间", required = true)
    val scriptTimeout: Int,
    @ApiModelProperty("脚本参数", required = true)
    val scriptParams: String? = null,
    @ApiModelProperty("脚本参数", required = true)
    val paramSensitive: Boolean,
    @ApiModelProperty("脚本类型", required = true)
    val type: Int,
    @ApiModelProperty("选择环境类型", required = true)
    val envType: String = "",
    @ApiModelProperty("环境ID", required = false)
    val envId: List<String>?,
    @ApiModelProperty("环境名称", required = false)
    val envName: List<String>?,
    @ApiModelProperty("节点ID", required = false)
    val nodeId: List<String>?,
    @ApiModelProperty("目标机器账户名", required = true)
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
