package com.tencent.devops.common.archive.pojo

import io.swagger.annotations.ApiModelProperty

data class BkRepoInfo(
    @ApiModelProperty("所属项目id")
    val projectId: String,
    @ApiModelProperty("所属仓库名称")
    val repoName: String,
    @ApiModelProperty("包名称")
    val name: String,
    @ApiModelProperty("包唯一key")
    val key: String,
    @ApiModelProperty("包类别")
    val type: String,
    @ApiModelProperty("最新上传版本")
    val latest: String,
    @ApiModelProperty("下载次数")
    val downloads: Long,
    @ApiModelProperty("版本数量")
    val versions: Long,
    @ApiModelProperty("简要描述")
    val description: String?,
    @ApiModelProperty("创建者")
    val createdBy: String,
    @ApiModelProperty("创建时间")
    val createdDate: String,
    @ApiModelProperty("修改者")
    val lastModifiedBy: String,
    @ApiModelProperty("修改时间")
    val lastModifiedDate: String,
    @ApiModelProperty("历史版本")
    val historyVersion: List<String>

)
