package com.tencent.devops.stream.pojo.v2.project

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂部分项目信息附带蓝盾项目信息")
data class GitProjectInfoWithProject(
    @ApiModelProperty("项目ID", name = "id")
    val gitProjectId: Long,
    @ApiModelProperty("项目名称", name = "name")
    val name: String,
    @ApiModelProperty("页面地址", name = "web_url")
    val homepage: String?,
    @ApiModelProperty("HTTP链接", required = true, name = "http_url_to_repo")
    val gitHttpUrl: String,
    @ApiModelProperty("HTTPS链接", name = "https_url_to_repo")
    val gitHttpsUrl: String?,
    @ApiModelProperty("gitSshUrl", name = "ssh_url_to_repo")
    val gitSshUrl: String?,
    @ApiModelProperty("带有所有者的项目名称", name = "name_with_namespace")
    val nameWithNamespace: String,
    @ApiModelProperty("带有所有者的项目路径", name = "path_with_namespace")
    val pathWithNamespace: String?,
    @ApiModelProperty("项目的默认分支", name = "default_branch")
    val defaultBranch: String?,
    @ApiModelProperty("项目的描述信息", name = "description")
    val description: String?,
    @ApiModelProperty("项目的头像信息", name = "avatar_url")
    val avatarUrl: String?,
    @ApiModelProperty("环境路由", name = "avatar_url")
    val routerTag: String?
)
