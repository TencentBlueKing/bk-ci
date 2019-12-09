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

package com.tencent.devops.plugin.worker.task.archive

import com.tencent.devops.common.pipeline.pojo.element.market.AtomBuildArchiveElement
import com.tencent.devops.common.pipeline.utils.ParameterUtils
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import java.io.File

@TaskClassType(classTypes = [AtomBuildArchiveElement.classType])
class AtomBuildArchiveTask : ITask() {

    private val atomApi = ApiFactory.create(AtomArchiveSDKApi::class)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val destPath = taskParams["destPath"] ?: throw TaskExecuteException(
            errorMsg = "param [destPath] is empty",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )
        val filePath = taskParams["filePath"] ?: throw TaskExecuteException(
            errorMsg = "param [filePath] is empty",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )

        val fileSha = atomApi.archiveAtom(filePath, destPath, workspace, buildVariables)
        if (fileSha.isNullOrBlank()) {
            throw TaskExecuteException(
                errorMsg = "atom file check sha fail!",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }

        val atomCode = buildTask.buildVariable!!["atomCode"] ?: throw TaskExecuteException(
            errorMsg = "need atomCode param",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )
        val atomVersion = buildTask.buildVariable!!["version"] ?: throw TaskExecuteException(
            errorMsg = "need version param",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )
        val preCmd = buildTask.buildVariable!!["preCmd"]
        val target = buildTask.buildVariable!!["target"]
        val atomEnvResult = atomApi.getAtomEnv(buildVariables.projectId, atomCode, atomVersion)
        val userId = ParameterUtils.getListValueByKey(buildVariables.variablesWithType, PIPELINE_START_USER_ID) ?: ""
        val atomEnv = atomEnvResult.data ?: throw TaskExecuteException(
            errorMsg = "can not found any $atomCode env",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        )
        val request = AtomEnvRequest(
            userId = userId,
            pkgPath = destPath,
            language = atomEnv.language,
            minVersion = atomEnv.minVersion,
            target = target ?: atomEnv.target,
            shaContent = fileSha,
            preCmd = preCmd
        )
        val result = atomApi.updateAtomEnv(buildVariables.projectId, atomCode, atomVersion, request)
        if (result.data != null && result.data == true) {
            LoggerService.addNormalLine("update Atom Env ok!")
        } else {
            throw TaskExecuteException(
                errorMsg = "update Atom Env fail: ${result.message}",
                errorType = ErrorType.SYSTEM,
                errorCode = AtomErrorCode.SYSTEM_SERVICE_ERROR
            )
        }
    }
}
