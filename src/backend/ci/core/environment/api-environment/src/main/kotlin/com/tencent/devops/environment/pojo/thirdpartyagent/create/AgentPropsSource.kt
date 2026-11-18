package com.tencent.devops.environment.pojo.thirdpartyagent.create

import com.tencent.devops.environment.pojo.enums.CreateEnvType

/** 节点的实际导入来源，环境类型使用 [CreateEnvType]，不直接复用此枚举。 */
enum class AgentPropsSource {
    REMOTEDEV, // 个人项目数字同事，与 IEG_IMATE 走同一套数字同事鉴权
    DEVCLOUD, // TEG iMate Linux
    IEG_IMATE, // 团队项目数字同事，目前只在数字同事共享到团队项目时产生
    CLOUD_DESKTOP_NODE // 创作流云桌面节点，权限与普通云桌面一致
    ;

    /** 节点来源转换为所属的环境类型；DEVCLOUD 不属于两类环境，返回 null。 */
    fun toEnvType(): CreateEnvType? = CreateEnvType.values().firstOrNull { it.matches(this) }

    /** 节点列表沿用数字同事合并筛选，其余来源精确匹配；不用于解析环境字段。 */
    fun matchesSource(target: AgentPropsSource?): Boolean = toEnvType()?.matches(target) ?: (this == target)

    companion object {
        /** 解析数据库中保存的节点来源名称，无法识别时返回 null。 */
        fun parse(name: String?): AgentPropsSource? = values().firstOrNull { it.name == name }
    }
}
