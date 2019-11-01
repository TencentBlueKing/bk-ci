package com.tencent.devops.common.pipeline.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("GSEKIT-执行命令-研发测试", description = GseKitProcRunCmdElementDev.classType)
data class GseKitProcRunCmdElementDev(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "GSEKIT-进程管理-执行命令",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("CC业务ID", required = true)
    val appId: Int,
    @ApiModelProperty("GSEKIT上的环境ID", required = true)
    val envId: Int,
    @ApiModelProperty("命令", required = true)
    val cmd: String = "",
    @ApiModelProperty("进程ID", required = true)
    val procId: String = "",
    @ApiModelProperty("参数", required = false)
    val params: List<String> = emptyList(),
    @ApiModelProperty("单次滚动并发数", required = true)
    val concurrency: Int,
    @ApiModelProperty("超时时间(分钟)", required = false)
    val timeout: Int = 30
) : Element(name, id, status) {
    companion object {
        const val classType = "gseKitProcRunCmdDev"
    }

    override fun getTaskAtom() = "gseKitProcRunCmdTaskAtomDev"

    override fun getClassType() = classType
}
