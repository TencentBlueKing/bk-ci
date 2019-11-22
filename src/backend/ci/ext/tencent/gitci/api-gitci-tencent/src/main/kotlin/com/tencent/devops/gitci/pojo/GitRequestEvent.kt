package com.tencent.devops.gitci.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

// 工蜂所有推过来的请求
@ApiModel("工蜂触发请求")
data class GitRequestEvent(
    @ApiModelProperty("ID")
    var id: Long?,
    @ApiModelProperty("OBJECT_KIND")
    val objectKind: String,
    @ApiModelProperty("OPERATION_KIND")
    val operationKind: String?,
    @ApiModelProperty("EXTENSION_ACTION")
    val extensionAction: String?,
    @ApiModelProperty("GIT_PROJECT_ID")
    val gitProjectId: Long,
    @ApiModelProperty("BRANCH")
    val branch: String,
    @ApiModelProperty("TARGET_BRANCH")
    val targetBranch: String?,
    @ApiModelProperty("COMMIT_ID")
    val commitId: String,
    @ApiModelProperty("COMMIT_MSG")
    val commitMsg: String?,
    @ApiModelProperty("COMMIT_TIMESTAMP")
    val commitTimeStamp: String?,
    @ApiModelProperty("用户")
    val userId: String,
    @ApiModelProperty("TOTAL_COMMIT_COUNT")
    val totalCommitCount: Long,
    @ApiModelProperty("MERGE_REQUEST_ID")
    val mergeRequestId: Long?,
    @ApiModelProperty("EVENT")
    val event: String,
    @ApiModelProperty("DESCRIPTION")
    var description: String?
)
