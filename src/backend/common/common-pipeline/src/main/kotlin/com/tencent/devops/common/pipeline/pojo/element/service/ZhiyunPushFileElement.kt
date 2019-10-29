package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("织云-推送文件", description = ZhiyunPushFileElement.classType)
data class ZhiyunPushFileElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "推送文件到织云",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("业务名", required = true)
    val product: String = "",
    @ApiModelProperty("包名", required = true)
    val packageName: String = "",
    @ApiModelProperty("版本描述", required = true)
    val description: String = "",
    @ApiModelProperty("是否先删除上一个版本的包的所有文件", required = true)
    val clean: Boolean,
    @ApiModelProperty("文件来源（PIPELINE-流水线仓库、CUSTOMIZE-自定义仓库）", required = true)
    val fileSource: String = "",
    @ApiModelProperty("文件路径，支持正则表达式(不支持逗号分隔多个文件)", required = true)
    val filePath: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "zhiyunPushFileElement"
    }

    override fun getTaskAtom(): String {
        return "zhiYunPushFileTaskAtom"
    }

    override fun getClassType() = classType
}
