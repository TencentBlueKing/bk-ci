package com.tencent.devops.store.pojo.vo

import com.tencent.devops.store.pojo.StoreServiceItem
import io.swagger.annotations.ApiModelProperty

data class ServiceVersionVO(
    @ApiModelProperty("扩展服务ID")
    val serviceId: String,
    @ApiModelProperty("扩展服务标识")
    val serviceCode: String,
    @ApiModelProperty("扩展服务名称")
    val serviceName: String,
    @ApiModelProperty("logo地址")
    val logoUrl: String?,
//    @ApiModelProperty("扩展服务分类code")
//    val classifyCode: String?,
//    @ApiModelProperty("扩展服务分类名称")
//    val classifyName: String?,
//    @ApiModelProperty("扩展服务范畴")
//    val category: String?,
//    @ApiModelProperty("扩展服务说明文档链接")
//    val docsLink: String?,
//    @ApiModelProperty("扩展服务类型")
//    val serviceType: String?,
    @ApiModelProperty("扩展服务简介")
    val summary: String?,
    @ApiModelProperty("扩展服务描述")
    val description: String?,
    @ApiModelProperty("版本号")
    val version: String?,
    @ApiModelProperty("扩展服务状态，INIT：初始化|COMMITTING：提交中|BUILDING：构建中|BUILD_FAIL：构建失败|TESTING：测试中|AUDITING：审核中|AUDIT_REJECT：审核驳回|RELEASED：已发布|GROUNDING_SUSPENSION：上架中止|UNDERCARRIAGING：下架中|UNDERCARRIAGED：已下架", required = true)
    val serviceStatus: String,
//    @ApiModelProperty("版本日志")
//    val versionContent: String?,
    @ApiModelProperty("开发语言")
    val language: String?,
    @ApiModelProperty("代码库链接")
    val codeSrc: String?,
    @ApiModelProperty("发布者")
    val publisher: String?,
    @ApiModelProperty("创建人")
    val creator: String,
    @ApiModelProperty("修改人")
    val modifier: String,
    @ApiModelProperty("创建时间")
    val createTime: String,
    @ApiModelProperty("修改时间")
    val updateTime: String,
    @ApiModelProperty("是否为默认扩展服务（默认扩展服务默认所有项目可见）true：默认扩展服务 false：普通扩展服务")
    val defaultFlag: Boolean?,
    @ApiModelProperty("是否可安装标识")
    val flag: Boolean?,
    @ApiModelProperty("扩展服务代码库授权者")
    val repositoryAuthorizer: String?,
    @ApiModelProperty("扩展服务的调试项目")
    val projectCode: String?,
//    @ApiModelProperty("标签列表", required = false)
//    val labelList: List<Label>?,
//    @ApiModelProperty("用户评论信息")
//    val userCommentInfo: StoreUserCommentInfo,
    @ApiModelProperty("项目可视范围,PRIVATE:私有 LOGIN_PUBLIC:登录用户开源")
    val visibilityLevel: String?,
//    @ApiModelProperty("扩展服务代码库不开源原因")
//    val privateReason: String?,
    @ApiModelProperty("是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null,
    @ApiModelProperty("扩展点列表")
    val itemListStore: List<StoreServiceItem>
)