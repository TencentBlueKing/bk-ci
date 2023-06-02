package com.tencent.devops.worker.common.service.impl

import com.tencent.devops.common.api.constant.NODEJS
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.store.pojo.common.StorePkgRunEnvInfo
import com.tencent.devops.store.pojo.common.enums.LanguageEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.task.market.AtomRunConditionFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Files

internal class NodeJsAtomRunConditionHandleServiceImplTest {

    private val atomApi: AtomArchiveSDKApi = mockk()

    private val self = spyk(
        AtomRunConditionFactory.createAtomRunConditionHandleService(
            language = NODEJS
        ) as NodeJsAtomRunConditionHandleServiceImpl,
        recordPrivateCalls = true
    )

    private val windowsNormalPkgRunEnvInfo = StorePkgRunEnvInfo(
        id = "1",
        storeType = StoreTypeEnum.ATOM.name,
        language = LanguageEnum.NODEJS.name,
        osName = OSType.WINDOWS.name,
        osArch = "",
        runtimeVersion = "16.*",
        pkgName = "node-v16.18.1-win-x64.zip",
        pkgDownloadPath = "https://npmmirror.com/mirrors/node/v16.18.1/node-v16.18.1-win-x64.zip",
        defaultFlag = true,
        creator = "admin",
        modifier = "admin",
        createTime = "",
        updateTime = ""
    )

    private val windowsErrorPkgRunEnvInfo = StorePkgRunEnvInfo(
        id = "1",
        storeType = StoreTypeEnum.ATOM.name,
        language = LanguageEnum.NODEJS.name,
        osName = OSType.WINDOWS.name,
        osArch = "",
        runtimeVersion = "16.*",
        pkgName = "node-v16.18.1-win-x64.zip",
        pkgDownloadPath = "https://npmmirror.com/mirrors/node/v16.18.1/node123-v16.18.1-win-x64.zip",
        defaultFlag = true,
        creator = "admin",
        modifier = "admin",
        createTime = "",
        updateTime = ""
    )

    private val linuxNormalPkgRunEnvInfo = StorePkgRunEnvInfo(
        id = "2",
        storeType = StoreTypeEnum.ATOM.name,
        language = LanguageEnum.NODEJS.name,
        osName = OSType.LINUX.name,
        osArch = "",
        runtimeVersion = "16.*",
        pkgName = "node-v16.18.1-linux-x64.tar.gz",
        pkgDownloadPath = "https://registry.npmmirror.com/-/binary/node/latest-v16.x/node-v16.18.1-linux-x64.tar.gz",
        defaultFlag = true,
        creator = "admin",
        modifier = "admin",
        createTime = "",
        updateTime = ""
    )

    private val linuxErrorPkgRunEnvInfo = StorePkgRunEnvInfo(
        id = "2",
        storeType = StoreTypeEnum.ATOM.name,
        language = LanguageEnum.NODEJS.name,
        osName = OSType.LINUX.name,
        osArch = "",
        runtimeVersion = "16.*",
        pkgName = "node-v16.18.1-linux-x64.tar.gz",
        pkgDownloadPath = "https://registry.npmmirror.com/-/binary/node/latest-v16.x/node123-v16.18.1-linux-x64.tar.gz",
        defaultFlag = true,
        creator = "admin",
        modifier = "admin",
        createTime = "",
        updateTime = ""
    )

    private val macNormalPkgRunEnvInfo = StorePkgRunEnvInfo(
        id = "3",
        storeType = StoreTypeEnum.ATOM.name,
        language = LanguageEnum.NODEJS.name,
        osName = OSType.MAC_OS.name,
        osArch = "",
        runtimeVersion = "16.*",
        pkgName = "node-v16.18.1-darwin-x64.tar.gz",
        pkgDownloadPath = "https://registry.npmmirror.com/-/binary/node/latest-v16.x/node-v16.18.1-darwin-x64.tar.gz",
        defaultFlag = true,
        creator = "admin",
        modifier = "admin",
        createTime = "",
        updateTime = ""
    )

    private val macErrorPkgRunEnvInfo = StorePkgRunEnvInfo(
        id = "3",
        storeType = StoreTypeEnum.ATOM.name,
        language = LanguageEnum.NODEJS.name,
        osName = OSType.MAC_OS.name,
        osArch = "",
        runtimeVersion = "16.*",
        pkgName = "node-v16.18.1-darwin-x64.tar.gz",
        pkgDownloadPath = "https://registry.npmmirror.com/-/binary/node/latest-v16.x/node1-v16.18.1-darwin-x64.tar.gz",
        defaultFlag = true,
        creator = "admin",
        modifier = "admin",
        createTime = "",
        updateTime = ""
    )

    @Nested
    inner class PrepareRunEnv {
        private val osType = AgentEnv.getOS()
        private val normalPkgRunEnvInfo = when (osType) {
            OSType.WINDOWS -> windowsNormalPkgRunEnvInfo
            OSType.LINUX -> linuxNormalPkgRunEnvInfo
            else -> macNormalPkgRunEnvInfo
        }
        private val errorPkgRunEnvInfo = when (osType) {
            OSType.WINDOWS -> windowsErrorPkgRunEnvInfo
            OSType.LINUX -> linuxErrorPkgRunEnvInfo
            else -> macErrorPkgRunEnvInfo
        }

        @Test
        @DisplayName("node环境包正常的情况")
        fun test_1() {
            mockkObject(ApiFactory)
            every {
                ApiFactory.create(AtomArchiveSDKApi::class)
            } returns atomApi
            every {
                atomApi.getStorePkgRunEnvInfo(
                    language = any(),
                    osName = osType.name,
                    osArch = any(),
                    runtimeVersion = any()
                )
            } returns Result(normalPkgRunEnvInfo)
            val workspace = Files.createTempDirectory(UUIDUtil.generate()).toFile()
            Assertions.assertTrue(
                self.prepareRunEnv(
                    osType = osType,
                    language = "nodejs",
                    runtimeVersion = "16.*",
                    workspace = workspace
                )
            )
        }

        @Test
        @DisplayName("node环境包不正常的情况")
        fun test_2() {
            mockkObject(ApiFactory)
            every {
                ApiFactory.create(AtomArchiveSDKApi::class)
            } returns atomApi
            every {
                atomApi.getStorePkgRunEnvInfo(
                    language = any(),
                    osName = osType.name,
                    osArch = any(),
                    runtimeVersion = any()
                )
            } returns Result(errorPkgRunEnvInfo)
            val workspace = Files.createTempDirectory(UUIDUtil.generate()).toFile()
            try {
                self.prepareRunEnv(
                    osType = osType,
                    language = "nodejs",
                    runtimeVersion = "16.*",
                    workspace = workspace
                )
            } catch (e: Throwable) {
                Assertions.assertThrows(TaskExecuteException::class.java) { throw e }
                Assertions.assertEquals(
                    (e as TaskExecuteException).errorCode,
                    ErrorCode.SYSTEM_WORKER_LOADING_ERROR
                )
            }
        }
    }
}
