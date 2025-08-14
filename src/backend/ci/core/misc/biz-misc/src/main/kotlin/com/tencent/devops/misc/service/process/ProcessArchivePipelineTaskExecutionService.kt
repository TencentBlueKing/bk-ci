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

package com.tencent.devops.misc.service.process

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_CANCEL_FLAG
import com.tencent.devops.common.api.constant.KEY_SEND_MSG_FLAG
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.task.event.BatchTaskPublishEvent
import com.tencent.devops.common.task.pojo.TaskResult
import com.tencent.devops.common.task.service.TaskExecutionService
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_PROJECT_ID
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("PIPELINE_ARCHIVE_TASK_EXECUTION")
class ProcessArchivePipelineTaskExecutionService @Autowired constructor(
    private val dslContext: DSLContext,
    private val processDao: ProcessDao,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val authPermissionApi: AuthPermissionApi,
    private val processArchivePipelineDataMigrateService: ProcessArchivePipelineDataMigrateService
) : TaskExecutionService {

    override fun doBus(batchTaskPublishEvent: BatchTaskPublishEvent): TaskResult {
        val userId = batchTaskPublishEvent.userId
        val taskId = batchTaskPublishEvent.taskId
        val data = batchTaskPublishEvent.data
        val projectId = data[KEY_PROJECT_ID].toString()
        val pipelineId = data[KEY_PIPELINE_ID].toString()
        val pipelineExists = processDao.getPipelineInfoByPipelineId(dslContext, projectId, pipelineId) != null
        if (!pipelineExists) {
            val params = arrayOf(pipelineId)
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
                params = params,
                defaultMessage = I18nUtil.getCodeLanMessage(
                    CommonMessageCode.ERROR_INVALID_PARAM_, params = params
                )
            )
        }
        // 验证用户对流水线的归档权限
        if (!authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = AuthResourceType.PIPELINE_DEFAULT,
                projectCode = projectId,
                resourceCode = pipelineId,
                permission = AuthPermission.ARCHIVE
            )
        ) {
            // 权限验证失败时返回错误结果
            return TaskResult(
                taskId = taskId,
                success = false,
                result = I18nUtil.getCodeLanMessage(
                    messageCode = CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    params = arrayOf(userId, projectId, pipelineId)
                )
            )
        }
        // 执行流水线数据迁移归档操作
        processArchivePipelineDataMigrateService.migrateData(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            cancelFlag = data[KEY_CANCEL_FLAG] as Boolean,
            sendMsgFlag = data[KEY_SEND_MSG_FLAG] as Boolean
        )
        return TaskResult(taskId = taskId, success = true)
    }
}
