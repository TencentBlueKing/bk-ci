package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("部署-作业平台", description = JobExecuteTaskExtElement.classType)
data class JobExecuteTaskExtElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "部署-作业平台",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("业务ID", required = true)
    val appId: Int,
    @ApiModelProperty("作业ID", required = true)
    val taskId: Int,
    @ApiModelProperty("全局参数", required = true)
    val globalVar: String? = null,
    @ApiModelProperty("超时时间(s)", required = true)
    val timeout: Int
) : Element(name, id, status) {
    companion object {
        const val classType = "jobExecuteTaskExt"
    }

    override fun getTaskAtom() = "jobExecuteTaskExtAtom"

    override fun getClassType() = classType
}
