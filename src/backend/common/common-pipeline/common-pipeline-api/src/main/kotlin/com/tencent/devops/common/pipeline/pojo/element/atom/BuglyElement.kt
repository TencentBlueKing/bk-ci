package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.enums.Platform
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("bugly符号表上传", description = BuglyElement.classType)
data class BuglyElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "bugly异常上报",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("版本号", required = false)
    val versionId: String = "",
    @ApiModelProperty("待上传给bugly的符号表文件夹(选择了安卓平台才显示，只能填一个路径，不支持正则匹配)", required = false)
    val buglyFolder: String = "",
    @ApiModelProperty("App所在的文件夹", required = false)
    val appFolder: String = "",
    @ApiModelProperty("凭证id", required = true)
    val credId: String = "",
    @ApiModelProperty("平台类型（ANDROID或者IPHONE）", required = true)
    val platform: Platform
) : Element(name, id, status) {
    companion object {
        const val classType = "bugly"
    }

    override fun getClassType() = classType
}
