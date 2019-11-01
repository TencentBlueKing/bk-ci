package com.tencent.devops.common.pipeline.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("ITest-创建审核单", description = ItestReviewCreateElement.classType)
data class ItestReviewCreateElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "ITest创建自测单",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("ITest项目ID", required = true)
    val itestProjectId: String = "",
    @ApiModelProperty("ITest API凭证", required = true)
    val ticketId: String = "",
    @ApiModelProperty("版本类型(默认值为0，其中：'0' => '普通版本', '1' => '紧急版本', '2' => '免测版本')", required = true)
    val versionType: String = "",
    @ApiModelProperty("版本号", required = true)
    val versionName: String = "",
    @ApiModelProperty("基线号", required = true)
    val baselineName: String = "",
    @ApiModelProperty("预计发布时间(时间戳)", required = true)
    val releaseTime: Long,
    @ApiModelProperty("测试说明", required = true)
    val description: String = "",
    @ApiModelProperty("是否自定义归档", required = true)
    val customize: Boolean,
    @ApiModelProperty("待测试包", required = true)
    val targetPath: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "itestReviewCreate"
    }

    override fun getTaskAtom(): String {
        return "itestReviewCreateTaskAtom"
    }

    override fun getClassType() = classType
}
