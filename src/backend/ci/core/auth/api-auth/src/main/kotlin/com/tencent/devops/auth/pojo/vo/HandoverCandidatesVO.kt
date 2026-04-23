package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "交接人推荐结果")
data class HandoverCandidatesVO(
    @get:Schema(title = "推荐的交接人列表")
    val candidates: List<HandoverCandidateVO> = emptyList(),
    @get:Schema(title = "需要交接的授权统计")
    val authorizationsToHandover: AuthorizationsToHandoverVO,
    @get:Schema(title = "推荐建议")
    val suggestion: String? = null
)

@Schema(title = "交接人候选")
data class HandoverCandidateVO(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "显示名称")
    val displayName: String,
    @get:Schema(title = "是否是管理员")
    val isManager: Boolean = false,
    @get:Schema(title = "是否可接收全部授权（仅当无需验证的授权时为true）")
    val canReceiveAll: Boolean = false,
    @get:Schema(title = "可直接接收的授权类型（如uniqueManagerGroup）")
    val canReceive: List<String> = emptyList(),
    @get:Schema(title = "无法接收的授权类型（已确认无法接收）")
    val cannotReceive: List<String> = emptyList(),
    @get:Schema(title = "无法接收的原因")
    val cannotReceiveReasons: Map<String, String> = emptyMap(),
    @get:Schema(title = "需要验证才能确定能否接收的授权类型")
    val requiresValidation: List<String> = emptyList(),
    @get:Schema(title = "验证条件提示")
    val validationHints: Map<String, String> = emptyMap(),
    @get:Schema(title = "推荐级别: highly_recommended/recommended/partial")
    val recommendLevel: String,
    @get:Schema(title = "标签")
    val tags: List<String> = emptyList()
)

@Schema(title = "需要交接的授权统计")
data class AuthorizationsToHandoverVO(
    @get:Schema(title = "流水线授权数量")
    val pipeline: Int = 0,
    @get:Schema(title = "代码库授权数量")
    val repertory: Int = 0,
    @get:Schema(title = "环境节点授权数量")
    val envNode: Int = 0,
    @get:Schema(title = "唯一管理员组数量")
    val uniqueManagerGroups: Int = 0
)
