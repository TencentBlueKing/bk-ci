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

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.element.store.StoreCodeccValidateElement
import com.tencent.devops.common.pipeline.utils.ParameterUtils
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.store.pojo.common.StoreValidateCodeccResultRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.worker.common.api.store.StoreCodeccResourceApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import org.slf4j.LoggerFactory
import java.io.File

@TaskClassType(classTypes = [StoreCodeccValidateElement.classType])
class StoreCodeccValidateTask : ITask() {

    private val logger = LoggerFactory.getLogger(StoreCodeccValidateTask::class.java)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        logger.info("StoreCodeccValidateTask buildTask: $buildTask,buildVariables: $buildVariables")
        val buildId = buildTask.buildId
        val params = buildTask.params ?: mapOf()
        val storeCode = params["storeCode"] ?: throw TaskExecuteException(
            errorMsg = "param [storeCode] is empty",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
        )
        val storeType = params["storeType"] ?: throw TaskExecuteException(
            errorMsg = "param [storeType] is empty",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
        )
        val language = params["language"] ?: throw TaskExecuteException(
            errorMsg = "param [language] is empty",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
        )
        // 根据校验标准模型去校验codecc代码扫描的指标是否满足需求
        LoggerService.addNormalLine("codecc validate start")
        val storeCodeccResourceApi = StoreCodeccResourceApi()
        val userId = ParameterUtils.getListValueByKey(buildVariables.variablesWithType, PIPELINE_START_USER_ID) ?: throw TaskExecuteException(
            errorMsg = "user basic info error, please check environment.",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
        )
        val storeValidateCodeccResultRequest = StoreValidateCodeccResultRequest(
            projectCode = buildVariables.projectId,
            userId = userId,
            buildId = buildId,
            storeCode = storeCode,
            storeType = StoreTypeEnum.valueOf(storeType),
            language = language
        )
        val codeccValidateResult = storeCodeccResourceApi.validate(storeValidateCodeccResultRequest)
        LoggerService.addNormalLine("codeccValidateResult: $codeccValidateResult")
        if (codeccValidateResult.isNotOk()) {
            LoggerService.addErrorLine(JsonUtil.toJson(codeccValidateResult))
            throw TaskExecuteException(
                errorMsg = "validate fail: ${codeccValidateResult.message}",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
            )
        }
    }
}
