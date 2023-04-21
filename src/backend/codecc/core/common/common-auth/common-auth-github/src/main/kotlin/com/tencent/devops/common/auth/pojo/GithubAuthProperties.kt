package com.tencent.devops.common.auth.pojo

data class GithubAuthProperties(
    var token : String? = null,

    /**
     * 流水线资源类型
     */
    val pipelineResourceType: String? = "pipeline"
)
