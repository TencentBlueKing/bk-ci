package com.tencent.devops.store.pojo.image.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@ApiModel("镜像版本")
data class ImageVersion(

    @ApiModelProperty("镜像Id", required = true)
    val imageId: String,

    @ApiModelProperty("镜像代码", required = true)
    val imageCode: String,

    @ApiModelProperty("镜像名称", required = true)
    val imageName: String,

    @ApiModelProperty("镜像所属范畴，TRIGGER：触发器类镜像 TASK：任务类镜像", required = true)
    val category: String,

    @ApiModelProperty("版本号", required = true)
    val version: String,

    @ApiModelProperty("镜像状态，INIT：初始化|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGED：已下架", required = true)
    val imageStatus: String,

    @ApiModelProperty("创建人", required = true)
    val creator: String,

    @ApiModelProperty("修改人", required = true)
    val modifier: String,

    @ApiModelProperty("创建时间", required = true)
    val createTime: Long,

    @ApiModelProperty("修改时间", required = true)
    val updateTime: Long

)