package com.tencent.devops.common.webhook.service.code.filter

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PushKindFilterTest {

    private val response = WebhookFilterResponse()

    @Test
    fun includeBranches() {
        var pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "create",
            actionKind = "createbranch",
            isMonitorCreate = false,
            isMonitorUpdate = false
        )
        // 两个都不见监听 false
        // 远程仓库新增分支
        Assertions.assertFalse(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "create",
            actionKind = "createbranch",
            isMonitorCreate = true,
            isMonitorUpdate = false
        )
        // 只监听创建分支
        // 远程仓库新增分支
        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "create",
            actionKind = "createbranch",
            isMonitorCreate = false,
            isMonitorUpdate = true
        )
        // 只监听新增文件
        // 远程仓库新增分支
        Assertions.assertFalse(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "create",
            actionKind = "createbranch",
            isMonitorCreate = true,
            isMonitorUpdate = true
        )
        // 两个都监听
        // 远程仓库新增分支
        Assertions.assertTrue(pushKindFilter.doFilter(response))

        // 本地提交
        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "update",
            actionKind = "clientpush",
            isMonitorCreate = false,
            isMonitorUpdate = false
        )
        // 两个都不见监听 false
        Assertions.assertFalse(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "update",
            actionKind = "clientpush",
            isMonitorCreate = true,
            isMonitorUpdate = false
        )
        // 只监听创建分支
        Assertions.assertFalse(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "update",
            actionKind = "clientpush",
            isMonitorCreate = false,
            isMonitorUpdate = true
        )
        // 只监听新增文件
        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "update",
            actionKind = "clientpush",
            isMonitorCreate = true,
            isMonitorUpdate = true
        )
        // 两个都监听
        Assertions.assertTrue(pushKindFilter.doFilter(response))

        // 本地新建分支 Filter结果应与本地提交相同
        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "create",
            actionKind = "clientpush",
            isMonitorCreate = false,
            isMonitorUpdate = false
        )
        // 两个都不见监听 false
        Assertions.assertFalse(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "create",
            actionKind = "clientpush",
            isMonitorCreate = true,
            isMonitorUpdate = false
        )
        // 只监听创建分支
        Assertions.assertFalse(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "create",
            actionKind = "clientpush",
            isMonitorCreate = false,
            isMonitorUpdate = true
        )
        // 只监听新增文件
        Assertions.assertTrue(pushKindFilter.doFilter(response))

        pushKindFilter = PushKindFilter(
            pipelineId = "p-8a49b34bfd834adda6e8dbaad01eedea",
            operationKind = "create",
            actionKind = "clientpush",
            isMonitorCreate = true,
            isMonitorUpdate = true
        )
        // 两个都监听
        Assertions.assertTrue(pushKindFilter.doFilter(response))
    }

}
