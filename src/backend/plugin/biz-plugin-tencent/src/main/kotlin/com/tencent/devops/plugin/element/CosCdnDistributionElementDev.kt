package com.tencent.devops.plugin.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("发布CDN-研发、测试", description = CosCdnDistributionElementDev.classType)
data class CosCdnDistributionElementDev(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "部署-发布CDN系统",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("文件上传路径（多个路径中间逗号隔开），支持正则表达式", required = true)
    val regexPaths: String = "",
    @ApiModelProperty("是否自定义归档", required = true)
    val customize: Boolean,
    @ApiModelProperty("凭证ID", required = true)
    val ticketId: String = "",
    @ApiModelProperty("CDN路径前缀", required = false)
    val cdnPathPrefix: String? = null,
    @ApiModelProperty("分发最长时间（单位：分钟）,默认30分钟", required = false)
    var maxRunningMins: Int = 30
) : Element(name, id, status) {
    companion object {
        const val classType = "cosCdnDistributionDev"
    }

    override fun getTaskAtom() = "cosCdnDistributionDevTaskAtom"

    override fun getClassType() = classType
}
