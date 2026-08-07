package com.tencent.devops.process.service.creative

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.dao.creative.CreativeFlowShareGrant
import com.tencent.devops.process.engine.dao.creative.PipelineShareGrantDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.enums.CreativeFlowShareGrantStatus
import com.tencent.devops.process.enums.CreativeFlowShareMode
import com.tencent.devops.process.enums.CreativeFlowShareVersionScope
import com.tencent.devops.process.pojo.creative.CreativeFlowShareExtInfo
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantCondition
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantFailure
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantUpsertRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantUpsertResult
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantVo
import com.tencent.devops.process.pojo.creative.CreativeFlowShareGrantRevokeRequest
import com.tencent.devops.process.pojo.creative.CreativeFlowShareValidateRules
import com.tencent.devops.process.utils.CreativeFlowVersionNumUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.enums.ProjectScopeType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreativeFlowShareGrantService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineShareGrantDao: PipelineShareGrantDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val creativeFlowEnvValidator: CreativeFlowEnvValidator
) {

    companion object {
        private val logger = LoggerFactory.getLogger(CreativeFlowShareGrantService::class.java)
    }

    /**
     * 逐条独立提交，单条失败不影响其他条目（spec §7.1「不阻塞上架」）。
     * 没有"差集撤销"这一步：manifest 移除条目不影响已发出的授权。
     */
    fun upsertGrants(
        userId: String,
        request: CreativeFlowShareGrantUpsertRequest
    ): CreativeFlowShareGrantUpsertResult {
        val granted = mutableListOf<CreativeFlowShareGrantVo>()
        val failed = mutableListOf<CreativeFlowShareGrantFailure>()

        for (item in request.flows) {
            try {
                // 1. 校验源流水线存在且 channelCode == CREATIVE_STREAM
                val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
                    projectId = item.sourceProjectId,
                    pipelineId = item.sourcePipelineId
                ) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_SHARE_SOURCE_NOT_EXISTS,
                    params = arrayOf(item.sourceProjectId, item.sourcePipelineId)
                )
                if (pipelineInfo.channelCode != ChannelCode.CREATIVE_STREAM) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_SHARE_CHANNEL_INVALID,
                        params = arrayOf(item.sourcePipelineId)
                    )
                }

                // 2. 校验源项目组织形态：非 PERSONAL 直接拒绝
                //    这里就是未来接团队形态的埋点，改动只需把 throw 换成按形态分流（见设计 §14）
                val projectResult = client.get(ServiceProjectResource::class).get(item.sourceProjectId)
                val projectVo = projectResult.data ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_SHARE_SOURCE_NOT_EXISTS,
                    params = arrayOf(item.sourceProjectId, item.sourcePipelineId)
                )
                val scopeType = ProjectScopeType.fromValue(projectVo.projectScope)
                if (scopeType != ProjectScopeType.PERSONAL) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_SHARE_TEAM_PROJECT_NOT_SUPPORT,
                        params = arrayOf(item.sourceProjectId)
                    )
                }

                // 3. 不做权限校验（§0）：只记录 GRANTED_BY

                // 4. 版本解析
                val versionScope: CreativeFlowShareVersionScope
                val version: Int?
                val versionNum: Int?
                if (item.versionNum != null) {
                    val parsedNum = CreativeFlowVersionNumUtil.parse(item.versionNum)
                    val versionSimple = pipelineResourceVersionDao.getReleasedVersionByVersionNum(
                        dslContext = dslContext,
                        projectId = item.sourceProjectId,
                        pipelineId = item.sourcePipelineId,
                        versionNum = parsedNum
                    ) ?: throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_VERSION_NUM_NOT_FOUND,
                        params = arrayOf(item.sourceProjectId, item.sourcePipelineId, item.versionNum)
                    )
                    versionScope = CreativeFlowShareVersionScope.PINNED
                    version = versionSimple.version
                    versionNum = parsedNum
                } else {
                    versionScope = CreativeFlowShareVersionScope.LATEST
                    version = null
                    versionNum = null
                }

                // 5. 采集 VALIDATE_RULES：读源 envHashId → OS
                val validateRules = try {
                    val setting = pipelineRepositoryService.getSetting(item.sourceProjectId, item.sourcePipelineId)
                    val envHashId = setting?.envHashId
                    if (!envHashId.isNullOrBlank()) {
                        val osType = creativeFlowEnvValidator.getEnvOsType(userId, item.sourceProjectId, envHashId)
                        CreativeFlowShareValidateRules(envOsType = osType)
                    } else null
                } catch (e: Exception) {
                    logger.warn("CreativeFlowShareGrantService|upsertGrants|" +
                        "failed to collect validateRules for ${item.sourceProjectId}/${item.sourcePipelineId}", e)
                    null
                }

                // 6. upsert 落库
                val grant = CreativeFlowShareGrant(
                    shareId = request.shareId,
                    flowId = item.flowId,
                    scene = request.scene,
                    shareMode = CreativeFlowShareMode.COPY,
                    sourceProjectId = item.sourceProjectId,
                    sourcePipelineId = item.sourcePipelineId,
                    versionScope = versionScope,
                    version = version,
                    versionNum = versionNum,
                    validateRulesJson = validateRules?.let { JsonUtil.toJson(it) },
                    extInfoJson = item.extInfo?.let { JsonUtil.toJson(it) },
                    talentCode = request.talentCode,
                    status = CreativeFlowShareGrantStatus.ENABLED,
                    grantedBy = userId,
                    grantedTime = System.currentTimeMillis()
                )
                pipelineShareGrantDao.upsert(dslContext, grant)

                granted.add(toVo(pipelineShareGrantDao.get(dslContext, request.shareId, item.flowId)!!))
            } catch (e: ErrorCodeException) {
                logger.warn("CreativeFlowShareGrantService|upsertGrants|item failed: ${item.flowId}", e)
                failed.add(CreativeFlowShareGrantFailure(
                    flowId = item.flowId,
                    errorCode = e.errorCode,
                    message = e.defaultMessage ?: e.errorCode
                ))
            } catch (e: Exception) {
                logger.error("CreativeFlowShareGrantService|upsertGrants|item error: ${item.flowId}", e)
                failed.add(CreativeFlowShareGrantFailure(
                    flowId = item.flowId,
                    errorCode = "UNKNOWN",
                    message = e.message ?: "Unknown error"
                ))
            }
        }
        return CreativeFlowShareGrantUpsertResult(granted = granted, failed = failed)
    }

    fun listGrants(
        userId: String,
        condition: CreativeFlowShareGrantCondition
    ): List<CreativeFlowShareGrantVo> {
        return pipelineShareGrantDao.list(
            dslContext = dslContext,
            shareId = condition.shareId,
            flowId = condition.flowId,
            talentCode = condition.talentCode,
            sourceProjectId = condition.sourceProjectId,
            sourcePipelineId = condition.sourcePipelineId,
            includeRevoked = condition.includeRevoked
        ).map { toVo(it) }
    }

    fun revokeGrants(userId: String, request: CreativeFlowShareGrantRevokeRequest): Int {
        return when {
            request.talentCode != null -> {
                pipelineShareGrantDao.revokeByTalentCode(dslContext, request.talentCode, userId)
            }
            request.shareId != null && !request.flowIds.isNullOrEmpty() -> {
                pipelineShareGrantDao.revoke(dslContext, request.shareId, request.flowIds, userId)
            }
            else -> throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_SHARE_REVOKE_PARAM_INVALID,
                defaultMessage = "shareId+flowIds or talentCode is required"
            )
        }
    }

    fun getEnabledGrant(shareId: String, flowId: String): CreativeFlowShareGrant {
        val grant = pipelineShareGrantDao.get(dslContext, shareId, flowId)
        if (grant == null || grant.status != CreativeFlowShareGrantStatus.ENABLED) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_SHARE_GRANT_NOT_EXISTS,
                params = arrayOf(shareId, flowId)
            )
        }
        return grant
    }

    private fun toVo(grant: CreativeFlowShareGrant): CreativeFlowShareGrantVo {
        return CreativeFlowShareGrantVo(
            shareId = grant.shareId,
            flowId = grant.flowId,
            scene = grant.scene,
            shareMode = grant.shareMode,
            sourceProjectId = grant.sourceProjectId,
            sourcePipelineId = grant.sourcePipelineId,
            versionScope = grant.versionScope,
            versionNum = grant.versionNum?.let { CreativeFlowVersionNumUtil.format(it) },
            validateRules = grant.validateRulesJson?.let {
                try { JsonUtil.to(it, CreativeFlowShareValidateRules::class.java) } catch (_: Exception) { null }
            },
            status = grant.status,
            talentCode = grant.talentCode,
            grantedBy = grant.grantedBy,
            grantedTime = grant.grantedTime,
            extInfo = grant.extInfoJson?.let {
                try { JsonUtil.to(it, CreativeFlowShareExtInfo::class.java) } catch (_: Exception) { null }
            }
        )
    }
}
