package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "成员退出/移出项目检查结果")
data class MemberExitCheckVO(
    @get:Schema(title = "是否可以直接退出（无需交接）")
    val canExitDirectly: Boolean = false,
    @get:Schema(title = "是否需要交接")
    val needHandover: Boolean = false,
    @get:Schema(title = "是否通过组织加入项目（退出后可能仍保留组织带来的权限）")
    val hasDepartmentJoined: Boolean = false,
    @get:Schema(title = "通过组织加入的组织名称")
    val departments: String? = null,
    @get:Schema(title = "需要交接的授权统计")
    val authorizationsToHandover: AuthorizationsToHandoverVO? = null,
    @get:Schema(title = "指定的交接人")
    val specifiedHandoverTo: String? = null,
    @get:Schema(title = "指定交接人是否可以接收全部授权")
    val handoverToCanReceiveAll: Boolean = false,
    @get:Schema(title = "指定交接人可接收的授权类型")
    val handoverToCanReceive: List<String> = emptyList(),
    @get:Schema(title = "指定交接人无法接收的授权类型")
    val handoverToCannotReceive: List<String> = emptyList(),
    @get:Schema(title = "指定交接人无法接收的原因")
    val handoverToCannotReceiveReasons: Map<String, String> = emptyMap(),
    @get:Schema(title = "推荐的交接人列表（当指定交接人无法接收全部授权时提供）")
    val recommendedCandidates: List<HandoverCandidateVO> = emptyList(),
    @get:Schema(title = "建议信息")
    val suggestion: String? = null
)
