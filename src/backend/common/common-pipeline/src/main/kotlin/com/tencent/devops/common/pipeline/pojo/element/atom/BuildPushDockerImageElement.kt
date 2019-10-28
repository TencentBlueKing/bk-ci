package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("构建并推送Docker镜像", description = BuildPushDockerImageElement.classType)
data class BuildPushDockerImageElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "执行脚本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("镜像名称", required = true)
    val imageName: String = "",
    @ApiModelProperty("镜像TAG", required = false)
    val imageTag: String? = "latest",
    @ApiModelProperty("build目录", required = false)
    val buildDir: String? = ".",
    @ApiModelProperty("Dockerfile位置", required = false)
    val dockerFile: String? = "Dockerfile"
) : Element(name, id, status) {

    companion object {
        const val classType = "buildPushDockerImage"
    }

    override fun getClassType() = classType
}
