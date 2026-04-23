package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "授权健康检查结果")
data class AuthorizationHealthVO(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "检查时间")
    val checkTime: Long,
    @get:Schema(title = "整体健康状态")
    val healthStatus: String,
    @get:Schema(title = "风险数量")
    val riskCount: Int,
    @get:Schema(title = "警告数量")
    val warningCount: Int,
    @get:Schema(title = "授权统计")
    val authorizationStats: AuthorizationStatsVO,
    @get:Schema(title = "风险项列表")
    val risks: List<AuthorizationRiskVO> = emptyList(),
    @get:Schema(title = "建议操作")
    val suggestions: List<String> = emptyList()
)

@Schema(title = "授权统计")
data class AuthorizationStatsVO(
    @get:Schema(title = "流水线授权总数")
    val pipelineAuthorizationCount: Int = 0,
    @get:Schema(title = "代码库授权总数")
    val repertoryAuthorizationCount: Int = 0,
    @get:Schema(title = "环境节点授权总数")
    val envNodeAuthorizationCount: Int = 0,
    @get:Schema(title = "唯一管理员用户组数量")
    val uniqueManagerGroupCount: Int = 0,
    @get:Schema(title = "授权人总数")
    val totalAuthorizerCount: Int = 0
)

@Schema(title = "授权风险项")
data class AuthorizationRiskVO(
    @get:Schema(title = "风险级别")
    val level: String,
    @get:Schema(title = "风险类型")
    val riskType: String,
    @get:Schema(title = "风险描述")
    val description: String,
    @get:Schema(title = "涉及的资源类型")
    val resourceType: String? = null,
    @get:Schema(title = "涉及的资源Code")
    val resourceCode: String? = null,
    @get:Schema(title = "涉及的资源名称")
    val resourceName: String? = null,
    @get:Schema(title = "涉及的用户")
    val affectedUser: String? = null,
    @get:Schema(title = "建议操作")
    val suggestion: String? = null
)
