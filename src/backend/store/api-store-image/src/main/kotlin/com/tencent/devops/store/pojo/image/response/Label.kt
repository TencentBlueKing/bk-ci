package com.tencent.devops.store.pojo.image.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@ApiModel("镜像标签")
data class Label(

    @ApiModelProperty("标签ID", required = true)
    val id: String,

    @ApiModelProperty("标签代码", required = true)
    val labelCode: String,

    @ApiModelProperty("标签名称", required = true)
    val labelName: String,

    @ApiModelProperty("类别 ATOM:插件,TEMPLATE:模板,IMAGE:镜像", required = true)
    val labelType: String,

    @ApiModelProperty("创建时间", required = true)
    val createTime: Long,

    @ApiModelProperty("修改时间", required = true)
    val updateTime: Long

)