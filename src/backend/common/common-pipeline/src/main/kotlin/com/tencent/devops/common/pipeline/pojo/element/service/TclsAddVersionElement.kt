package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.enums.TclsType
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("TCLS-升级版本", description = TclsAddVersionElement.classType)
data class TclsAddVersionElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "TCLS-升级版本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("TCLS业务ID", required = true)
    val tclsAppId: String?,
    @ApiModelProperty("是否属于MTCLS业务", required = false)
    val mtclsApp: TclsType?,
    @ApiModelProperty("业务ServiceID", required = false)
    val serviceId: String? = null,
    @ApiModelProperty("凭证ID", required = true)
    val ticketId: String = "",
    @ApiModelProperty("执行环境", required = true)
    val envId: String = "",
    @ApiModelProperty("当前版本号", required = true)
    val versionFrom: String = "",
    @ApiModelProperty("升级版本号", required = true)
    val versionTo: String = "",
    @ApiModelProperty("升级包描述", required = true)
    val desc: String = "",
    @ApiModelProperty("升级包名称", required = true)
    val pkgName: String = "",
    @ApiModelProperty("升级包下载URL", required = true)
    val httpUrl: String = "",
    @ApiModelProperty("升级包Hash值", required = true)
    val fileHash: String = "",
    @ApiModelProperty("升级包大小", required = true)
    val size: String = "",
    @ApiModelProperty("升级策略", required = true)
    val updateWay: String = "",
    @ApiModelProperty("完整性校验hash文件URL", required = false)
    val hashUrl: String = "",
    @ApiModelProperty("完整性校验hash文件MD5", required = false)
    val hashMd5: String = "",
    @ApiModelProperty("版本升级包自定义字段", required = false)
    val customStr: String = "",
    @ApiModelProperty("升级包类型(手游)", required = false)
    val updatePkgType: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "tclsAddVersion"
    }

    override fun getTaskAtom(): String {
        return "tclsAddVersionTaskAtom"
    }

    override fun getClassType() = classType
}
