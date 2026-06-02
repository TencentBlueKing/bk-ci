/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.service.impl

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.worker.common.BK_CI_ATOM_EXECUTE_ENV_PATH
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.AtomRunConditionHandleService
import com.tencent.devops.worker.common.utils.WorkspaceUtils
import java.io.File
import org.slf4j.LoggerFactory

class GolangAtomRunConditionHandleServiceImpl : AtomRunConditionHandleService {

    companion object {
        private const val RETRY_NUM = 3
        private const val GO_DIR_PREFIX = ".go"
        private const val GO_BIN_NAME = "go"
        private const val GO_EXE_NAME = "go.exe"
        private val logger = LoggerFactory.getLogger(GolangAtomRunConditionHandleServiceImpl::class.java)
    }

    override fun prepareRunEnv(
        osType: OSType,
        language: String,
        runtimeVersion: String,
        workspace: File
    ): String? {
        if (runtimeVersion.isBlank()) return null

        // 获取安装包运行时环境信息
        val atomApi = ApiFactory.create(AtomArchiveSDKApi::class)
        val storePkgRunEnvInfoResult = atomApi.getStorePkgRunEnvInfo(
            language = language,
            osName = osType.name,
            osArch = System.getProperty("os.arch"),
            runtimeVersion = runtimeVersion
        )
        if (storePkgRunEnvInfoResult.isNotOk()) {
            LoggerService.addErrorLine("get plugin pkgRunEnvInfo fail: ${storePkgRunEnvInfoResult.message}")
            throw TaskExecuteException(
                errorMsg = "get plugin pkgRunEnvInfo fail",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )
        }
        val storePkgRunEnvInfo = storePkgRunEnvInfoResult.data
        val envDir = WorkspaceUtils.getCommonEnvDir() ?: workspace
        if (storePkgRunEnvInfo != null) {
            LoggerService.addNormalLine(
                "Go plugin: requested version=$runtimeVersion, " +
                    "pkgName=${storePkgRunEnvInfo.pkgName}, " +
                    "pkgDownloadPath=${storePkgRunEnvInfo.pkgDownloadPath}"
            )
        }

        storePkgRunEnvInfo?.let {
            val goVersionDir = File(envDir, "$GO_DIR_PREFIX/go-$runtimeVersion")
            val goBinPath = File(goVersionDir, "bin").absolutePath
            val goExecutePath = "$goBinPath${File.separator}"
            System.setProperty(BK_CI_ATOM_EXECUTE_ENV_PATH, goExecutePath)

            // 检查 Go 二进制文件是否存在并验证版本
            val goBinaryName = if (osType == OSType.WINDOWS) GO_EXE_NAME else GO_BIN_NAME
            val goBinaryFile = File(goBinPath, goBinaryName)
            val needDownload = if (!goBinaryFile.exists()) {
                LoggerService.addNormalLine("Go $runtimeVersion not found, starting download...")
                true
            } else {
                // 缓存命中，检查版本号是否匹配（版本匹配则不会触发 GOTOOLCHAIN=auto）
                val cachedVersion = getGoVersion(goBinaryFile, envDir)
                if (cachedVersion != null && cachedVersion.startsWith("$runtimeVersion.")) {
                    LoggerService.addNormalLine(
                        "Go $runtimeVersion (cached $cachedVersion) already exists at $goVersionDir"
                    )
                    false
                } else {
                    goVersionDir.deleteRecursively()
                    LoggerService.addNormalLine(
                        "Go $runtimeVersion cache version mismatch (cached=$cachedVersion), re-downloading..."
                    )
                    true
                }
            }
            if (needDownload) {
                val pkgFile = File(envDir, "$GO_DIR_PREFIX/${storePkgRunEnvInfo.pkgName}")
                prepareGoEnv(
                    retryNum = RETRY_NUM,
                    envDir = envDir,
                    osType = osType,
                    pkgFile = pkgFile,
                    goVersionDir = goVersionDir,
                    pkgDownloadPath = storePkgRunEnvInfo.pkgDownloadPath
                )
                pkgFile.delete()
            }
            return goExecutePath
        }
        return null
    }

    /**
     * 下载并解压 Go 二进制包
     * 使用循环而非递归重试，避免下载成功后因解压/验证失败而重复下载
     */
    @Suppress("LongParameterList")
    private fun prepareGoEnv(
        retryNum: Int,
        envDir: File,
        osType: OSType,
        pkgFile: File,
        goVersionDir: File,
        pkgDownloadPath: String
    ) {
        val pkgName = pkgFile.name
        var remainingRetries = retryNum
        var downloadSucceeded = false

        while (remainingRetries >= 0) {
            // goVersionDir 每次循环都清空（解压路径必须干净）
            if (goVersionDir.exists()) {
                goVersionDir.deleteRecursively()
            }
            // pkgFile 仅在需要重新下载时删除，已下载成功时保留供后续重试解压
            if (!downloadSucceeded && pkgFile.exists()) {
                pkgFile.delete()
            }
            try {
                // 下载 Go 安装包（仅在之前未下载成功时执行）
                if (!downloadSucceeded) {
                    OkhttpUtils.downloadFile(
                        url = pkgDownloadPath,
                        destPath = pkgFile,
                        readTimeoutInSec = 180
                    )
                    downloadSucceeded = true
                }

                if (osType == OSType.WINDOWS) {
                    ZipUtil.unZipFile(pkgFile, goVersionDir.absolutePath, false)
                } else {
                    extractGoTarball(envDir, pkgName, pkgFile, goVersionDir)
                }

                // 验证解压后的 Go 二进制文件
                val goBinaryName = if (osType == OSType.WINDOWS) GO_EXE_NAME else GO_BIN_NAME
                val goBinaryFile = File(File(goVersionDir, "bin"), goBinaryName)
                require(goBinaryFile.exists()) { "Go binary not found after extraction" }
                LoggerService.addNormalLine("prepareGoEnv decompress [$pkgName] success")
                return
            } catch (e: Throwable) {
                LoggerService.addWarnLine(
                    "prepareGoEnv pkg[$pkgName] fail, remainingRetries: $remainingRetries, " +
                        "error: ${e.message}"
                )
                if (remainingRetries == 0) {
                    throw TaskExecuteException(
                        errorType = ErrorType.SYSTEM,
                        errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR,
                        errorMsg = "Script command execution failed because of ${e.message}"
                    )
                }
                remainingRetries--
                // 下载失败（downloadSucceeded=false）→ 下次循环删除旧pkgFile并重新下载
                // 解压/验证失败（downloadSucceeded=true）→ 下次循环保留pkgFile，跳过下载只重新解压
            }
        }
    }

    /**
     * 解压 Go tarball（Linux/Mac），处理 go/ 顶层目录
     */
    private fun extractGoTarball(envDir: File, pkgName: String, pkgFile: File, goVersionDir: File) {
        val tmpDir = File(envDir, "$GO_DIR_PREFIX/tmp")
        tmpDir.mkdirs()
        CommandLineUtils.execute(
            "tar -xzf $pkgName -C ${tmpDir.absolutePath}",
            pkgFile.parentFile,
            print2Logger = true
        )
        val extractedGoDir = File(tmpDir, "go")
        if (extractedGoDir.exists()) {
            extractedGoDir.renameTo(goVersionDir)
        } else {
            tmpDir.listFiles()?.forEach { it.renameTo(File(goVersionDir, it.name)) }
        }
        tmpDir.delete()
    }

    /**
     * 运行 go version 并解析版本号（如 "go version go1.23.12 linux/amd64" → "1.23.12"）
     * 返回 null 表示执行失败或无法解析版本
     */
    private fun getGoVersion(goBinary: File, envDir: File): String? {
        return try {
            val process = ProcessBuilder(goBinary.absolutePath, "version")
                .directory(envDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            Regex("""go(\d+\.\d+\.?\d*)""").find(output)?.groupValues?.get(1)
        } catch (e: Exception) {
            LoggerService.addWarnLine("getGoVersion failed for ${goBinary.absolutePath}: ${e.message}")
            null
        }
    }

    override fun handleAtomTarget(
        target: String,
        osType: OSType,
        postEntryParam: String?
    ): String {
        var convertTarget = target
        if (!postEntryParam.isNullOrBlank()) {
            convertTarget = "$target --postAction=$postEntryParam"
        }
        logger.info("handleAtomTarget convertTarget:$convertTarget")
        return convertTarget
    }

    override fun handleAtomPreCmd(
        preCmd: String,
        osName: String,
        pkgName: String,
        runtimeVersion: String?
    ): String {
        val preCmds = CommonUtils.strToList(preCmd).toMutableList()
        if (osName != OSType.WINDOWS.name.lowercase()) {
            preCmds.add(0, "chmod +x $pkgName")
        }
        logger.info("handleAtomPreCmd convertPreCmd:$preCmds")
        return JsonUtil.toJson(preCmds, false)
    }
}
