package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("部署-发布CDN", description = SpmDistributionElement.classType)
data class SpmDistributionElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "部署-发布CDN",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("CMDB业务ID", required = true)
    val cmdbAppId: Int,
    @ApiModelProperty("CMDB业务名称", required = false)
    var cmdbAppName: String = "",
    @ApiModelProperty("一级目录", required = false)
    var rootPath: String = "",
    @ApiModelProperty("SPM密钥", required = true)
    val secretKey: String = "",
    @ApiModelProperty("文件上传路径（多个路径中间逗号隔开），支持正则表达式", required = true)
    val regexPaths: String = "",
    @ApiModelProperty("是否自定义归档", required = true)
    val customize: Boolean,
    @ApiModelProperty("分发最长时间（单位：秒）,默认10分钟", required = false)
    var maxRunningMins: Int = 600
) : Element(name, id, status) {
    companion object {
        const val classType = "spmDistribution"
    }

    override fun getTaskAtom(): String {
        return "spmDistributionTaskAtom"
    }

    override fun getClassType() = classType
}
