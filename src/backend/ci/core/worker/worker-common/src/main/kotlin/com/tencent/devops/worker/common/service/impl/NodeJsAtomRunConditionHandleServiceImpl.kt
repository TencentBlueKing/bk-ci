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

import com.tencent.devops.common.api.constant.NODEJS
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.worker.common.BK_CI_ATOM_EXECUTE_ENV_PATH
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.AtomRunConditionHandleService
import com.tencent.devops.worker.common.utils.WorkspaceUtils
import org.slf4j.LoggerFactory
import java.io.File

class NodeJsAtomRunConditionHandleServiceImpl : AtomRunConditionHandleService {

    private val logger = LoggerFactory.getLogger(NodeJsAtomRunConditionHandleServiceImpl::class.java)

    override fun prepareRunEnv(
        osType: OSType,
        language: String,
        runtimeVersion: String,
        workspace: File
    ): Boolean {
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
        logger.info("prepareRunEnv param:[$osType,$language,$runtimeVersion,$envDir,$storePkgRunEnvInfo]")
        storePkgRunEnvInfo?.let {
            // 判断nodejs安装包是否已经存在构建机上
            val pkgName = storePkgRunEnvInfo.pkgName
            val pkgFileFolderName = if (osType == OSType.WINDOWS) {
                pkgName.removeSuffix(".zip")
            } else {
                pkgName.removeSuffix(".tar.gz")
            }
            val pkgFileDir = File(envDir, "$NODEJS/$pkgFileFolderName")
            if (pkgFileDir.exists() && pkgFileDir.listFiles()?.isEmpty() == true) {
                // 空文件夹需要删除
                pkgFileDir.delete()
            }
            if (!pkgFileDir.exists()) {
                // 把指定的nodejs安装包下载到构建机上
                val pkgFile = File(envDir, "$NODEJS/$pkgName")
                OkhttpUtils.downloadFile(storePkgRunEnvInfo.pkgDownloadPath, pkgFile)
                logger.info("prepareRunEnv download [$pkgName] success")
                // 把nodejs安装包解压到构建机上
                isUnzipSuccess(
                    retryNum = 3,
                    pkgFile = pkgFile,
                    pkgFileDir = pkgFileDir,
                    envDir = envDir,
                    osType = osType,
                    pkgName = pkgName
                )
                // 删除安装包
                pkgFile.delete()
                logger.info("prepareRunEnv decompress [$pkgName] success")
            }
            // 把nodejs执行路径写入系统变量
            val nodejsPath = if (osType == OSType.WINDOWS) {
                pkgFileDir.absolutePath
            } else {
                "${pkgFileDir.absolutePath}/bin"
            }
            System.setProperty(BK_CI_ATOM_EXECUTE_ENV_PATH, "$nodejsPath${File.separator}")
        }
        return true
    }

    override fun handleAtomTarget(
        target: String,
        osType: OSType,
        postEntryParam: String?
    ): String {
        logger.info("handleAtomTarget target:$target,osType:$osType,postEntryParam:$postEntryParam")
        var convertTarget = target
        val executePath = System.getProperty(BK_CI_ATOM_EXECUTE_ENV_PATH)
        if (!executePath.isNullOrBlank()) {
            convertTarget = "$executePath$target"
        }
        if (!postEntryParam.isNullOrBlank()) {
            convertTarget = "$convertTarget --post-action=$postEntryParam"
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
        preCmds.add(0, "tar -xzf $pkgName")
        logger.info("handleAtomPreCmd convertPreCmd:$preCmds")
        return JsonUtil.toJson(preCmds, false)
    }

    private fun isUnzipSuccess(
        retryNum: Int,
        pkgFile: File,
        pkgFileDir: File,
        envDir: File,
        osType: OSType,
        pkgName: String
    ) {
        try {
            if (osType == OSType.WINDOWS) {
                //ZipUtil.unZipFile(pkgFile, pkgFileDir.absolutePath, false)
                CommandLineUtils.execute("node -v", pkgFileDir.absoluteFile, true)
            } else {
                //CommandLineUtils.execute("tar -xzf $pkgName", File(envDir, NODEJS), true)
                CommandLineUtils.execute("node -v", File(envDir, NODEJS).absoluteFile, true)
            }
        } catch (e: Exception) {
            logger.info("循环次数$retryNum")
            if (retryNum == 0) {
                throw TaskExecuteException(
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_SCRIPT_COMMAND_INVAILD,
                    errorMsg = "Script command execution failed because of ${e.message}"
                )
            }
            isUnzipSuccess(
                retryNum = retryNum - 1,
                pkgFile = pkgFile,
                pkgFileDir = pkgFileDir,
                envDir = envDir,
                osType = osType,
                pkgName = pkgName
            )
        }
    }
}
