package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("推送镜像到第三方仓库", description = PushImageToThirdRepoElement.classType)
data class PushImageToThirdRepoElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "推送镜像到第三方仓库",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("源镜像名称", required = true)
    val srcImageName: String,
    @ApiModelProperty("源镜像tag", required = true)
    val srcImageTag: String,
    @ApiModelProperty("第三方仓库地址", required = false)
    val repoAddress: String?,
    @ApiModelProperty("凭证ID", required = true)
    val ticketId: String = "",
    @ApiModelProperty("镜像名称", required = true)
    val targetImageName: String,
    @ApiModelProperty("镜像tag", required = true)
    val targetImageTag: String,
    @ApiModelProperty("镜像关联的cmdb的ID", required = false)
    val cmdbId: String?,
    @ApiModelProperty("启用oa验证", required = false)
    val verifyByOa: Boolean?

) : Element(name, id, status) {
    companion object {
        const val classType = "pushImageToThirdRepo"
    }

    override fun getTaskAtom() = "pushImageToThirdRepoTaskAtom"

    override fun getClassType() = classType
}
