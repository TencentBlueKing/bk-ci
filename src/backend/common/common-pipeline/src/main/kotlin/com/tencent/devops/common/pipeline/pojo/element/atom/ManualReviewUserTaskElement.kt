package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("人工审核", description = ManualReviewUserTaskElement.classType)
data class ManualReviewUserTaskElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "人工审核",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("审核人", required = true)
    var reviewUsers: MutableList<String> = mutableListOf(),
    @ApiModelProperty("描述", required = false)
    var desc: String? = "",
    @ApiModelProperty("审核意见", required = false)
    var suggest: String? = "",
    @ApiModelProperty("参数列表", required = false)
    var params: MutableList<ManualReviewParamPair> = mutableListOf()

) : Element(name, id, status) {
    companion object {
        const val classType = "manualReviewUserTask"
    }

    override fun getTaskAtom() = "manualReviewTaskAtom"

    override fun getClassType() = classType
}
