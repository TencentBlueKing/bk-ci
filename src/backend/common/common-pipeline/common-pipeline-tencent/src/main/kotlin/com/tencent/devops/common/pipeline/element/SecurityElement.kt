package com.tencent.devops.common.pipeline.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("APK加固", description = SecurityElement.classType)
data class SecurityElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "APK加固",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("待加固的文件(支持正则表达式，只支持单个文件)", required = true)
    val apkFile: String = "",
    @ApiModelProperty("加固环境id", required = true)
    val envId: String = "",
    @ApiModelProperty("是否异步", required = true)
    val asynchronous: Boolean? = true,
    @ApiModelProperty("源类型，“CUSTOM”:自定义仓库，“PIPELINE”:流水线仓库", required = true)
    val sourceType: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "securityFile"
    }

    override fun getTaskAtom(): String {
        return "securityTaskAtom"
    }

    override fun getClassType() = classType
}
