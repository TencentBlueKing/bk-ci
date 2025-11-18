package com.tencent.devops.process.pojo.pipeline.enums

/**
 * yaml文件操作类型
 */
enum class YamlFileActionType {
    // 开启pac时同步
    SYNC,

    CREATE,

    // 更新
    UPDATE,

    // 删除
    DELETE,

    // 重命名
    RENAME,

    // 触发
    TRIGGER,

    DEPENDENCY_UPGRADE,

    DEPENDENCY_UPGRADE_AND_TRIGGER,

    // 关闭操作
    CLOSE,

    // 更新并触发
    UPDATE_AND_TRIGGER,

    // 已合并
    MERGED,

    // 校验有问题，改流水线不触发
    NO_TRIGGER;

    fun isChange(): Boolean {
        return this == CREATE || this == UPDATE || this == DELETE || this == RENAME
    }
}
