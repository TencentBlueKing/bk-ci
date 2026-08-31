package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.remotedev.config.BkConfig
import com.tencent.devops.remotedev.pojo.DesktopTokenSign
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.remotedev.service.RemotedevSdkService
import io.mockk.every
import io.mockk.mockk
import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.Test

class ServiceRemoteDevResourceImplTest {

    private val bkConfig = mockk<BkConfig>().also {
        every { it.desktopSdkToken } returns DESKTOP_SDK_TOKEN
    }

    private val workspaceTemplate = RemotedevSdkService(
        kafkaClient = mockk(),
        bkConfig = bkConfig,
        workspaceService = mockk(),
        permissionService = mockk(),
        workspaceAppOauth2MaterialsDao = mockk(),
        dslContext = mockk(),
        client = mockk()
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
            status = WorkspaceStatus.RUNNING.name,
            realOwner = "",
            displayName = "",
            ownerDepartments = null,
            currentLoginUsers = null,
            machineType = "",
            macAddress = ""
        )
        // 用与生产相同的算法动态生成指纹与签名，避免在代码中硬编码真实 token 及其派生值
        val fingerprint = DigestUtils.md5Hex("${ws.macAddress}$DESKTOP_SDK_TOKEN").uppercase()
        val baseSign = DesktopTokenSign(
            fingerprint = fingerprint,
            appId = "appId",
            fileName = "fileName",
            fileVersion = "fileVersion",
            fileUpdateTime = "fileUpdateTime",
            productName = "productName",
            productVersion = "productVersion",
            sha1 = "sha1",
            timestamp = 123,
            publicKey = "publicKey",
            sign = ""
        )
        val unsigned = "${baseSign.fingerprint}," +
            "${baseSign.appId}," +
            "${baseSign.fileName}," +
            "${baseSign.fileVersion}," +
            "${baseSign.fileUpdateTime}," +
            "${baseSign.productName}," +
            "${baseSign.productVersion}," +
            "${baseSign.sha1}," +
            "${baseSign.timestamp}," +
            baseSign.publicKey
        val sign = baseSign.copy(
            sign = ShaUtils.hmacSha1(
                DESKTOP_SDK_TOKEN.toByteArray(),
                unsigned.toByteArray()
            ).uppercase()
        )
        workspaceTemplate.check(ws, sign, ws.innerIp!!)
    }

    companion object {
        // 仅用于测试的任意 token，签名逻辑通过动态计算校验，与真实 token 无关
        private const val DESKTOP_SDK_TOKEN = "123456"
    }
}
