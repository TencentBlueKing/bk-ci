package com.tencent.devops.store.pojo.image.response

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * @Description
 * @Date 2019/9/17
 * @Version 1.0
 */
@ApiModel("我的镜像")
data class MyImage(

    @ApiModelProperty("镜像Id", required = true)
    val imageId: String,

    @ApiModelProperty("镜像代码", required = true)
    val imageCode: String,

    @ApiModelProperty("镜像名称", required = true)
    val imageName: String,

    @ApiModelProperty("镜像来源 BKDEVOPS:蓝盾，THIRD:第三方", required = true)
    val imageSourceType: String,

    @ApiModelProperty("镜像仓库URL", required = false)
    val imageRepoUrl: String,

    @ApiModelProperty("镜像在仓库中的名称", required = false)
    val imageRepoName: String,

    @ApiModelProperty("版本号", required = true)
    val version: String,

    @ApiModelProperty("镜像tag", required = true)
    val imageTag: String,

    @ApiModelProperty("镜像大小（MB字符串）", required = true)
    val imageSize: String,

    @ApiModelProperty("镜像大小数值（字节）", required = true)
    val imageSizeNum: Long,

    @ApiModelProperty(
        "镜像状态，INIT：初始化|COMMITTING：提交中|CHECKING：验证中|CHECK_FAIL：验证失败|TESTING：测试中|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGING：下架中|UNDERCARRIAGED：已下架",
        required = true
    )
    val imageStatus: String,

    @ApiModelProperty("所属项目代码", required = true)
    val projectCode: String,

    @ApiModelProperty("所属项目名称", required = true)
    val projectName: String,

    @ApiModelProperty("项目是否被禁用", required = true)
    val projectEnabled: Boolean,

    @ApiModelProperty("创建人", required = true)
    val creator: String,

    @ApiModelProperty("修改人", required = true)
    val modifier: String,

    @ApiModelProperty("创建时间", required = true)
    val createTime: Long,

    @ApiModelProperty("修改时间", required = true)
    val updateTime: Long,

    @ApiModelProperty("是否为公共镜像", required = true)
    val publicFlag: Boolean
)