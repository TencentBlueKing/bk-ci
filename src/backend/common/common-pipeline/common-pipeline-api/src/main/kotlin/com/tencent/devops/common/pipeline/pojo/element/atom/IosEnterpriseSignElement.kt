package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("企业证书签名并归档", description = IosEnterpriseSignElement.classType)
data class IosEnterpriseSignElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "ios企业证书签名并推送",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("ipa文件", required = true)
    val ipaFile: String = "",
    @ApiModelProperty("上传到的目标路径（仅在自定义归档选择才用到）", required = false)
    val destPath: String?,
    @ApiModelProperty("是否自定义归档", required = false)
    val customize: Boolean?,
    @ApiModelProperty("证书ID", required = false)
    val certId: String?
) : Element(name, id, status) {
    companion object {
        const val classType = "enterpriseSign"
    }

    override fun getClassType() = classType
}
