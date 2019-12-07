package com.tencent.devops.store.pojo.image.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@ApiModel("项目简要信息")
data class ProjectSimpleInfo(

    @ApiModelProperty("项目标识", required = true)
    val projectCode: String,

    @ApiModelProperty("项目名称", required = true)
    val projectName: String,

    @ApiModelProperty("创建人", required = true)
    val creator: String?,

    @ApiModelProperty("创建时间", required = true)
    val createTime: String?

)