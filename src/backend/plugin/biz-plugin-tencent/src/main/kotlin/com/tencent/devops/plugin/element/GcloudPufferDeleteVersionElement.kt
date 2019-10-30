package com.tencent.devops.plugin.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("GCloud-Puffer-删除版本(IEG专用)", description = GcloudPufferDeleteVersionElement.classType)
data class GcloudPufferDeleteVersionElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "GCloud-Puffer-删除版本(IEG专用)",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("环境配置id", required = true)
    var configId: String = "",
    @ApiModelProperty("渠道 ID", required = true)
    var productId: String = "",
    @ApiModelProperty("游戏 ID", required = true)
    var gameId: String = "",
    @ApiModelProperty("版本号，格式同 IPv4 点分十进制格式，如 3.3.3.3", required = true)
    var versionStr: String = "",
    @ApiModelProperty("凭证id", required = true)
    var ticketId: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "gcloudPufferDeleteVersion"
    }

    override fun getTaskAtom() = "gcloudPufferDeleteVersionTaskAtom"

    override fun getClassType() = classType
}
