package com.tencent.devops.process.service.builds

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineBuildParamCombinationDao
import com.tencent.devops.process.engine.dao.PipelineBuildParamCombinationDetailDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.BuildParamCombination
import com.tencent.devops.process.pojo.pipeline.BuildParamCombinationReq
import com.tencent.devops.process.service.ParamFacadeService
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineBuildParamCombinationService @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val dslContext: DSLContext,
    private val pipelineBuildParamCombinationDao: PipelineBuildParamCombinationDao,
    private val pipelineBuildParamCombinationDetailDao: PipelineBuildParamCombinationDetailDao,
    private val client: Client,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val paramFacadeService: ParamFacadeService
) {
    fun saveCombination(
        userId: String,
        projectId: String,
        pipelineId: String,
        request: BuildParamCombinationReq
    ): Long {
        logger.info("save build param combination|$userId|$projectId|$pipelineId|${request.name}")
        validatePermission(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId
        )
        val combinationId =
            client.get(ServiceAllocIdResource::class).generateSegmentId(PIPELINE_BUILD_PARAM_COMBINATION).data ?: 0
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineBuildParamCombinationDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                combinationId = combinationId,
                combinationName = request.name
            )
            pipelineBuildParamCombinationDetailDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                combinationId = combinationId,
                combinationName = request.name,
                params = PipelineUtils.cleanOptions(request.params)
            )
        }
        return combinationId
    }

    fun editCombination(
        userId: String,
        projectId: String,
        pipelineId: String,
        combinationId: Long,
        request: BuildParamCombinationReq
    ) {
        logger.info("edit build param combination|$userId|$projectId|$pipelineId|${request.name}")
        validatePermission(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId
        )
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineBuildParamCombinationDao.update(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                combinationId = combinationId,
                combinationName = request.name
            )
            pipelineBuildParamCombinationDetailDao.delete(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                combinationId = combinationId,
            )
            pipelineBuildParamCombinationDetailDao.save(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                combinationId = combinationId,
                combinationName = request.name,
                params = PipelineUtils.cleanOptions(request.params)
            )
        }
    }

    fun getCombination(
        userId: String,
        projectId: String,
        pipelineId: String,
        combinationId: Long
    ): List<BuildFormProperty> {
        validatePermission(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId
        )
        val params = pipelineBuildParamCombinationDetailDao.getParams(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            combinationId = combinationId
        )
        return paramFacadeService.filterParams(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            params = params
        )
    }

    fun deleteCombination(
        userId: String,
        projectId: String,
        pipelineId: String,
        combinationId: Long
    ) {
        validatePermission(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId
        )
        val buildParamCombinationRecord = pipelineBuildParamCombinationDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            combinationId = combinationId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_BUILD_PARAM_COMBINATION_NOT_FOUND
        )
        logger.info(
            "delete build param combination|$userId|$projectId|$pipelineId|$combinationId|" +
                    "${buildParamCombinationRecord.combinationName}"
        )
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineBuildParamCombinationDao.delete(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                combinationId = combinationId
            )
            pipelineBuildParamCombinationDetailDao.delete(
                dslContext = transactionContext,
                projectId = projectId,
                pipelineId = pipelineId,
                combinationId = combinationId
            )
        }
    }

    fun listCombination(
        userId: String,
        projectId: String,
        pipelineId: String,
        combinationName: String?,
        varName: String?,
        page: Int?,
        pageSize: Int?
    ): SQLPage<BuildParamCombination> {
        validatePermission(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId
        )
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val count = pipelineBuildParamCombinationDao.countCombination(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            combinationName = combinationName,
            varName = varName
        )
        val records = pipelineBuildParamCombinationDao.listCombination(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            combinationName = combinationName,
            varName = varName,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        ).map { record ->
            BuildParamCombination(
                id = record.id,
                name = record.combinationName
            )
        }
        return SQLPage(count = count, records = records)
    }

    fun getCombinationFromBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<BuildFormProperty> {
        validatePermission(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId
        )
        // 1. 获取流水线编排的启动参数
        val buildInfo = pipelineRuntimeService.getBuildInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf(buildId)
        )
        val resource = pipelineRepositoryService.getBuildTriggerInfo(
            projectId = projectId, pipelineId = pipelineId, version = buildInfo.version
        ).second
        val triggerContainer = resource.model.getTriggerContainer()
        val resourceParams = paramFacadeService.filterParams(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            params = triggerContainer.params
        )
        // 2. 获取触发时的参数
        val buildParamMap = pipelineRuntimeService.getBuildParametersFromStartup(
            projectId = projectId,
            buildId = buildId
        ).associateBy { it.key }

        // 3. 合并参数
        val params = mutableListOf<BuildFormProperty>()
        resourceParams.forEach { rParams ->
            val param = buildParamMap[rParams.id]?.let {
                rParams.copy(defaultValue = it.value)
            } ?: rParams
            params.add(param)
        }
        return params
    }

    private fun validatePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
    ) {
        val permission = AuthPermission.EXECUTE
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId, projectId, permission.getI18n(I18nUtil.getLanguage(userId)), pipelineId
                )
            )
        )
    }

    companion object {
        private const val PIPELINE_BUILD_PARAM_COMBINATION = "PIPELINE_BUILD_PARAM_COMBINATION"
        private val logger = LoggerFactory.getLogger(PipelineBuildParamCombinationService::class.java)
    }
}
