package com.tencent.devops.scm.pojo.request

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.repository.pojo.enums.CodeSvnRegion
import io.swagger.annotations.ApiParam

data class CommitCheckRequest(
    @ApiParam("项目名称", required = true)
    val projectName: String,
    @ApiParam("仓库地址", required = true)
    val url: String,
    @ApiParam("仓库类型", required = true)
    val type: ScmType,
    @ApiParam("privateKey", required = true)
    val privateKey: String?,
    @ApiParam("passPhrase", required = false)
    val passPhrase: String?,
    @ApiParam("token", required = true)
    val token: String?,
    @ApiParam("仓库区域前缀（只有svn用到）", required = false)
    val region: CodeSvnRegion?,
    @ApiParam("CommitId", required = false)
    val commitId: String,
    @ApiParam("详情链接", required = true)
    val state: String,
    @ApiParam("详情链接", required = true)
    val targetUrl: String,
    @ApiParam("区分标志", required = true)
    val context: String,
    @ApiParam("详情链接", required = true)
    val description: String,
    @ApiParam("详情链接", required = true)
    val block: Boolean,
    @ApiParam("mr对应的requestId", required = true)
    val mrRequestId: Long?,
    @ApiParam("报表数据", required = true)
    val reportData: Pair<List<String>, MutableMap<String, MutableList<List<String>>>>
)