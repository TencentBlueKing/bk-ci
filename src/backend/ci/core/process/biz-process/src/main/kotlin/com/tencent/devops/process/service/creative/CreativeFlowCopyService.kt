package com.tencent.devops.process.service.creative

import com.tencent.devops.common.api.context.ChannelContext
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.dao.creative.CreativeFlowCopyTrace
import com.tencent.devops.process.engine.dao.creative.CreativeFlowShareGrant
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.enums.CreativeFlowCopyConflictPolicy
import com.tencent.devops.process.enums.CreativeFlowCopyStatus
import com.tencent.devops.process.enums.CreativeFlowShareVersionScope
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyResult
import com.tencent.devops.process.pojo.creative.CreativeFlowShareExtInfo
import com.tencent.devops.process.pojo.creative.CreativeFlowShareValidateRules
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.PipelineOperationLogService
import com.tencent.devops.process.utils.CreativeFlowVersionNumUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList")
class CreativeFlowCopyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val grantService: CreativeFlowShareGrantService,
    private val envValidator: CreativeFlowEnvValidator,
    private val variableOverrideService: CreativeFlowVariableOverrideService,
    private val traceService: CreativeFlowCopyTraceService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val operationLogService: PipelineOperationLogService,
    private val modelCheckPlugin: ModelCheckPlugin
) {

    companion object {
        private val logger = LoggerFactory.getLogger(CreativeFlowCopyService::class.java)
        private const val TARGET_NAME_MAX_LENGTH = 128
    }

    fun copyAcrossProject(
        userId: String,
        targetProjectId: String,
        request: CreativeFlowCopyRequest
    ): CreativeFlowCopyResult {
        if (request.copyDependencies) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_COPY_DEPENDENCIES_NOT_SUPPORT
            )
        }

        return ChannelContext.withChannel(ChannelCode.CREATIVE_STREAM.name) {
            // 1. 授权：唯一凭据，源信息只从 grant 取
            val grant = grantService.getEnabledGrant(request.shareId, request.flowId)

            // OVERWRITE 必须显式带已登记的 targetPipelineId（spec §8 红线）
            if (request.conflictPolicy == CreativeFlowCopyConflictPolicy.OVERWRITE &&
                request.targetPipelineId.isNullOrBlank()
            ) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_COPY_TARGET_NOT_TRACED,
                    params = arrayOf("")
                )
            }

            // 2. 环境 OS 校验
            val validateRules = grant.validateRulesJson?.let {
                try {
                    JsonUtil.to(it, CreativeFlowShareValidateRules::class.java)
                } catch (_: Exception) { null }
            }
            envValidator.validate(userId, targetProjectId, request.targetEnvHashId, validateRules)

            // 3. 幂等：锁内查溯源
            val lockKey = "lock:creative:flow:copy:$targetProjectId:${request.shareId}:${request.flowId}"
            val lock = RedisLock(redisOperation, lockKey, 60)
            lock.lock()
            try {
                doCopy(userId, targetProjectId, request, grant)
            } finally {
                lock.unlock()
            }
        }
    }

    private fun doCopy(
        userId: String,
        targetProjectId: String,
        request: CreativeFlowCopyRequest,
        grant: CreativeFlowShareGrant
    ): CreativeFlowCopyResult {
        val traced = traceService.getLatestAlive(targetProjectId, request.shareId, request.flowId)
        if (traced != null) {
            when (request.conflictPolicy) {
                CreativeFlowCopyConflictPolicy.SKIP -> {
                    return CreativeFlowCopyResult(
                        status = CreativeFlowCopyStatus.SKIPPED,
                        targetPipelineId = traced.targetPipelineId,
                        skippedReason = "副本已存在: ${traced.targetPipelineId}"
                    )
                }
                CreativeFlowCopyConflictPolicy.FAIL -> {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_COPY_TARGET_NAME_CONFLICT,
                        params = arrayOf(targetProjectId, traced.targetPipelineName)
                    )
                }
                CreativeFlowCopyConflictPolicy.OVERWRITE -> {
                    if (request.targetPipelineId != traced.targetPipelineId) {
                        throw ErrorCodeException(
                            errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_COPY_TARGET_NOT_TRACED,
                            params = arrayOf(request.targetPipelineId ?: "")
                        )
                    }
                }
            }
        }

        // 同人克隆自己上架的分身：目标个人项目 == 源项目，且源 pipeline 仍在 →
        // 按同一 pipelineId 复用，禁止再 create 出第二条流。
        if (traced == null && grant.sourceProjectId == targetProjectId) {
            val sourceInfo = pipelineRepositoryService.getPipelineInfo(
                projectId = grant.sourceProjectId,
                pipelineId = grant.sourcePipelineId,
                channelCode = ChannelCode.CREATIVE_STREAM
            )
            if (sourceInfo != null) {
                return reuseSameProjectSource(
                    userId = userId,
                    targetProjectId = targetProjectId,
                    request = request,
                    grant = grant,
                    sourcePipelineName = sourceInfo.pipelineName,
                    sourcePipelineVersion = sourceInfo.version
                )
            }
        }

        // 4. 版本解析
        val sourceVersion = resolveSourceVersion(grant)

        // 5. 读源编排（以系统身份读，聘用者对源项目无需任何权限）
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = grant.sourceProjectId,
            pipelineId = grant.sourcePipelineId,
            version = sourceVersion.first
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_SHARE_SOURCE_NOT_EXISTS,
            params = arrayOf(grant.sourceProjectId, grant.sourcePipelineId)
        )

        val sourceSetting = pipelineRepositoryService.getSettingByPipelineVersion(
            projectId = grant.sourceProjectId,
            pipelineId = grant.sourcePipelineId,
            pipelineVersion = sourceVersion.first
        )

        // 6. 变量覆盖
        var model = resource.model
        model = variableOverrideService.applyOverrides(model, request.variableOverrides)
        modelCheckPlugin.clearUpModel(model)

        // 7. 目标名
        val extInfo = grant.extInfoJson?.let {
            try { JsonUtil.to(it, CreativeFlowShareExtInfo::class.java) } catch (_: Exception) { null }
        }
        val targetName = resolveTargetName(
            requestName = request.targetPipelineName,
            extInfo = extInfo,
            sourceName = sourceSetting?.pipelineName ?: ""
        )

        // 8. 落地
        val isOverwrite = traced != null && request.conflictPolicy == CreativeFlowCopyConflictPolicy.OVERWRITE
        val deployResult = try {
            if (!isOverwrite) {
                createWithNameConflictRetry(
                    userId = userId,
                    targetProjectId = targetProjectId,
                    model = model,
                    sourceSetting = sourceSetting,
                    targetName = targetName,
                    targetEnvHashId = request.targetEnvHashId,
                    talentCode = grant.talentCode,
                    shareId = grant.shareId
                )
            } else {
                pipelineInfoFacadeService.editPipeline(
                    userId = userId,
                    projectId = targetProjectId,
                    pipelineId = traced!!.targetPipelineId,
                    model = model,
                    yaml = null,
                    channelCode = ChannelCode.CREATIVE_STREAM,
                    checkPermission = true,
                    savedSetting = sourceSetting?.copy(
                        projectId = targetProjectId,
                        pipelineName = targetName,
                        envHashId = request.targetEnvHashId
                    ),
                    versionStatus = VersionStatus.RELEASED
                )
            }
        } catch (e: ErrorCodeException) {
            if (e.errorCode == ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_COPY_TARGET_NAME_CONFLICT,
                    params = arrayOf(targetProjectId, targetName)
                )
            }
            throw e
        }

        val finalTargetName = deployResult.pipelineName
        val copyAction = if (isOverwrite) CreativeFlowCopyStatus.OVERWRITTEN else CreativeFlowCopyStatus.CREATED

        // 9. 溯源
        val variableOverridesSnapshot = request.variableOverrides?.let {
            val sanitized = it.mapValues { (_, v) -> if (v.length > 100) "***" else v }
            JsonUtil.toJson(sanitized)
        }

        traceService.record(
            CreativeFlowCopyTrace(
                shareId = grant.shareId,
                flowId = grant.flowId,
                scene = grant.scene,
                shareMode = grant.shareMode,
                talentCode = grant.talentCode,
                sourceProjectId = grant.sourceProjectId,
                sourcePipelineId = grant.sourcePipelineId,
                sourceVersion = sourceVersion.first,
                sourceVersionNum = sourceVersion.second,
                targetProjectId = targetProjectId,
                targetPipelineId = deployResult.pipelineId,
                targetPipelineName = finalTargetName,
                targetVersion = deployResult.version,
                targetVersionNum = deployResult.versionNum,
                targetEnvHashId = request.targetEnvHashId,
                copyAction = copyAction,
                variableOverrides = variableOverridesSnapshot,
                operator = userId
            )
        )

        // 10. 操作日志
        val sourceVersionNumStr = sourceVersion.second?.let { CreativeFlowVersionNumUtil.format(it) } ?: "LATEST"
        val paramsStr = buildOperationLogParams(
            grant.sourceProjectId, grant.sourcePipelineId,
            sourceVersionNumStr, grant.shareId, grant.flowId
        )
        operationLogService.addOperationLog(
            userId = userId,
            projectId = targetProjectId,
            pipelineId = deployResult.pipelineId,
            version = deployResult.version,
            operationLogType = OperationLogType.CREATIVE_FLOW_SHARE_COPY,
            params = paramsStr,
            description = null
        )

        return CreativeFlowCopyResult(
            status = copyAction,
            targetPipelineId = deployResult.pipelineId,
            targetPipelineName = finalTargetName,
            targetVersion = deployResult.version,
            targetVersionNum = deployResult.versionNum?.let { CreativeFlowVersionNumUtil.format(it) },
            resolvedSourceVersion = sourceVersion.first,
            resolvedSourceVersionNum = sourceVersion.second?.let { CreativeFlowVersionNumUtil.format(it) }
        )
    }

    /**
     * 源项目 == 目标项目且源创作流仍存在时：复用同一 pipelineId，登记一条 SKIPPED 溯源，避免再次 create。
     * 同人克隆自己上架的分身走此路径；OVERWRITE 也不改写源流本身（避免把自己的正式流当「副本」覆盖）。
     */
    private fun reuseSameProjectSource(
        userId: String,
        targetProjectId: String,
        request: CreativeFlowCopyRequest,
        grant: CreativeFlowShareGrant,
        sourcePipelineName: String,
        sourcePipelineVersion: Int
    ): CreativeFlowCopyResult {
        val sourceVersion = resolveSourceVersion(grant)
        val skippedReason =
            "源与目标为同一项目下的同一创作流(${grant.sourcePipelineId})，复用不复制"
        logger.info(
            "CreativeFlowCopyService|reuse same project source|" +
                "$targetProjectId|${grant.sourcePipelineId}|shareId=${grant.shareId}|flowId=${grant.flowId}"
        )
        traceService.record(
            CreativeFlowCopyTrace(
                shareId = grant.shareId,
                flowId = grant.flowId,
                scene = grant.scene,
                shareMode = grant.shareMode,
                talentCode = grant.talentCode,
                sourceProjectId = grant.sourceProjectId,
                sourcePipelineId = grant.sourcePipelineId,
                sourceVersion = sourceVersion.first,
                sourceVersionNum = sourceVersion.second,
                targetProjectId = targetProjectId,
                targetPipelineId = grant.sourcePipelineId,
                targetPipelineName = sourcePipelineName,
                targetVersion = sourcePipelineVersion,
                targetVersionNum = sourceVersion.second,
                targetEnvHashId = request.targetEnvHashId,
                copyAction = CreativeFlowCopyStatus.SKIPPED,
                variableOverrides = null,
                operator = userId
            )
        )
        return CreativeFlowCopyResult(
            status = CreativeFlowCopyStatus.SKIPPED,
            targetPipelineId = grant.sourcePipelineId,
            targetPipelineName = sourcePipelineName,
            targetVersion = sourcePipelineVersion,
            targetVersionNum = sourceVersion.second?.let { CreativeFlowVersionNumUtil.format(it) },
            resolvedSourceVersion = sourceVersion.first,
            resolvedSourceVersionNum = sourceVersion.second?.let { CreativeFlowVersionNumUtil.format(it) },
            skippedReason = skippedReason
        )
    }

    /**
     * 优先用原名创建；重名时加 _{talentCode|shareId前8位} 后缀再试一次。
     */
    private fun createWithNameConflictRetry(
        userId: String,
        targetProjectId: String,
        model: com.tencent.devops.common.pipeline.Model,
        sourceSetting: com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting?,
        targetName: String,
        targetEnvHashId: String,
        talentCode: String?,
        shareId: String
    ): com.tencent.devops.process.pojo.pipeline.DeployPipelineResult {
        return try {
            pipelineInfoFacadeService.createPipeline(
                userId = userId,
                projectId = targetProjectId,
                model = model.copy(name = targetName),
                channelCode = ChannelCode.CREATIVE_STREAM,
                setting = sourceSetting?.copy(
                    projectId = targetProjectId,
                    pipelineName = targetName,
                    envHashId = targetEnvHashId
                ),
                checkPermission = true,
                versionStatus = VersionStatus.RELEASED
            )
        } catch (e: ErrorCodeException) {
            if (e.errorCode != ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS) throw e
            val suffix = "_${talentCode ?: shareId.take(8)}"
            val maxBase = TARGET_NAME_MAX_LENGTH - suffix.length
            val renamed = targetName.take(maxBase.coerceAtLeast(1)) + suffix
            logger.info(
                "CreativeFlowCopyService|name conflict, retry with suffix|" +
                    "$targetProjectId|$targetName -> $renamed"
            )
            pipelineInfoFacadeService.createPipeline(
                userId = userId,
                projectId = targetProjectId,
                model = model.copy(name = renamed),
                channelCode = ChannelCode.CREATIVE_STREAM,
                setting = sourceSetting?.copy(
                    projectId = targetProjectId,
                    pipelineName = renamed,
                    envHashId = targetEnvHashId
                ),
                checkPermission = true,
                versionStatus = VersionStatus.RELEASED
            )
        }
    }

    /**
     * LATEST 取最新已发布正式版本；PINNED 只能是 grant.version
     */
    private fun resolveSourceVersion(grant: CreativeFlowShareGrant): Pair<Int, Int?> {
        return when (grant.versionScope) {
            CreativeFlowShareVersionScope.PINNED -> {
                val version = grant.version ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_SHARE_SOURCE_NOT_EXISTS,
                    params = arrayOf(grant.sourceProjectId, grant.sourcePipelineId)
                )
                Pair(version, grant.versionNum)
            }
            CreativeFlowShareVersionScope.LATEST -> {
                val released = pipelineResourceVersionDao.getReleaseVersionRecord(
                    dslContext, grant.sourceProjectId, grant.sourcePipelineId
                ) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_SHARE_SOURCE_NOT_EXISTS,
                    params = arrayOf(grant.sourceProjectId, grant.sourcePipelineId)
                )
                Pair(released.version, released.versionNum)
            }
        }
    }

    private fun resolveTargetName(
        requestName: String?,
        extInfo: CreativeFlowShareExtInfo?,
        sourceName: String
    ): String {
        val baseName = requestName ?: extInfo?.flowName ?: extInfo?.sourcePipelineName ?: sourceName
        return if (baseName.length <= TARGET_NAME_MAX_LENGTH) {
            baseName
        } else {
            baseName.take(TARGET_NAME_MAX_LENGTH)
        }
    }

    private fun buildOperationLogParams(
        sourceProjectId: String,
        sourcePipelineId: String,
        versionNum: String,
        shareId: String,
        flowId: String
    ): String {
        val raw = "$sourceProjectId/$sourcePipelineId@$versionNum|$shareId#$flowId"
        return if (raw.length > 250) {
            val truncatedFlowId = flowId.take(250 - "$sourceProjectId/$sourcePipelineId@$versionNum|$shareId#".length)
            "$sourceProjectId/$sourcePipelineId@$versionNum|$shareId#$truncatedFlowId"
        } else raw
    }
}
