package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("GCloud-puffer-动态资源更新(IEG专用)", description = GcloudPufferElement.classType)
data class GcloudPufferElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "GCLOUD-创建APP原子",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("环境配置id", required = true)
    var configId: String,
    @ApiModelProperty("游戏ID", required = true)
    var gameId: String,
    @ApiModelProperty("accessId && accessKey 的ticket id", required = true)
    var ticketId: String,
    @ApiModelProperty("上传文件的accessId && accessKey 的ticket id", required = false)
    var fileTicketId: String?,
    @ApiModelProperty("渠道 ID", required = true)
    var productId: String,
    @ApiModelProperty("资源版本号，格式同 IPv4 点分十进制格式，如 3.3.3.3", required = true)
    var resourceVersion: String,
    @ApiModelProperty("资源名称", required = true)
    var resourceName: String,
    @ApiModelProperty("CDN 是否使用 HTTPS, 可选值 0/1", required = false)
    var https: String?,
    @ApiModelProperty("文件路径，支持正则表达式(不支持逗号分隔多个文件)", required = true)
    var filePath: String,
    @ApiModelProperty("文件来源（PIPELINE-流水线仓库、CUSTOMIZE-自定义仓库）", required = true)
    var fileSource: String,
    @ApiModelProperty("版本标签(0: 不可用，1：正式版本, 2：审核版本）", required = true)
    var versionType: String,
    @ApiModelProperty("版本描述", required = false)
    var versionDes: String?,
    @ApiModelProperty("自定义字符串", required = false)
    var customStr: String?
) : Element(name, id, status) {
    companion object {
        const val classType = "gcloudPuffer"
    }

    override fun getTaskAtom() = "gcloudPufferTaskAtom"

    override fun getClassType() = classType
}
