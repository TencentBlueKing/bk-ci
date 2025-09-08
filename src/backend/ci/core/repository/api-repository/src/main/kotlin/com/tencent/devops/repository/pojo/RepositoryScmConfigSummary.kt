package com.tencent.devops.repository.pojo

import com.tencent.devops.common.api.enums.ScmType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 代码库配置摘要信息
 */
data class RepositoryScmConfigSummary(
    @get:Schema(title = "代码库标识", required = true)
    val scmCode: String,
    @get:Schema(title = "代码库名称", required = true)
    val name: String,
    @get:Schema(title = "源代码提供者,如github,gitlab", required = true)
    val providerCode: String,
    @get:Schema(title = "代码库类型,如git,svn", required = true)
    val scmType: ScmType
)
