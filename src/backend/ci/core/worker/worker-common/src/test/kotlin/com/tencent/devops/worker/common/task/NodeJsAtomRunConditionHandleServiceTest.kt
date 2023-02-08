package com.tencent.devops.worker.common.task

import com.tencent.devops.common.api.constant.NODEJS
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.store.pojo.common.StorePkgRunEnvInfo
import com.tencent.devops.store.pojo.common.enums.LanguageEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.worker.common.service.impl.NodeJsAtomRunConditionHandleServiceImpl
import com.tencent.devops.worker.common.task.market.AtomRunConditionFactory
import io.mockk.spyk
import org.apache.commons.lang3.reflect.MethodUtils
import org.junit.jupiter.api.Test
import java.io.File
import java.lang.reflect.InvocationTargetException

class NodeJsAtomRunConditionHandleServiceTest {
    private val self = spyk(
        AtomRunConditionFactory.createAtomRunConditionHandleService(
            language = NODEJS
        ) as NodeJsAtomRunConditionHandleServiceImpl,
        recordPrivateCalls = true
    )

    @Test
    fun test2() {
        val os = System.getProperty("os.name")
        val osName = when {
            os.startsWith("Windows", true) -> {
                OSType.WINDOWS
            }
            os.startsWith("Linux", true) -> {
                OSType.LINUX
            }
            else -> {
                OSType.MAC_OS
            }
        }
        val pkgFileDir = File(getAtomBasePath() + "/src/test/file")
        val storePkgRunEnvInfo1 = StorePkgRunEnvInfo(
            id = "",
            storeType = StoreTypeEnum.ATOM.name,
            language = LanguageEnum.NODEJS.name,
            osName = osName.name,
            osArch = "",
            runtimeVersion = "",
            pkgName = "node-v16.18.1-win-x64.zip",
            pkgDownloadPath = "https://npmmirror.com/mirrors/node/v16.18.1/node-v16.18.1-win-x64.zip",
            defaultFlag = true,
            creator = "",
            modifier = "",
            createTime = "",
            updateTime = ""
        )
        val storePkgRunEnvInfo2 = StorePkgRunEnvInfo(
            id = "",
            storeType = StoreTypeEnum.ATOM.name,
            language = LanguageEnum.NODEJS.name,
            osName = osName.name,
            osArch = "",
            runtimeVersion = "",
            pkgName = "node-v16.18.1-win-x86.zip",
            pkgDownloadPath = "https://share.weiyun.com/9lduBwRR",
            defaultFlag = true,
            creator = "",
            modifier = "",
            createTime = "",
            updateTime = ""
        )
        try {
            self.invokePrivate<Unit>("performPkgProcessing", storePkgRunEnvInfo1, pkgFileDir, osName)
            println("success")
        } catch (ignored: Throwable) {
            println("storePkgRunEnvInfo1 Fail")
        }
        try {
            self.invokePrivate<Unit>("performPkgProcessing", storePkgRunEnvInfo2, pkgFileDir, osName)
        } catch (ignored: Throwable) {
            println("storePkgRunEnvInfo2 Fail")
        }
    }

    private fun getAtomBasePath(): String {
        return System.getProperty("java.io.tmpdir")
    }

    /**
     * 调用私有函数
     */
    private inline fun <reified R> Any.invokePrivate(methodName: String, vararg args: Any): R? {
        try {
            val invokeResult = MethodUtils.invokeMethod(this, true, methodName, *args) ?: return null
            if (invokeResult is R) {
                return invokeResult
            } else {
                throw IllegalArgumentException("Result type is illegal")
            }
        } catch (e: Throwable) {
            throw if (e is InvocationTargetException) e.targetException else e
        }
    }
}