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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.worker.common.BK_ATOM_PYTHON_VENV_ENABLED
import com.tencent.devops.worker.common.PYTHON_VENV_DIR
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.AtomRunConditionHandleService
import java.io.File
import org.slf4j.LoggerFactory

class PythonAtomRunConditionHandleServiceImpl : AtomRunConditionHandleService {

    private val logger = LoggerFactory.getLogger(PythonAtomRunConditionHandleServiceImpl::class.java)

    override fun prepareRunEnv(
        osType: OSType,
        language: String,
        runtimeVersion: String,
        workspace: File,
        atomTmpSpace: File?,
        runtimeVariables: Map<String, String>
    ): String? {
        if (atomTmpSpace == null) {
            logger.warn("prepareRunEnv atomTmpSpace is null, skip venv creation")
            return null
        }
        // 从插件配置的环境变量中判断是否启用虚拟环境
        val venvEnabled = runtimeVariables[BK_ATOM_PYTHON_VENV_ENABLED]
        if (venvEnabled?.toBoolean() != true) {
            LoggerService.addWarnLine("prepareRunEnv python venv is not enabled, skip")
            return null
        }
        // 根据runtimeVersion确定Python命令
        val pythonCmd = if (runtimeVersion in setOf("python3", "python2")) runtimeVersion else "python"
        logger.info("prepareRunEnv pythonCmd:$pythonCmd, runtimeVersion:$runtimeVersion")
        // 验证Python命令是否可用
        if (!isPythonAvailable(pythonCmd)) {
            logger.warn("prepareRunEnv python command[$pythonCmd] is not available, fallback to system env")
            LoggerService.addWarnLine("Python command [$pythonCmd] is not available, skip venv creation")
            return null
        }
        val venvPath = File(atomTmpSpace, PYTHON_VENV_DIR)
        // 根据Python版本创建虚拟环境
        val binPath = if (runtimeVersion == "python3") {
            createPython3Venv(pythonCmd = pythonCmd, venvPath = venvPath, osType = osType)
        } else {
            createPython2Venv(pythonCmd = pythonCmd, venvPath = venvPath, osType = osType)
        }
        if (binPath == null) {
            logger.warn("prepareRunEnv create venv failed, fallback to system env")
            LoggerService.addWarnLine("Failed to create python venv, fallback to system environment")
            // 清理创建失败残留的虚拟环境目录
            cleanupVenvDir(venvPath)
        }
        return binPath
    }

    override fun handleAtomTarget(
        target: String,
        osType: OSType,
        postEntryParam: String?,
        atomExecuteEnvPath: String?
    ): String {
        // 若虚拟环境路径存在，将启动命令拼接为虚拟环境内的绝对路径
        var convertTarget = if (!atomExecuteEnvPath.isNullOrBlank()) {
            val fullPath = "$atomExecuteEnvPath${File.separator}$target"
            // Windows路径含反斜杠，用双引号包裹防止被shell错误解析
            if (osType == OSType.WINDOWS) "\"$fullPath\"" else fullPath
        } else {
            target
        }
        if (!postEntryParam.isNullOrBlank()) {
            convertTarget = "$convertTarget --post_action=$postEntryParam"
        }
        logger.info("handleAtomTarget convertTarget:$convertTarget")
        return convertTarget
    }

    override fun handleAtomPreCmd(
        preCmd: String,
        osName: String,
        pkgName: String,
        runtimeVersion: String?,
        atomExecuteEnvPath: String?
    ): String {
        val preCmds = CommonUtils.strToList(preCmd).toMutableList()
        val pipCmd = if (runtimeVersion == "python3") "pip3" else "pip"
        // 若虚拟环境路径存在，使用绝对路径执行pip，Windows路径用双引号包裹
        val fullPipCmd = if (!atomExecuteEnvPath.isNullOrBlank()) {
            val fullPath = "$atomExecuteEnvPath${File.separator}$pipCmd"
            if (osName == OSType.WINDOWS.name.lowercase()) "\"$fullPath\"" else fullPath
        } else {
            pipCmd
        }
        preCmds.add(0, "$fullPipCmd --default-timeout=600 install $pkgName --upgrade")
        logger.info("handleAtomPreCmd convertPreCmd:$preCmds")
        return JsonUtil.toJson(preCmds, false)
    }

    /** 验证Python命令是否可用 */
    private fun isPythonAvailable(pythonCmd: String): Boolean {
        return try {
            CommandLineUtils.execute(
                command = "$pythonCmd --version",
                workspace = null,
                print2Logger = true
            )
            true
        } catch (ignored: Throwable) {
            logger.warn("isPythonAvailable check [$pythonCmd --version] failed: ${ignored.message}")
            false
        }
    }

    /** 使用python3 -m venv创建虚拟环境，成功返回bin路径，失败返回null */
    private fun createPython3Venv(pythonCmd: String, venvPath: File, osType: OSType): String? {
        return try {
            val command = "$pythonCmd -m venv ${venvPath.absolutePath}"
            logger.info("createPython3Venv command:$command")
            LoggerService.addNormalLine("Creating python3 venv: $command")
            CommandLineUtils.execute(
                command = command,
                workspace = venvPath.parentFile,
                print2Logger = true
            )
            val binPath = getVenvBinPath(venvPath = venvPath, osType = osType)
            logger.info("createPython3Venv success, binPath:$binPath")
            LoggerService.addNormalLine("Python3 venv created successfully at: $binPath")
            binPath
        } catch (e: Throwable) {
            logger.warn("createPython3Venv failed: ${e.message}", e)
            null
        }
    }

    /** 使用python2 -m virtualenv创建虚拟环境，成功返回bin路径，失败返回null */
    private fun createPython2Venv(pythonCmd: String, venvPath: File, osType: OSType): String? {
        return try {
            // 检查virtualenv是否可用
            if (!isVirtualenvAvailable(pythonCmd)) {
                logger.info("virtualenv not found, installing...")
                LoggerService.addNormalLine("Installing virtualenv...")
                CommandLineUtils.execute(
                    command = "pip install virtualenv",
                    workspace = null,
                    print2Logger = true
                )
            }
            val command = "$pythonCmd -m virtualenv ${venvPath.absolutePath}"
            logger.info("createPython2Venv command:$command")
            LoggerService.addNormalLine("Creating python2 venv: $command")
            CommandLineUtils.execute(
                command = command,
                workspace = venvPath.parentFile,
                print2Logger = true
            )
            val binPath = getVenvBinPath(venvPath = venvPath, osType = osType)
            logger.info("createPython2Venv success, binPath:$binPath")
            LoggerService.addNormalLine("Python2 venv created successfully at: $binPath")
            binPath
        } catch (e: Throwable) {
            logger.warn("createPython2Venv failed: ${e.message}", e)
            null
        }
    }

    /** 检查virtualenv模块是否可用 */
    private fun isVirtualenvAvailable(pythonCmd: String): Boolean {
        return try {
            CommandLineUtils.execute(
                command = "$pythonCmd -m virtualenv --version",
                workspace = null,
                print2Logger = true
            )
            true
        } catch (ignored: Throwable) {
            false
        }
    }

    /** 根据操作系统获取虚拟环境bin目录路径 */
    private fun getVenvBinPath(venvPath: File, osType: OSType): String {
        val binDir = if (osType == OSType.WINDOWS) "Scripts" else "bin"
        return File(venvPath, binDir).absolutePath
    }

    /** 清理创建失败残留的虚拟环境目录 */
    private fun cleanupVenvDir(venvPath: File) {
        try {
            if (venvPath.exists()) {
                venvPath.deleteRecursively()
                logger.info("cleanupVenvDir success, path:${venvPath.absolutePath}")
                LoggerService.addWarnLine("cleanupVenvDir success, path:${venvPath.absolutePath}")
            }
        } catch (e: Throwable) {
            logger.warn("cleanupVenvDir failed, path:${venvPath.absolutePath}, error:${e.message}")
        }
    }
}
