package com.tencent.devops.plugin.element

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("作业平台-作业执行", description = JobDevOpsExecuteTaskExtElement.classType)
data class JobDevOpsExecuteTaskExtElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "执行job作业",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("作业ID", required = true)
    val taskId: Int,
    @ApiModelProperty("全局参数", required = true)
    val globalVar: Map<String, String> = mapOf(),
    @ApiModelProperty("超时时间(s)", required = true)
    val timeout: Int
) : Element(name, id, status) {
    companion object {
        const val classType = "jobDevOpsExecuteTaskExt"
    }

    override fun getTaskAtom(): String {
        return "jobDevOpsExecuteTaskExtTaskAtom"
    }

    override fun getClassType() = classType

    private fun getSetParameters(taskParameters: Map<String, String>): String {
        return ObjectMapper().writeValueAsString(taskParameters)
    }
}
