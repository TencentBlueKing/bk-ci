package com.tencent.devops.remotedev.pojo

enum class OpHistoryCopyWriting(
    val default: String
) {
    CREATE_WINDOWS("opHistoryCopyWriting.createWindows"), // 创建了一个云桌面环境
    FIRST_START("opHistoryCopyWriting.firstStart"), // 激活了本环境
    NOT_FIRST_START("opHistoryCopyWriting.notFirstStart"), // 重新激活了本环境
    SAFE_INITIALIZATION("opHistoryCopyWriting.safeInitialization"), // 此环境正在安全初始化
    MANUAL_STOP("opHistoryCopyWriting.manualStop"), // 主动关闭了工作空间
    DELETE("opHistoryCopyWriting.delete"), // 删除了本环境
    SHARE("opHistoryCopyWriting.share"), // 给%s共享了此环境
    ACTION_CHANGE("opHistoryCopyWriting.actionChange"), // 状态变更: %s -> %s
    ASSIGN_OWNER("opHistoryCopyWriting.assignOwner"), // 分配拥有者：将拥有者设置为 %s
    CHANGE_OWNER("opHistoryCopyWriting.changeOwner"), // 修改拥有者：将拥有者从 %s 修改为 %s
    MODIFY_DISPLAY_NAME("opHistoryCopyWriting.modifyDisplayName"), // 修改显示名称：将显示名称从 "%s" 修改为 "%s"
    MODIFY_REMARK("opHistoryCopyWriting.modifyRemark"), // 修改备注：修改了备注信息
    MODIFY_LABELS("opHistoryCopyWriting.modifyLabels"), // 修改标签：修改了标签信息
    ASSIGN_VIEWER("opHistoryCopyWriting.assignViewer"), // 分配查看者：添加了查看者 %s
    UNSHARE("opHistoryCopyWriting.unshare"), // 取消共享：取消了与 %s 的共享
    AGREE_RECORD("opHistoryCopyWriting.agreeRecord") // 同意开启云录制
}
