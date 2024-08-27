package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.pojo.DesktopTokenSign
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ServiceRemoteDevResourceImplTest {

    private val bkConfig = BkConfig()
    private val workspaceTemplate = ServiceRemoteDevResourceImpl(
        permissionService = mockk(),
        workspaceService = mockk(),
        desktopWorkspaceService = mockk(),
        createControl = mockk(),
        deleteControl = mockk(),
        workspaceCommon = mockk(),
        windowsResourceConfigService = mockk(),
        notifyControl = mockk(),
        client = mockk(),
        redisOperation = mockk(),
        workspaceLoginService = mockk(),
        startWorkspaceService = mockk(),
        rabbitTemplate = mockk(),
        expertSupportService = mockk(),
        devcloudService = mockk(),
        deliverControl = mockk(),
        imageManageService = mockk(),
        whiteListService = mockk(),
        rebuildWorkspaceHandler = mockk(),
        startWorkspaceHandler = mockk(),
        stopWorkspaceHandler = mockk(),
        restartWorkspaceHandler = mockk(),
        makeWorkspaceImageHandler = mockk(),
        bkConfig = bkConfig,
        tGitService = mockk()
    )

    @Test
    fun check() {
        val ws = WeSecProjectWorkspace(
            workspaceName = "workspace name",
            projectId = "",
            creator = "",
            owner = "",
            createTime = "",
            regionId = "",
            innerIp = "1.2.3",
            status = WorkspaceStatus.RUNNING,
            realOwner = "",
            displayName = "",
            ownerDepartments = null,
            currentLoginUsers = null,
            machineType = "",
            macAddress = ""
        )
        val sign = DesktopTokenSign(
            fingerprint = "3E919A2223D81E2DD558F29692461351",
            appId = "appId",
            fileName = "fileName",
            fileVersion = "fileVersion",
            fileUpdateTime = "fileUpdateTime",
            productName = "productName",
            productVersion = "productVersion",
            sha1 = "sha1",
            timestamp = 123,
            publicKey = "publicKey",
            sign = "EF988B15567B548C72F290BE01418840DECE8514"
        )
        workspaceTemplate.check(ws, sign, ws.innerIp!!)
    }
}
