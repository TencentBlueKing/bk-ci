package com.tencent.devops.plugin.element

import com.tencent.devops.common.pipeline.enums.artifactory.SourceType
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("云石分发(IEG专用)", description = CloudStoneElement.classType)
data class CloudStoneElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "云石",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("关联CC业务Id", required = true)
    val ccAppId: String = "",
    @ApiModelProperty("文件上传路径（不支持多个路径），支持正则表达式", required = true)
    val sourcePath: String = "",
    @ApiModelProperty("是否自定义归档(PIPELINE or CUSTOMIZE)", required = true)
    val sourceType: SourceType,
    @ApiModelProperty("文件上传的目标路径(例如: /js_test/codecc.sql)", required = true)
    val targetPath: String = "",
    @ApiModelProperty("发布说明", required = false)
    val releaseNote: String?,
    @ApiModelProperty("版本号", required = true)
    val versionId: String = "",
    @ApiModelProperty("文件类型(server 或者 client)", required = true)
    val fileType: String = "",
    @ApiModelProperty("版本标签", required = false)
    val customFiled: List<String>?
) : Element(name, id, status) {
    companion object {
        const val classType = "cloudStone"
    }

    override fun getTaskAtom() = "cloudStoneTaskAtom"

    override fun getClassType() = classType
}
