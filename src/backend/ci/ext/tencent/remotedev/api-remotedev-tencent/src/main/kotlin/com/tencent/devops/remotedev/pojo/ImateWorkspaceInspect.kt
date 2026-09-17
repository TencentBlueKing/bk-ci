package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "构建态 imate 分身归属检查结果")
data class ImateWorkspaceInspect(
    @get:Schema(title = "分身 workspaceName")
    val workspaceName: String,
    @get:Schema(title = "展示名")
    val displayName: String? = null,
    @get:Schema(title = "分身是否存在")
    val exists: Boolean,
    @get:Schema(title = "分身实际所属者")
    val owner: String? = null,
    @get:Schema(title = "创建人")
    val createUserId: String? = null,
    @get:Schema(title = "分身状态")
    val status: String? = null,
    @get:Schema(title = "是否开启 imate")
    val imateEnabled: Boolean = false,
    @get:Schema(title = "是否属于目标用户")
    val belongsToUser: Boolean,
    @get:Schema(title = "是否可发起会话（属于目标用户且运行中且已开 imate）")
    val usable: Boolean,
    @get:Schema(title = "不可用原因码")
    val reason: String? = null,
    @get:Schema(title = "蓝盾/云桌面项目 ID")
    val projectId: String? = null,
    @get:Schema(title = "归属类型 PERSONAL / PROJECT / PROJECT_PUBLIC")
    val ownerType: String? = null,
    @get:Schema(title = "工作区数字 ID")
    val workspaceId: Long? = null,
    @get:Schema(title = "云桌面环境 ID")
    val envId: String? = null,
    @get:Schema(title = "云桌面 IP")
    val ip: String? = null,
    @get:Schema(title = "数字分身 ID")
    val digitalAvatarId: String? = null,
    @get:Schema(title = "Agent ID")
    val agentId: String? = null,
    @get:Schema(title = "Agent 名称")
    val agentName: String? = null,
    @get:Schema(title = "当前用户是否以被共享人身份可见")
    val shared: Boolean? = null,
    @get:Schema(title = "已绑定的创作流/蓝盾项目 ID")
    val selectedBoundProjectId: String? = null
)
