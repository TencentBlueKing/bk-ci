package com.tencent.devops.plugin.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.plugin.element.DelayElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验", description = DelayElement.classType)
data class ExperienceElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "转体验",
    @ApiModelProperty("id", required = false, hidden = true)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("路径", required = true)
    val path: String = "",
    @ApiModelProperty("是否自定义仓库", required = true)
    val customized: Boolean,
    @ApiModelProperty("时间类型(ABSOLUTE, RELATIVE)", required = true)
    val timeType: String = "ABSOLUTE",
    @ApiModelProperty("结束时间(s)或者结束天数(day)", required = true)
    val expireDate: Long,
    @ApiModelProperty("体验组", required = true)
    val experienceGroups: Set<String> = setOf(),
    @ApiModelProperty("内部名单", required = true)
    val innerUsers: Set<String> = setOf(),
    @ApiModelProperty("外部名单", required = true)
    val outerUsers: String = "",
    @ApiModelProperty("通知类型(RTX,WECHAT,EMAIL)", required = true)
    val notifyTypes: Set<String> = setOf(),
    @ApiModelProperty("是否开启企业微信群通知", required = true)
    val enableGroupId: Boolean? = true,
    @ApiModelProperty("企业微信群ID(逗号分隔)", required = true)
    val groupId: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "experience"
    }

    override fun getTaskAtom() = "experienceTaskAtom"

    override fun getClassType() = classType
}
