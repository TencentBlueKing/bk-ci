package com.tencent.devops.common.pipeline.pojo.element.market

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("验证镜像合法性", description = MarketCheckImageElement.classType)
data class MarketCheckImageElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "验证镜像合法性",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("用户名", required = false)
    val registryUser: String? = null,
    @ApiModelProperty("密码", required = false)
    val registryPwd: String? = null
) : Element(name, id, status) {

    companion object {
        const val classType = "marketCheckImage"
    }

    override fun getClassType() = classType
}
