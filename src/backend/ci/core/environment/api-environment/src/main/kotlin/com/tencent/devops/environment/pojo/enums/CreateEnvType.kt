package com.tencent.devops.environment.pojo.enums

import com.tencent.devops.environment.pojo.thirdpartyagent.create.AgentPropsSource
import io.swagger.v3.oas.annotations.media.Schema

/** 创作流环境的分类，区别于节点导入来源以及环境用途 [EnvType]。 */
@Schema(title = "创作流环境类型：IEG_IMATE 数字同事 / CLOUD_DESKTOP_NODE 云桌面")
enum class CreateEnvType(private val agentSources: Set<AgentPropsSource>) {
    // 数字同事环境同时接纳个人项目和团队项目的数字同事节点。
    IEG_IMATE(setOf(AgentPropsSource.REMOTEDEV, AgentPropsSource.IEG_IMATE)),
    CLOUD_DESKTOP_NODE(setOf(AgentPropsSource.CLOUD_DESKTOP_NODE));

    /** 环境类型对应的节点来源集合；不能反向推断为某一种具体的数字同事来源。 */
    fun toAgentSources(): Set<AgentPropsSource> = agentSources

    /** 判断节点来源是否属于当前环境类型，未知来源不匹配。 */
    fun matches(source: AgentPropsSource?): Boolean = source in agentSources

    companion object {
        /**
         * JAX-RS 将 @QueryParam 绑定为枚举时优先使用 fromString，否则按枚举名匹配。
         * 把节点来源 REMOTEDEV（个人项目数字同事）作为 IEG_IMATE（数字同事环境）的别名，
         * 使环境列表按 nodeSource 筛选时 REMOTEDEV 与 IEG_IMATE 等价，与节点列表 fetchNodes 口径一致。
         * 仅作用于 HTTP 参数绑定，不改变数据库环境类型解析 parse 与 JSON 序列化语义。
         */
        @JvmStatic
        fun fromString(value: String): CreateEnvType =
            if (value == AgentPropsSource.REMOTEDEV.name) IEG_IMATE else valueOf(value)

        /** 仅解析数据库中的环境类型，不将 REMOTEDEV、DEVCLOUD 等节点来源作为环境类型。 */
        fun parse(name: String?): CreateEnvType? = values().firstOrNull { it.name == name }

        /** 环境是否接纳该来源的节点：未限定类型（存量环境）全部接纳，限定后只接纳对应来源，无法识别的类型不接纳。 */
        fun accepts(envNodeSource: String?, source: AgentPropsSource?): Boolean =
            envNodeSource == null || parse(envNodeSource)?.matches(source) == true
    }
}
