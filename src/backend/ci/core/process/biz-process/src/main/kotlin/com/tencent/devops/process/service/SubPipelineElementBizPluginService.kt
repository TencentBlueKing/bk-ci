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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementBatchCheckParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementHolder
import com.tencent.devops.common.pipeline.pojo.element.atom.PipelineCheckFailedErrors
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PIPELINE_ELEMENT_CHECK_FAILED_MESSAGE
import com.tencent.devops.process.engine.atom.plugin.IElementBizPluginService
import com.tencent.devops.process.pojo.pipeline.SubPipelineIdAndName
import com.tencent.devops.process.engine.service.SubPipelineRefService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 子流水线插件扩展点处理类
 */
@Service
class SubPipelineElementBizPluginService @Autowired constructor(
    private val subPipelineCheckService: SubPipelineCheckService,
    private val subPipelineRefService: SubPipelineRefService
) : IElementBizPluginService {

    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineElementBizPluginService::class.java)
    }

    override fun supportElement(element: Element): Boolean {
        return subPipelineCheckService.supportElement(element)
    }

    override fun supportAtomCode(atomCode: String): Boolean {
        return subPipelineCheckService.supportAtomCode(atomCode)
    }

    override fun beforeDelete(element: Element, param: BeforeDeleteParam) {
        element.id?.let {
            subPipelineRefService.delete(
                projectId = param.projectId,
                pipelineId = param.pipelineId,
                taskId = it
            )
        }
    }

    override fun batchCheck(elements: List<ElementHolder>, param: ElementBatchCheckParam) {
        if (param.projectId.isNullOrBlank() || param.isTemplate) return
        val errors = mutableListOf<PipelineCheckFailedErrors.ErrorInfo>()
        with(param) {
            val subPipelineElementMap = subPipelineCheckService.distinctSubPipeline(
                projectId = projectId!!,
                elements = elements,
                contextMap = contextMap
            )
            val watcher = Watcher("batch check sub pipeline|${param.pipelineId}")
            watcher.start("check branch version resource")
            checkBranchVersion(
                param = param,
                subPipelineElementMap = subPipelineElementMap,
                errors = errors
            )
            watcher.start("check permission")
            checkPermission(
                param = param,
                subPipelineElementMap = subPipelineElementMap,
                errors = errors
            )
            watcher.start("check cycle")
            checkCycle(
                param = param,
                subPipelineElementMap = subPipelineElementMap,
                errors = errors
            )
            watcher.stop()
        }
        if (errors.isNotEmpty()) {
            val failedReason = PipelineCheckFailedErrors(
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_PIPELINE_ELEMENT_CHECK_FAILED_MESSAGE
                ),
                errors = errors
            )
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_ELEMENT_CHECK_FAILED,
                params = arrayOf(JsonUtil.toJson(failedReason))
            )
        }
    }

    fun checkPermission(
        param: ElementBatchCheckParam,
        subPipelineElementMap: Map<SubPipelineIdAndName, MutableList<ElementHolder>>,
        errors: MutableList<PipelineCheckFailedErrors.ErrorInfo>
    ) {
        with(param) {
            val userId = oauthUser ?: userId
            val errorDetails = subPipelineCheckService.batchCheckPermission(
                userId = userId,
                permission = AuthPermission.EXECUTE,
                subPipelineElementMap = subPipelineElementMap
            )
            if (errorDetails.isNotEmpty()) {
                val errorInfo = PipelineCheckFailedErrors.ErrorInfo(
                    errorTitle = I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_NOT_SUB_PIPELINE_EXECUTE_PERMISSION_ERROR_TITLE,
                        params = arrayOf(userId)
                    ),
                    errorDetails = errorDetails
                )
                errors.add(errorInfo)
            }
        }
    }

    fun checkCycle(
        param: ElementBatchCheckParam,
        subPipelineElementMap: Map<SubPipelineIdAndName, MutableList<ElementHolder>>,
        errors: MutableList<PipelineCheckFailedErrors.ErrorInfo>
    ) {
        with(param) {
            val errorDetails = subPipelineCheckService.batchCheckCycle(
                projectId = projectId!!,
                pipelineId = pipelineId,
                subPipelineElementMap = subPipelineElementMap
            )
            if (errorDetails.isNotEmpty()) {
                val errorInfo = PipelineCheckFailedErrors.ErrorInfo(
                    errorTitle = I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.BK_SUB_PIPELINE_CIRCULAR_DEPENDENCY_ERROR_TITLE
                    ),
                    errorDetails = errorDetails
                )
                errors.add(errorInfo)
            }
        }
    }

    fun checkBranchVersion(
        param: ElementBatchCheckParam,
        subPipelineElementMap: Map<SubPipelineIdAndName, MutableList<ElementHolder>>,
        errors: MutableList<PipelineCheckFailedErrors.ErrorInfo>
    ) {
        with(param) {
            val errorDetails = subPipelineCheckService.batchCheckBranchVersion(
                projectId = projectId!!,
                pipelineId = pipelineId,
                subPipelineElementMap = subPipelineElementMap
            )
            if (errorDetails.isNotEmpty()) {
                val errorInfo = PipelineCheckFailedErrors.ErrorInfo(
                    errorTitle = I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_BRANCH_TITLE
                    ),
                    errorDetails = errorDetails
                )
                errors.add(errorInfo)
            }
        }
    }
}
