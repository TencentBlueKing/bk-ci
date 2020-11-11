package com.tencent.devops.common.auth.api.pojo.external

enum class CodeCCAuthAction(val actionName: String,
                            val alias: String) {

    TASK_MANAGE("task_manage", "任务设置"),
    ANALYZE("analyze", "执行分析"),
    DEFECT_MANAGE("defect_manage", "管理告警"),
    DEFECT_VIEW("defect_view", "查看告警"),
    REPORT_VIEW("report_view", "查看报表");
}