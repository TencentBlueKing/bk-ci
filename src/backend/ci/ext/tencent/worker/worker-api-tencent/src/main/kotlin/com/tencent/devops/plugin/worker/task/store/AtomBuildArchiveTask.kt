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

package com.tencent.devops.plugin.worker.task.store

import com.tencent.devops.common.api.constant.KEY_OS_ARCH
import com.tencent.devops.common.api.constant.KEY_OS_NAME
import com.tencent.devops.common.api.constant.KEY_VALID_OS_ARCH_FLAG
import com.tencent.devops.common.api.constant.KEY_VALID_OS_NAME_FLAG
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.pojo.element.market.AtomBuildArchiveElement
import com.tencent.devops.common.pipeline.utils.ParameterUtils
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.task.market.AtomRunConditionFactory
import com.tencent.devops.worker.common.utils.ArchiveUtils
import java.io.File
import java.nio.file.Paths

@Suppress("UNUSED", "ComplexMethod")
@TaskClassType(classTypes = [AtomBuildArchiveElement.classType])
class AtomBuildArchiveTask : ITask() {

    private val atomApi = ApiFactory.create(AtomArchiveSDKApi::class)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val destPath = taskParams["destPath"] ?: throw TaskExecuteException(
            errorMsg = "param [destPath] is empty",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
        )
        val filePath = taskParams["filePath"] ?: throw TaskExecuteException(
            errorMsg = "param [filePath] is empty",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
        )
        val buildVariable = buildTask.buildVariable
        val atomCode = buildVariable!!["atomCode"] ?: throw TaskExecuteException(
            errorMsg = "need atomCode param",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
        )
        val atomVersion = buildVariable["version"] ?: throw TaskExecuteException(
            errorMsg = "need version param",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
        )
        val packageName = buildVariable["packageName"] ?: throw TaskExecuteException(
            errorMsg = "need packageName param",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
        )
        val preCmd = buildVariable["preCmd"] ?: ""
        val target = buildVariable["target"]

        val fileSha = archiveAtom(
            atomCode = atomCode,
            atomVersion = atomVersion,
            file = File(workspace, filePath),
            destPath = destPath,
            buildVariables = buildVariables
        )

        val frontendFilePath = taskParams["frontendFilePath"]
        // 判断是否是自定义UI类型的插件，如果是则需要把前端文件上传至仓库的路径
        if (null != frontendFilePath) {
            val frontendDestPath = taskParams["frontendDestPath"] ?: throw TaskExecuteException(
                errorMsg = "param [frontendDestPath] is empty",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
            )
            val baseFile = File(workspace, frontendFilePath)
            val baseFileDirPath = Paths.get(baseFile.canonicalPath)
            val fileList = ArchiveUtils.recursiveGetFiles(baseFile)
            fileList.forEach {
                val relativePath = baseFileDirPath.relativize(Paths.get(it.canonicalPath)).toString()
                val fileSeparator = System.getProperty("file.separator")
                atomApi.uploadAtomStaticFile(
                    atomCode = atomCode,
                    atomVersion = atomVersion,
                    file = it,
                    destPath = frontendDestPath + fileSeparator + relativePath,
                    buildVariables = buildVariables
                )
            }
        }
        val osName = taskParams[KEY_OS_NAME]
        val osArch = taskParams[KEY_OS_ARCH]
        val validOsNameFlag = buildVariable[KEY_VALID_OS_NAME_FLAG]?.toBoolean()
        val validOsArchFlag = buildVariable[KEY_VALID_OS_ARCH_FLAG]?.toBoolean()
        val finalOsName = if (validOsNameFlag == true) {
            osName
        } else {
            null
        }
        val finalOsArch = if (validOsArchFlag == true) {
            osArch
        } else {
            null
        }
        val atomEnv = atomEnv(
            projectId = buildVariables.projectId,
            atomCode = atomCode,
            atomVersion = atomVersion,
            osName = finalOsName,
            osArch = finalOsArch
        )

        val userId = ParameterUtils.getListValueByKey(buildVariables.variablesWithType, PIPELINE_START_USER_ID)
            ?: throw TaskExecuteException(
                errorMsg = "user basic info error, please check environment.",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
            )
        val language = atomEnv.language!!
        val atomRunConditionHandleService = AtomRunConditionFactory.createAtomRunConditionHandleService(language)
        val request = AtomEnvRequest(
            userId = userId,
            pkgName = packageName,
            pkgRepoPath = destPath,
            language = language,
            minVersion = atomEnv.minVersion,
            target = target ?: atomEnv.target,
            shaContent = fileSha,
            preCmd = atomRunConditionHandleService.handleAtomPreCmd(
                preCmd = preCmd,
                osName = osName ?: "",
                pkgName = packageName,
                runtimeVersion = atomEnv.runtimeVersion
            ),
            atomPostInfo = atomEnv.atomPostInfo,
            osName = finalOsName,
            osArch = finalOsArch
        )
        val result = atomApi.updateAtomEnv(buildVariables.projectId, atomCode, atomVersion, request)
        if (result.data != null && result.data == true) {
            LoggerService.addNormalLine("update Atom Env ok!")
        } else {
            throw TaskExecuteException(
                errorMsg = "update Atom Env fail: ${result.message}",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
            )
        }
    }

    private fun atomEnv(
        projectId: String,
        atomCode: String,
        atomVersion: String,
        osName: String? = null,
        osArch: String? = null
    ): AtomEnv {
        val atomEnvResult = atomApi.getAtomEnv(
            projectCode = projectId,
            atomCode = atomCode,
            atomVersion = atomVersion,
            atomStatus = AtomStatusEnum.BUILDING.status.toByte(),
            osName = osName,
            osArch = osArch,
            convertOsFlag = false
        )
        return atomEnvResult.data ?: throw TaskExecuteException(
            errorMsg = "can not found any $atomCode env",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
        )
    }

    private fun archiveAtom(
        atomCode: String,
        atomVersion: String,
        file: File,
        destPath: String,
        buildVariables: BuildVariables
    ): String {
        val fileSha = atomApi.archiveAtom(
            atomCode = atomCode,
            atomVersion = atomVersion,
            file = file,
            destPath = destPath,
            buildVariables = buildVariables
        )
        if (fileSha.isBlank()) {
            throw TaskExecuteException(
                errorMsg = "atom file check sha fail!",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
            )
        }
        return fileSha
    }
}
