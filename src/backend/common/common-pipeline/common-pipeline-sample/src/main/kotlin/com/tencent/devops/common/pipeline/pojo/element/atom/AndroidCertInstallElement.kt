package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Android证书安装", description = AndroidCertInstallElement.classType)
data class AndroidCertInstallElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "Android证书安装",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("用户在上传证书时指定的ID号", required = false)
    val certId: String = "",
    @ApiModelProperty("目标路径", required = false)
    val destPath: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "androidCertInstall"
    }

    override fun getClassType() = classType
}
