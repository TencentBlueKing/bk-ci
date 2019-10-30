package com.tencent.devops.plugin.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("GCloud-修改版本设置(IEG专用)", description = GcloudUpdateVersionElement.classType)
data class GcloudUpdateVersionElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "GCloud-资源版本更新(IEG专用)",
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
    var ticketId: String = "",
    @ApiModelProperty("版本标签(0: 测试版本， 1：审核版本）", required = false)
    var versionType: String?,
    @ApiModelProperty("普通用户可用", required = false)
    var normalUserCanUse: String?,
    @ApiModelProperty("灰度用户可用", required = false)
    var grayUserCanUse: String?,
    @ApiModelProperty("灰度规则 ID, 灰度用户可用时必须指定", required = false)
    var grayRuleId: String?,
    @ApiModelProperty("版本描述", required = false)
    var versionDes: String?,
    @ApiModelProperty("自定义字符串", required = false)
    var customStr: String?
) : Element(name, id, status) {
    companion object {
        const val classType = "gcloudUpdateVersion"
    }

    override fun getTaskAtom() = "gcloudUpdateVersionTaskAtom"

    override fun getClassType() = classType
}
