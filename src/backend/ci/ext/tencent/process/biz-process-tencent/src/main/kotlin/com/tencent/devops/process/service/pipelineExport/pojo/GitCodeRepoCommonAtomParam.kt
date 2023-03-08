package com.tencent.devops.process.service.pipelineExport.pojo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.GitPullModeType

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GitCodeRepoCommonAtomParam(
    var repositoryUrl: String? = null,
    var ticketId: String? = null,
    var localPath: String? = null,
    var strategy: CodePullStrategy? = null,
    var enableSubmodule: Boolean? = null,
    var enableVirtualMergeBranch: Boolean? = null,
    var enableSubmoduleRemote: Boolean? = null,
    var enableAutoCrlf: Boolean? = null,
    var pullType: GitPullModeType? = null,
    var refName: String? = null,
    var includePath: String? = null,
    var excludePath: String? = null,
    var fetchDepth: Int? = null,
    var enableGitClean: Boolean? = null,
    var accessToken: String? = null,
    var username: String? = null,
    var password: String? = null,
    var enableGitLfs: Boolean? = null,

    // 非前端传递的参数
    @JsonProperty("pipeline.start.type")
    val pipelineStartType: String? = null,
    val hookEventType: String? = null,
    val hookSourceBranch: String? = null,
    val hookTargetBranch: String? = null,
    val hookSourceUrl: String? = null,
    val hookTargetUrl: String? = null,
    @JsonProperty("git_mr_number")
    val gitMrNumber: String? = null,
    val noScmVariable: Boolean? = null,

    @JsonProperty("pipeline.start.channel")
    var channelCode: String? = ""
) {
    @JsonIgnore
    fun getBranch(): String? {
        return refName
    }
}
