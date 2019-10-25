package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IOS证书安装", description = IosCertInstallElement.classType)
data class IosCertInstallElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "ios证书安装",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("用户在上传证书时指定的ID号", required = false)
    val certId: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "iosCertInstall"
    }

    override fun getClassType() = classType
}
