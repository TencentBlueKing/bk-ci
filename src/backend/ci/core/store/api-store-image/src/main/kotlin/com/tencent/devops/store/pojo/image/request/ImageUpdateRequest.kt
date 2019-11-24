package com.tencent.devops.store.pojo.image.request

import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import io.swagger.annotations.ApiModelProperty

data class ImageUpdateRequest(
    @ApiModelProperty("镜像名称", required = true)
    val imageName: String,
    @ApiModelProperty("所属分类ID", required = true)
    val classifyId: String,
    @ApiModelProperty("功能标签", required = false)
    val labelList: List<String>,
    @ApiModelProperty("镜像所属范畴CATEGORY_CODE", required = false)
    val category: String?,
    @ApiModelProperty("镜像适用的构建机类型", required = true)
    val agentTypeScope: List<ImageAgentTypeEnum>,
    @ApiModelProperty("版本号", required = true)
    val version: String,
    @ApiModelProperty("镜像来源", required = true)
    val imageSourceType: ImageType,
    @ApiModelProperty("镜像仓库地址", required = true)
    val imageRepoUrl: String,
    @ApiModelProperty("镜像在仓库的名称", required = true)
    val imageRepoName: String,
    @ApiModelProperty("镜像在仓库的路径", required = true)
    val imageRepoPath: String,
    @ApiModelProperty("凭证ID", required = true)
    val ticketId: String,
    @ApiModelProperty("镜像大小", required = false)
    val imageSize: String,
    @ApiModelProperty("镜像TAG", required = true)
    val imageTag: String,
    @ApiModelProperty("LOGO url", required = true)
    val logoUrl: String,
    @ApiModelProperty("镜像图标（BASE64字符串）", required = false)
    val icon: String,
    @ApiModelProperty("镜像简介）", required = false)
    val summary: String,
    @ApiModelProperty("镜像描述", required = false)
    val description: String,
    @ApiModelProperty("发布者", required = true)
    val publisher: String,
    @ApiModelProperty("是否公开 true：公开，false：不公开", required = false)
    val publicFlag: Boolean? = null,
    @ApiModelProperty("是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @ApiModelProperty("是否官方认证 true：是，false：否", required = false)
    val certificationFlag: Boolean? = null,
    @ApiModelProperty("研发来源 SELF_DEVELOPED：自研 THIRD_PARTY：第三方", required = false)
    val rdType: ImageRDTypeEnum?,
    @ApiModelProperty("权重（数值越大代表权重越高）")
    val weight: Int? = null
)