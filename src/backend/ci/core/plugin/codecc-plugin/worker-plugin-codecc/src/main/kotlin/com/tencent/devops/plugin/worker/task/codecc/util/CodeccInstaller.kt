/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.worker.task.codecc.util

import com.google.common.io.Files
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.plugin.codecc.pojo.CodeccToolType
import com.tencent.devops.plugin.worker.pojo.CodeccExecuteConfig
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants.THIRD_CODECC_FOLDER
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants.THIRD_COVERITY_FILE
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants.THIRD_GOMETALINTER_FILE
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants.THIRD_GOROOT_FILE
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants.THIRD_JDK_FILE
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants.THIRD_NODE_FILE
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants.THIRD_PYLINT2_FILE
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants.THIRD_PYLINT3_FILE
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants.THIRD_PYTHON3_TAR_FILE
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.CommandLineUtils
import com.tencent.devops.worker.common.utils.WorkspaceUtils
import java.io.File

object CodeccInstaller {

    private val helper = CodeccToolHelper()

    fun setupTools(coverityConfig: CodeccExecuteConfig) {
        // 安装python 2.x(macOs 不需要安装)
//        LoggerService.addNormalLine("download python 2.x...")
//        if (AgentEnv.getOS() != OSType.MAC_OS) {
//            helper.getTool(CodeccToolType.PYTHON2, THIRD_PYTHON2_TAR_FILE, Runnable {
//                setupPython2(WorkspaceUtils.getLandun(), THIRD_PYTHON2_TAR_FILE)
//            })
//        }

        // 安装coverity
//        if (coverityConfig.tools.contains("COVERITY")) {
//            // 1.1 拉取coverity
//            LoggerService.addNormalLine("download coverity...(该耗时可能比较长，请耐心等候)")
//            // 因为coverity有1G多，所以本地md5存起来，不保留原压缩文件
//            helper.getTool(CodeccToolType.COVERITY, THIRD_COVERITY_FILE, Runnable {
//                setupCov()
//            })
//        }

        // 安装klocwork
//        if (coverityConfig.tools.contains("KLOCWORK")) {
//            // 1.1 拉取coverity
//            LoggerService.addNormalLine("download klocwork...(该耗时可能比较长，请耐心等候)")
//            helper.getTool(CodeccToolType.KLOCWORK, THIRD_KLOCWORK_FILE, Runnable {
//                FileUtil.unzipTgzFile(THIRD_KLOCWORK_FILE.canonicalPath, THIRD_CODECC_FOLDER)
//                THIRD_KLOCWORK_FILE.deleteOnExit()
//            })
//        }

        // 安装node-eslint
        if (coverityConfig.tools.contains("ESLINT")) {
            LoggerService.addNormalLine("download node-v8.9.0-linux-x64_eslint...")
            helper.getTool(CodeccToolType.ESLINT, THIRD_NODE_FILE, Runnable {
                FileUtil.unzipTgzFile(THIRD_NODE_FILE.canonicalPath, THIRD_CODECC_FOLDER)
            })
        }

        // 安装pylint
        if (coverityConfig.tools.contains("PYLINT")) {
            LoggerService.addNormalLine("download pylint2...")
            helper.getTool(CodeccToolType.PYLINT2, THIRD_PYLINT2_FILE, Runnable {
                FileUtil.unzipFile(THIRD_PYLINT2_FILE.canonicalPath, THIRD_CODECC_FOLDER)
            })

            LoggerService.addNormalLine("download pylint3...")
            helper.getTool(CodeccToolType.PYLINT3, THIRD_PYLINT3_FILE, Runnable {
                FileUtil.unzipFile(THIRD_PYLINT3_FILE.canonicalPath, THIRD_CODECC_FOLDER)
            })
        }

        // 安装gometalinter
        if (coverityConfig.tools.contains("GOML")) {
            LoggerService.addNormalLine("download golang...")
            helper.getTool(CodeccToolType.GOLANG, THIRD_GOROOT_FILE, Runnable {
                FileUtil.unzipTgzFile(THIRD_GOROOT_FILE.canonicalPath, THIRD_CODECC_FOLDER)
            })

            LoggerService.addNormalLine("download gometalinter...")
            helper.getTool(CodeccToolType.GOMETALINTER, THIRD_GOMETALINTER_FILE, Runnable {
                FileUtil.unzipFile(THIRD_GOMETALINTER_FILE.canonicalPath, THIRD_CODECC_FOLDER)
            })
        }

        // 安装jdk8
        if (coverityConfig.tools.contains("CCN") && (AgentEnv.getOS() == OSType.LINUX)) {
            LoggerService.addNormalLine("download jdk8...")
            helper.getTool(CodeccToolType.JDK8, THIRD_JDK_FILE, Runnable {
                FileUtil.unzipTgzFile(THIRD_JDK_FILE.canonicalPath, THIRD_CODECC_FOLDER)
            })
        }
    }

    fun setUpPython3(coverityConfig: CodeccExecuteConfig) {
        // 多工具需要安装python3
        // 安装python 3.x
        if (coverityConfig.tools.minus(listOf("COVERITY", "KLOCWORK")).isNotEmpty()) {
            // 2.1 安装python 3.x
            LoggerService.addNormalLine("download python 3.x")
            helper.getTool(CodeccToolType.PYTHON3, THIRD_PYTHON3_TAR_FILE, Runnable {
                setupPython3(WorkspaceUtils.getLandun(), THIRD_PYTHON3_TAR_FILE)
            })
        }
    }

    fun donwloadScript() {
        // 拉取coverity python脚本
        LoggerService.addNormalLine("download cov script...")
        helper.downloadCovScript()

        // 拉取多工具python脚本
        LoggerService.addNormalLine("download tools script...")
        helper.downloadToolScript()
    }

    fun windowsDonwloadScript() {
        // 拉取coverity python脚本
        LoggerService.addNormalLine("download cov script...")
        helper.windowsDownloadCovScript()

        // 拉取多工具python脚本
        LoggerService.addNormalLine("download tools script...")
        helper.windowsDownloadToolScript()
    }

    private fun setupCov() {
        FileUtil.unzipTgzFile(THIRD_COVERITY_FILE.canonicalPath, THIRD_CODECC_FOLDER)
        // 执行相关命令, 加上执行权限
        val commands = mutableListOf("chmod +x ${THIRD_COVERITY_FILE.canonicalPath.removeSuffix(".tar.gz")}/bin/cov-*")
        LoggerService.addNormalLine("execute chmod command: $commands")
        val script = File(WorkspaceUtils.getLandun(), "paas_cov_python_script.sh")
        script.deleteOnExit()
        script.writeText(commands.joinToString(System.lineSeparator()))
        CommandLineUtils.execute(script, WorkspaceUtils.getLandun(), true)
        THIRD_COVERITY_FILE.deleteOnExit()
    }

    private fun setupPython2(workspace: File, pythonFile: File): String {
        try {
            LoggerService.addNormalLine("安装python: ${pythonFile.canonicalPath}")
            // 先解压tgz
            val tmpDir = Files.createTempDir()
            tmpDir.deleteOnExit()
            FileUtil.unzipTgzFile(pythonFile.canonicalPath, tmpDir.canonicalPath)

            // 执行相关命令
            val commands = mutableListOf<String>()
            commands.add("cd $tmpDir/${pythonFile.name.removeSuffix(".tgz")}")
            commands.add("chmod +x ./configure")
            commands.add("chmod +x ./Parser/asdl_c.py")
            commands.add("chmod +x Python/makeopcodetargets.py")
            commands.add("./configure --prefix=${pythonFile.canonicalPath.removeSuffix(".tgz")}")
            commands.add("make && make install")
            commands.forEach { LoggerService.addNormalLine(it) }

            val script = File(workspace, "paas_codecc_python_script.sh")
            script.deleteOnExit()
            script.writeText(commands.joinToString(System.lineSeparator()))
            return CommandLineUtils.execute(script, workspace, true)
        } catch (e: Exception) {
            throw RuntimeException("安装python2失败: ${e.message}")
        }
    }

    private fun setupPython3(workspace: File, pythonFile: File) {
        try {
            // 解压tgz
            val pythonPath = pythonFile.canonicalPath.removeSuffix(".tgz")
            LoggerService.addNormalLine("解压${pythonFile.name}到: $pythonFile")
            FileUtil.unzipTgzFile(pythonFile.canonicalPath, pythonPath)

            // 执行相关命令
            CommandLineUtils.execute("chmod -R 755 $pythonPath/bin/python", workspace, true)
        } catch (e: Exception) {
            throw RuntimeException("安装python3失败: ${e.message}")
        }
    }

    // 命令检查python是否能用
    fun pythonExist(pythonDir: File): Boolean {
        val cmd = "$pythonDir -V"
        return try {
            val result = CommandLineUtils.execute(cmd, null, true)
            LoggerService.addNormalLine("check python result is : $result")
            true
        } catch (e: Exception) {
            LoggerService.addNormalLine("check python result is : ${e.message}")
            false
        }
    }

    fun installMacPython(): String {
        val cmd = "sh /data/soda/apps/python/python_install.sh"
        return CommandLineUtils.execute(cmd, null, true)
    }
}