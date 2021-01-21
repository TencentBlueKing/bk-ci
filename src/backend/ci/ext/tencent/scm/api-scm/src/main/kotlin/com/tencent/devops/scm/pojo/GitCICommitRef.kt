package com.tencent.devops.scm.pojo

import io.swagger.annotations.ApiModel

/**
 * {
        "type": "branch",
        "name": "master"
    }
 */

@ApiModel("gitci commit的归属")
data class GitCICommitRef(
    val name: String,
    val type: String
)
