package com.tencent.devops.store.pojo.vo

import io.swagger.annotations.ApiModelProperty

data class MyExtServiceRespItem(
    @ApiModelProperty("扩展服务ID", required = true)
    val serviceId: String,
    @ApiModelProperty("扩展服务名称", required = true)
    val serviceName: String,
    @ApiModelProperty("扩展服务代码", required = true)
    val serviceCode: String,
    @ApiModelProperty("开发语言", required = true)
    val language: String?,
    @ApiModelProperty("扩展服务所属范畴，TRIGGER：触发器类扩展服务 TASK：任务类扩展服务", required = true)
    val category: String,
    @ApiModelProperty("logo链接")
    val logoUrl: String? = null,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty(
        "扩展服务状态，INIT：初始化|COMMITTING：提交中|BUILDING：构建中|BUILD_FAIL：构建失败|TESTING：测试中|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGING：下架中|UNDERCARRIAGED：已下架",
        required = true
    )
    val serviceStatus: String,
    @ApiModelProperty("项目", required = true)
    val projectName: String,
    @ApiModelProperty("是否有处于上架状态的扩展服务扩展服务版本", required = true)
    val releaseFlag: Boolean,
    @ApiModelProperty("创建人", required = true)
    val creator: String,
    @ApiModelProperty("扩展点", required = true)
    val itemName: String,
    @ApiModelProperty("修改人", required = true)
    val modifier: String,
    @ApiModelProperty("创建时间", required = true)
    val createTime: String,
    @ApiModelProperty("创建时间", required = true)
    val updateTime: String
)