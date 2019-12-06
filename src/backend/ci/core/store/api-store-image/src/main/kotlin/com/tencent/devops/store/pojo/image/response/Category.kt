package com.tencent.devops.store.pojo.image.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@ApiModel("镜像范畴")
data class Category(

    @ApiModelProperty("范畴ID", required = true)
    val id: String,

    @ApiModelProperty("范畴代码", required = true)
    val categoryCode: String,

    @ApiModelProperty("范畴名称", required = true)
    val categoryName: String,

    @ApiModelProperty("类别 ATOM:插件,TEMPLATE:模板,IMAGE:镜像", required = true)
    val categoryType: String,

    @ApiModelProperty("范畴图标链接", required = true)
    val iconUrl: String,

    @ApiModelProperty("创建时间", required = true)
    val createTime: Long,

    @ApiModelProperty("修改时间", required = true)
    val updateTime: Long

)