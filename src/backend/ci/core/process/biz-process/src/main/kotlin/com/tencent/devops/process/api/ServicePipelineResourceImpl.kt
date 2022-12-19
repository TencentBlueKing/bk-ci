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

package com.tencent.devops.process.api

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.event.pojo.measure.PipelineLabelRelateInfo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.ModelUpdate
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.audit.service.AuditService
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.rule.PipelineRuleService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineCopy
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineIdAndName
import com.tencent.devops.process.pojo.PipelineIdInfo
import com.tencent.devops.process.pojo.PipelineName
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.audit.Audit
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.process.pojo.pipeline.enums.PipelineRuleBusCodeEnum
import com.tencent.devops.process.pojo.setting.PipelineModelAndSetting
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.PipelineListFacadeService
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
@RestResource
class ServicePipelineResourceImpl @Autowired constructor(
    private val auditService: AuditService,
    private val pipelineRuleService: PipelineRuleService,
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelinePermissionService: PipelinePermissionService
) : ServicePipelineResource {
    override fun status(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode?
    ): Result<Pipeline?> {
        checkParams(userId, projectId)
        return Result(
            data = pipelineListFacadeService.getSinglePipelineStatus(
                userId = userId,
                projectId = projectId,
                pipeline = pipelineId,
                channelCode = channelCode
            )
        )
    }

    override fun create(
        userId: String,
        projectId: String,
        pipeline: Model,
        channelCode: ChannelCode,
        useTemplateSettings: Boolean?
    ): Result<PipelineId> {
        checkUserId(userId)
        checkProjectId(projectId)
        return Result(
            data = PipelineId(
                pipelineInfoFacadeService.createPipeline(
                    userId = userId,
                    projectId = projectId,
                    model = pipeline,
                    channelCode = channelCode,
                    checkPermission = ChannelCode.isNeedAuth(channelCode),
                    useTemplateSettings = useTemplateSettings
                )
            )
        )
    }

    override fun edit(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipeline: Model,
        channelCode: ChannelCode,
        updateLastModifyUser: Boolean?
    ): Result<Boolean> {
        checkParams(userId, projectId)
        pipelineInfoFacadeService.editPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            model = pipeline,
            channelCode = channelCode,
            checkPermission = ChannelCode.isNeedAuth(channelCode),
            updateLastModifyUser = updateLastModifyUser
        )
        return Result(true)
    }

    override fun copy(
        userId: String,
        projectId: String,
        pipelineId: String,
        pipeline: PipelineCopy
    ): Result<PipelineId> {
        checkParam(userId, projectId)
        val pid = PipelineId(
            pipelineInfoFacadeService.copyPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineCopy = pipeline,
                channelCode = ChannelCode.BS
            )
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pid.id,
                resourceName = pipeline.name,
                userId = userId,
                action = "copy",
                actionContent = "复制流水线/Copy Pipeline from($pipelineId)",
                projectId = projectId
            )
        )
        return Result(pid)
    }

    override fun uploadPipeline(
        userId: String,
        projectId: String,
        modelAndSetting: PipelineModelAndSetting,
        channelCode: ChannelCode,
        useTemplateSettings: Boolean?
    ): Result<PipelineId> {
        val pipelineId = PipelineId(
            id = pipelineInfoFacadeService.uploadPipeline(
                userId = userId,
                projectId = projectId,
                pipelineModelAndSetting = modelAndSetting
            )
        )
        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId.id,
                resourceName = modelAndSetting.model.name,
                userId = userId,
                action = "create",
                actionContent = "调用创建流水线API/Create Pipeline API",
                projectId = projectId
            )
        )
        return Result(pipelineId)
    }

    override fun updatePipeline(
        userId: String,
        projectId: String,
        pipelineId: String,
        modelAndSetting: PipelineModelAndSetting,
        channelCode: ChannelCode
    ): Result<DeployPipelineResult> {
        modelAndSetting.setting.checkParam()
        val buildNumRule = modelAndSetting.setting.buildNumRule
        if (!buildNumRule.isNullOrBlank()) {
            pipelineRuleService.validateRuleStr(buildNumRule, PipelineRuleBusCodeEnum.BUILD_NUM.name)
        }
        val pipelineResult = pipelineInfoFacadeService.saveAll(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            model = modelAndSetting.model,
            setting = modelAndSetting.setting,
            channelCode = ChannelCode.BS
        )

        auditService.createAudit(
            Audit(
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceId = pipelineId,
                resourceName = modelAndSetting.model.name,
                userId = userId,
                action = "edit",
                actionContent = "调用保存流水线API/API Save Ver.${pipelineResult.version}",
                projectId = projectId
            )
        )
        return Result(pipelineResult)
    }

    override fun get(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode
    ): Result<Model> {
        checkParams(userId, projectId)
        return Result(
            pipelineInfoFacadeService.getPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode,
                checkPermission = false
            )
        )
    }

    override fun getWithPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean
    ): Result<Model> {
        checkParams(userId, projectId)
        return Result(
            data = pipelineInfoFacadeService.getPipeline(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode,
                checkPermission = checkPermission
            )
        )
    }

    override fun getSettingWithPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean
    ): Result<PipelineSetting> {
        checkParams(userId, projectId)
        return Result(
            data = pipelineSettingFacadeService.userGetSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode,
                checkPermission = checkPermission
            )
        )
    }

    override fun getBatch(
        userId: String,
        projectId: String,
        pipelineIds: List<String>,
        channelCode: ChannelCode
    ): Result<List<Pipeline>> {
        checkParams(userId, projectId, pipelineIds)
        return Result(
            data = pipelineListFacadeService.getBatchPipelinesWithModel(
                userId = userId,
                projectId = projectId,
                pipelineIds = pipelineIds,
                channelCode = channelCode,
                checkPermission = false
            )
        )
    }

    override fun saveSetting(
        userId: String,
        projectId: String,
        pipelineId: String,
        updateLastModifyUser: Boolean?,
        channelCode: ChannelCode?,
        setting: PipelineSetting
    ): Result<Boolean> {
        checkProjectId(projectId)
        checkPipelineId(pipelineId)
        pipelineSettingFacadeService.saveSetting(
            userId = userId,
            setting = setting,
            checkPermission = ChannelCode.isNeedAuth(channelCode ?: ChannelCode.BS),
            updateLastModifyUser = updateLastModifyUser
        )
        return Result(true)
    }

    override fun getPipelineInfo(
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode?
    ): Result<PipelineInfo?> {
        checkProjectId(projectId)
        return Result(pipelineRepositoryService.getPipelineInfo(projectId, pipelineId))
    }

    override fun delete(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode
    ): Result<Boolean> {
        checkParams(userId, projectId)
        pipelineInfoFacadeService.deletePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = channelCode,
            checkPermission = ChannelCode.isNeedAuth(channelCode)
        )
        return Result(true)
    }

    override fun list(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode?,
        checkPermission: Boolean?
    ): Result<Page<Pipeline>> {
        checkUserId(userId)
        checkProjectId(projectId)
        val result = pipelineListFacadeService.listPermissionPipeline(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            sortType = PipelineSortType.CREATE_TIME,
            channelCode = channelCode ?: ChannelCode.BS,
            checkPermission = false
        )
        return Result(Page(result.page, result.pageSize, result.count, result.records))
    }

    override fun count(projectId: Set<String>?, channelCode: ChannelCode?): Result<Long> {
        val data = pipelineListFacadeService.count(projectId ?: setOf(), channelCode)
        return Result(data = data.toLong())
    }

    override fun isPipelineRunning(projectId: String, buildId: String, channelCode: ChannelCode): Result<Boolean> {
        checkProjectId(projectId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(pipelineListFacadeService.isPipelineRunning(projectId, buildId, channelCode))
    }

    override fun isRunning(projectId: String, buildId: String, channelCode: ChannelCode): Result<Boolean> {
        checkProjectId(projectId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(pipelineListFacadeService.isRunning(projectId, buildId, channelCode))
    }

    override fun getPipelineByIds(projectId: String, pipelineIds: Set<String>): Result<List<SimplePipeline>> {
        return Result(pipelineListFacadeService.getPipelineByIds(projectId = projectId, pipelineIds = pipelineIds))
    }

    override fun getPipelineNameByIds(projectId: String, pipelineIds: Set<String>): Result<Map<String, String>> {
        return Result(pipelineListFacadeService.getPipelineNameByIds(projectId, pipelineIds))
    }

    override fun getBuildNoByBuildIds(buildIds: Set<String>, projectId: String?): Result<Map<String, String>> {
        return Result(pipelineListFacadeService.getBuildNoByByPair(buildIds, projectId))
    }

    override fun getAllstatus(userId: String, projectId: String, pipelineId: String): Result<List<Pipeline>?> {
        return Result(
            pipelineListFacadeService.getPipelineAllStatus(
                userId = userId,
                projectId = projectId,
                pipeline = pipelineId
            )
        )
    }

    override fun rename(userId: String, projectId: String, pipelineId: String, name: PipelineName): Result<Boolean> {
        checkParams(userId, projectId)
        pipelineInfoFacadeService.renamePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            name = name.name,
            channelCode = ChannelCode.BS
        )
        return Result(true)
    }

    override fun restore(userId: String, projectId: String, pipelineId: String): Result<Boolean> {
        checkParams(userId, projectId)
        pipelineInfoFacadeService.restorePipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.BS
        )
        return Result(true)
    }

    override fun getProjectPipelineIds(projectCode: String): Result<List<PipelineIdInfo>> {
        return Result(pipelineListFacadeService.getProjectPipelineId(projectCode))
    }

    override fun getPipelineId(projectCode: String, pipelineId: String): Result<PipelineIdInfo?> {
        return Result(pipelineListFacadeService.getPipelineId(projectCode, pipelineId))
    }

    override fun getPipelineInfoByPipelineId(pipelineId: String): Result<SimplePipeline?>? {
        val pipelineInfos = pipelineListFacadeService.getByPipelineIds(setOf(pipelineId))
        if (pipelineInfos.isNotEmpty()) {
            return Result(pipelineInfos[0])
        }
        return null
    }

    override fun batchUpdatePipelineNamePinYin(userId: String): Result<Boolean> {
        pipelineInfoFacadeService.batchUpdatePipelineNamePinYin(userId)
        return Result(true)
    }

    override fun getPipelineLabelInfos(
        userId: String,
        projectIds: List<String>
    ): Result<List<PipelineLabelRelateInfo>> {
        return Result(
            pipelineListFacadeService.getProjectPipelineLabelInfos(projectIds)
        )
    }

    override fun searchByName(
        userId: String,
        projectId: String,
        pipelineName: String?
    ): Result<List<PipelineIdAndName>> {
        checkParam(userId, projectId)
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                permission = AuthPermission.VIEW
            )
        ) {
            throw PermissionForbiddenException("$userId 无项目$projectId 查看权限")
        }
        val pipelineInfos = pipelineListFacadeService.searchIdAndName(
            projectId = projectId,
            pipelineName = pipelineName,
            page = null,
            pageSize = null
        )
        return Result(pipelineInfos)
    }

    override fun batchUpdateModelName(modelUpdateList: List<ModelUpdate>): Result<List<ModelUpdate>> {
        return Result(pipelineInfoFacadeService.batchUpdateModelName(modelUpdateList))
    }

    private fun checkParams(userId: String, projectId: String) {
        checkUserId(userId)
        checkProjectId(projectId)
    }

    private fun checkParams(userId: String, projectId: String, pipelineIds: List<String>) {
        checkUserId(userId)
        checkProjectId(projectId)
        checkPipelineIds(pipelineIds)
    }

    private fun checkUserId(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }

    private fun checkProjectId(projectId: String) {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    private fun checkPipelineId(pipelineId: String) {
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    @Suppress("ALL")
    private fun checkPipelineIds(pipelineIds: List<String>) {
        if (pipelineIds.isEmpty()) {
            throw ParamBlankException("Invalid projectId list")
        }
        if (pipelineIds.size > 100) {
            throw InvalidParamException("Number of pipelines is too large, size:${pipelineIds.size}")
        }
    }
}
