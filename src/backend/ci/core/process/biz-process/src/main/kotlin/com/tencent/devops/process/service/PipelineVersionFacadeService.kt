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

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.common.api.constant.NUM_ZERO
import com.tencent.devops.common.api.context.ChannelContext
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.PipelineVersionWithModel
import com.tencent.devops.common.pipeline.PipelineVersionWithModelRequest
import com.tencent.devops.common.pipeline.dialect.PipelineDialectType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.PublicVarGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceCreateRequest
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_IS_NOT_PAC
import com.tencent.devops.process.engine.pojo.PipelineVersionWithInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRepositoryVersionService
import com.tencent.devops.process.enums.PipelineGetVersionSource
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineDetail
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.pojo.PipelineVersionReleaseRequest
import com.tencent.devops.process.pojo.PipelineYamlBuildVersion
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineDraftStatusResult
import com.tencent.devops.process.pojo.pipeline.PipelineDraftVersionSimple
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.pipeline.PrefetchReleaseResult
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDraftActionType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDraftStatus
import com.tencent.devops.process.pojo.pipeline.version.PipelineDraftSaveReq
import com.tencent.devops.process.pojo.pipeline.version.PipelineRollbackReq
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.pipeline.PipelineTransferYamlService
import com.tencent.devops.process.service.pipeline.version.PipelineVersionManager
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.`var`.PublicVarGroupReferManageService
import com.tencent.devops.process.trigger.pac.PipelineYamlFacadeService
import com.tencent.devops.process.utils.PipelineVersionUtils
import com.tencent.devops.process.pac.PipelineYamlCommonService
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import jakarta.ws.rs.core.Response
import java.util.concurrent.TimeUnit
import org.slf4j.MDC
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineVersionFacadeService @Autowired constructor(
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val transferService: PipelineTransferYamlService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val repositoryVersionService: PipelineRepositoryVersionService,
    private val pipelineYamlFacadeService: PipelineYamlFacadeService,
    private val pipelineYamlCommonService: PipelineYamlCommonService,
    private val pipelineRecentUseService: PipelineRecentUseService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val scmProxyService: ScmProxyService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineVersionManager: PipelineVersionManager,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val publicVarGroupReferManageService: PublicVarGroupReferManageService,
    private val client: Client,
    private val authProjectApi: AuthProjectApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val clientTokenService: ClientTokenService,
    @Qualifier("publishNotifyExecutor")
    private val publishNotifyExecutor: ThreadPoolTaskExecutor,
    private val redisOperation: RedisOperation
) {

    companion object {
        /** 版本发布通知去重键前缀 */
        private const val PUBLISH_NOTIFY_KEY_PREFIX = "pipeline:publish:notify:"

        /** 通知去重键保留 24 小时，覆盖消息重投窗口 */
        @Suppress("MagicNumber")
        private const val PUBLISH_NOTIFY_TTL_SECONDS = 86400L

        private const val PAC_BRANCH_PREFIX = "bk-ci-pipeline-"
        private const val CREATIVE_STREAM_PREFIX = "creative-stream"
        private const val PIPELINE_OWNER_VAR = "ci.pipeline_owner"
        private val USER_SEPARATOR_REGEX = "[,;]".toRegex()
        fun getReleaseBranchName(pipelineId: String, version: Int): String =
            "$PAC_BRANCH_PREFIX$pipelineId-$version"
        private val logger = LoggerFactory.getLogger(PipelineVersionFacadeService::class.java)
    }

    /**
     * 流水线草稿编辑提醒阈值（单位：天）
     * 当草稿编辑天数超过该阈值时，会触发提醒
     */
    @Value("\${pipeline.draft.max_reminder_days:7}")
    private val maxReminderDays: Long = 7L

    /**
     * 通知邮件模板顶部 logo 图标地址，未配置时为空
     */
    @Value("\${bkci.logo.iconUrl:}")
    private val logoIconUrl: String = ""

    /**
     * 通知邮件模板顶部 logo 文字图片地址，未配置时为空
     */
    @Value("\${bkci.logo.textUrl:}")
    private val logoTextUrl: String = ""

    fun getPipelineDetailIncludeDraft(
        userId: String,
        projectId: String,
        pipelineId: String,
        archiveFlag: Boolean? = false
    ): PipelineDetail {
        val detailInfo = pipelineListFacadeService.getPipelineDetail(
            userId = userId, projectId = projectId, pipelineId = pipelineId, archiveFlag = archiveFlag
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID, params = arrayOf(pipelineId)
        )
        val draftResource = pipelineRepositoryService.getDraftVersionResource(
            projectId = projectId, pipelineId = pipelineId, archiveFlag = archiveFlag
        )
        // 有草稿且不是空白的编排才可以发布
        val canRelease = draftResource != null && draftResource.model.stages.size > 1
        // 存在草稿版本就可以调试
        val canDebug = draftResource != null
        val releaseResource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = detailInfo.pipelineVersion,
            archiveFlag = archiveFlag
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID,
            params = arrayOf(pipelineId)
        )
        val yamlInfo = if (archiveFlag != true) {
            pipelineYamlFacadeService.getPipelineYamlVo(projectId, pipelineId, releaseResource.version)
        } else {
            null
        }
        var baseVersion: Int? = null
        var baseVersionName: String? = null
        var baseVersionStatus: VersionStatus? = null
        draftResource?.let { draft ->
            val baseResource = draft.baseVersion?.let { base ->
                pipelineRepositoryService.getPipelineResourceVersion(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = base,
                    archiveFlag = archiveFlag
                )
            }
            baseResource?.let { baseVersion = it.version }
            baseResource?.status?.let { baseVersionStatus = it }
            baseResource?.versionName?.let { baseVersionName = it }
        }
        val releaseSetting = if (archiveFlag != true) {
            pipelineSettingFacadeService.userGetSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                detailInfo = detailInfo
            )
        } else {
            null
        }
        /**
         * 获取最新版本和版本名称
         *
         * 如果最新版本是分支版本,则需要获取分支最新的激活版本,否则最新版本可能是正式或者草稿版本
         */
        val (releaseVersion, releaseVersionName) = when (releaseResource.status) {
            // 分支版本,需要获取当前分支最新的激活版本
            VersionStatus.BRANCH -> {
                val branchVersion = pipelineRepositoryService.getBranchVersionResource(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    branchName = releaseResource.versionName,
                    archiveFlag = archiveFlag
                )
                Pair(branchVersion?.version ?: releaseResource.version, branchVersion?.versionName)
            }

            else -> {
                Pair(releaseResource.version, releaseResource.versionName)
            }
        }
        // 草稿版本和版本名,如果有草稿版本,则使用草稿版本,否则使用最新版本
        val (version, versionName, versionStatus) = if (draftResource == null) {
            Triple(releaseVersion, releaseVersionName, releaseResource.status)
        } else {
            Triple(draftResource.version, null, draftResource.status)
        }
        val permissions = pipelineListFacadeService.getPipelinePermissions(userId, projectId, pipelineId)
        val yamlExist = archiveFlag.takeUnless { it == true }?.run {
            pipelineRecentUseService.record(userId, projectId, pipelineId)
            pipelineYamlFacadeService.yamlExistInDefaultBranch(projectId, pipelineId)
        }
        // 获取当前最新版本的设置
        val pipelineSetting = pipelineRepositoryService.getSetting(
            projectId = projectId,
            pipelineId = pipelineId
        )
        return PipelineDetail(
            pipelineId = detailInfo.pipelineId,
            pipelineName = detailInfo.pipelineName,
            hasCollect = detailInfo.hasCollect,
            instanceFromTemplate = detailInfo.instanceFromTemplate,
            templateId = detailInfo.templateId,
            templateVersion = detailInfo.templateVersion,
            canManualStartup = detailInfo.canManualStartup,
            canDebug = canDebug,
            canRelease = canRelease,
            hasPermission = detailInfo.hasPermission,
            pipelineDesc = detailInfo.pipelineDesc,
            creator = detailInfo.creator,
            createTime = detailInfo.createTime,
            updateTime = detailInfo.updateTime,
            viewNames = detailInfo.viewNames,
            latestVersionStatus = detailInfo.latestVersionStatus,
            runLockType = releaseSetting?.runLockType,
            permissions = permissions,
            version = version,
            versionName = versionName,
            versionStatus = versionStatus,
            releaseVersion = releaseVersion,
            releaseVersionName = releaseVersionName,
            baseVersion = baseVersion,
            baseVersionStatus = baseVersionStatus,
            baseVersionName = baseVersionName,
            pipelineAsCodeSettings = PipelineAsCodeSettings(enable = yamlInfo != null),
            yamlInfo = yamlInfo,
            yamlExist = yamlExist,
            locked = detailInfo.locked,
            lockUser = detailInfo.lockUser,
            lockReason = detailInfo.lockReason,
            lockTime = detailInfo.lockTime,
            yamlLocked = detailInfo.yamlLocked,
            yamlLockUser = detailInfo.yamlLockUser,
            yamlLockTime = detailInfo.yamlLockTime,
            buildCancelPolicy = pipelineSetting?.buildCancelPolicy,
            draftVersion = draftResource?.draftVersion
        )
    }

    fun preFetchDraftVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        targetAction: CodeTargetAction?,
        repoHashId: String?,
        targetBranch: String?
    ): PrefetchReleaseResult {
        val draftVersion = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_DRAFT_EXISTS
        )
        val draftSetting = draftVersion.settingVersion?.let {
            pipelineSettingFacadeService.userGetSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = it
            )
        } ?: pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId
        )
        val releaseVersion = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId)
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID,
                params = arrayOf(pipelineId)
            )
        // 第一次发布,versionNum不需要+1
        val newVersionNum = if (draftVersion.version == releaseVersion.version) {
            releaseVersion.versionNum ?: releaseVersion.version
        } else {
            (releaseVersion.versionNum ?: releaseVersion.version) + 1
        }
        val prefetchVersionName = targetAction?.let {
            getVersionStatusAndName(
                projectId = projectId,
                pipelineId = pipelineId,
                draftVersion = draftVersion,
                targetAction = targetAction,
                repoHashId = repoHashId,
                targetBranch = targetBranch
            ).second
        } ?: PipelineVersionUtils.getVersionNameByModel(
            currPipelineVersion = releaseVersion.pipelineVersion ?: 1,
            currTriggerVersion = releaseVersion.triggerVersion ?: 1,
            settingVersion = draftSetting.version,
            versionNum = newVersionNum,
            originModel = releaseVersion.model,
            newModel = draftVersion.model
        )

        return PrefetchReleaseResult(
            pipelineId = pipelineId,
            pipelineName = draftVersion.model.name,
            version = draftVersion.version,
            newVersionNum = newVersionNum,
            newVersionName = prefetchVersionName
        )
    }

    fun releaseDraftVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        request: PipelineVersionReleaseRequest
    ): DeployPipelineResult {
        val result = pipelineVersionManager.deployPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            request = request
        )
        // 仅当本次发布版本配置了发布通知订阅时才提交通知
        if (result.publishSubscriptionConfigured == true) {
            sendPublishVersionNotify(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                result = result,
                description = request.description
            )
        }
        return result
    }

    /**
     * 发送版本发布通知。
     *
     * 供草稿发布（[releaseDraftVersion]）与 PAC 默认分支保存（`PipelineYamlResourceService`）共用。
     * 提交后立即返回，发送动作在独立线程池中异步执行；发送异常与线程池拒绝提交只记日志，不影响发布结果。
     *
     * @param result 发布结果，取其 pipelineName 与 version 定位发布版本的设置快照
     * @param description 版本描述，作为通知中的 `ci.pipeline_desc`
     */
    fun sendPublishVersionNotify(
        userId: String,
        projectId: String,
        pipelineId: String,
        result: DeployPipelineResult,
        description: String?
    ) {
        // 同一版本只通知一次，避免消息重投重复发送；去重判断异常时放行，不影响发布主流程
        val notifyKey = "$PUBLISH_NOTIFY_KEY_PREFIX$projectId:$pipelineId:${result.version}"
        val firstNotify = try {
            redisOperation.setIfAbsent(notifyKey, "1", PUBLISH_NOTIFY_TTL_SECONDS)
        } catch (ignored: Throwable) {
            logger.warn("check publish notify duplicated failed|$projectId|$pipelineId|${result.version}", ignored)
            true
        }
        if (!firstNotify) {
            logger.info("skip duplicated publish notify|$projectId|$pipelineId|${result.version}")
            return
        }
        // 通知为旁路动作，提交到独立线程池执行，避免阻塞发布接口与 PAC 的构建触发
        val channelCode = ChannelContext.getChannel()
        val traceId = MDC.get(TraceTag.BIZID)
        try {
            publishNotifyExecutor.execute {
                try {
                    ChannelContext.withChannel(channelCode) {
                        if (traceId != null) MDC.put(TraceTag.BIZID, traceId)
                        doSendPublishVersionNotify(userId, projectId, pipelineId, result, description)
                    }
                } catch (ignored: Throwable) {
                    logger.warn(
                        "send publish version notify failed|$projectId|$pipelineId|${result.version}",
                        ignored
                    )
                } finally {
                    MDC.remove(TraceTag.BIZID)
                }
            }
        } catch (ignored: Throwable) {
            logger.warn("submit publish version notify failed|$projectId|$pipelineId|${result.version}", ignored)
        }
    }

    private fun doSendPublishVersionNotify(
        userId: String,
        projectId: String,
        pipelineId: String,
        result: DeployPipelineResult,
        description: String?
    ) {
        try {
            // 按本次发布版本的设置版本读取订阅；版本无设置版本时（历史数据）回退当前正式设置
            val settingVersion = pipelineRepositoryService.getPipelineResourceVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                version = result.version
            )?.settingVersion ?: 0
            val setting = pipelineSettingFacadeService.userGetSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = settingVersion
            )
            val publishList = setting.publishSubscriptionList.orEmpty()
            if (publishList.isEmpty()) return
            // 创作流的通知链接与模板与流水线控制台不同，按渠道区分
            val channelCode = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)?.channelCode
                ?: ChannelCode.getRequestChannelCode()
            // 项目名查询失败只影响模板中的项目名展示，降级为项目 ID，不放弃本轮其它通知
            val projectName = try {
                client.get(ServiceProjectResource::class).get(projectId).data?.projectName ?: projectId
            } catch (e: Throwable) {
                logger.warn("query project name failed|$projectId", e)
                projectId
            }
            // ci.pipeline_owner：流水线资源级拥有者组（多个拥有者以逗号连接，parseReceiver 会展开为多个接收人）
            // 仅在订阅引用该变量时查询，未引用时为空列表
            val pipelineOwners = if (publishList.any { it.requiresPipelineOwners() }) {
                queryPipelineOwners(projectId, pipelineId)
            } else {
                emptyList()
            }
            val detailUrl = if (channelCode == ChannelCode.CREATIVE_STREAM) {
                "${HomeHostUtil.outerServerHost()}/console/$CREATIVE_STREAM_PREFIX/$projectId/flow/$pipelineId" +
                    "/detail/${result.version}"
            } else {
                "${HomeHostUtil.outerServerHost()}/console/pipeline/$projectId/$pipelineId" +
                    "/history/pipeline/${result.version}"
            }
            val params = mapOf(
                "projectName" to projectName,
                "pipelineName" to result.pipelineName,
                "versionName" to (result.versionName ?: result.version.toString()),
                "version" to result.version.toString(),
                "username" to userId,
                "description" to (description ?: ""),
                "logoIconUrl" to logoIconUrl,
                "logoTextUrl" to logoTextUrl,
                "detailUrl" to detailUrl,
                "ci.project_name" to projectName,
                "ci.pipeline_name" to result.pipelineName,
                "ci.pipeline_version" to (result.versionName ?: result.version.toString()),
                "ci.pipeline_modifier" to userId,
                // 发布通知沿用既有默认接收人 ${{ci.actor}} 时也能取到值
                "ci.actor" to userId,
                "ci.pipeline_desc" to (description ?: ""),
                PIPELINE_OWNER_VAR to pipelineOwners.joinToString(",")
            ).toMutableMap()
            // 角色组与订阅内容无关，只查一次，避免按订阅数重复请求
            val roleUsersMap = if (publishList.any { it.groups.isNotEmpty() }) {
                authProjectApi.getProjectGroupAndUserList(
                    serviceCode = pipelineAuthServiceCode,
                    projectCode = projectId
                ).associate { it.roleName to it.userIdList }
            } else {
                emptyMap()
            }
            publishList.forEach { subscription ->
                try {
                    // 接收人
                    val receivers = parseReceiver(subscription.users, params)
                    subscription.groups.forEach { groupName ->
                        receivers.addAll(roleUsersMap[groupName].orEmpty())
                    }
                    // 企微群 key 解析后放入 bodyParams（对齐 WEWORK_GROUP_KEY）
                    val group = EnvUtils.parseEnv(subscription.wechatGroup, params, true)
                    params[NotifyUtils.WEWORK_GROUP_KEY] = group
                    // 订阅自定义通知内容解析后放入 bodyParams，供模板 ${publishContent}/${emailPublishContent} 使用
                    val publishContent = EnvUtils.parseEnv(subscription.content, params, true)
                    params["publishContent"] = publishContent
                    params["emailPublishContent"] = publishContent
                    // 创作流默认使用 markdown，与构建通知的渠道约定一致
                    val notifyMarkdown = channelCode == ChannelCode.CREATIVE_STREAM
                    val notifyType = subscription.types.filter {
                        it != PipelineSubscriptionType.WEWORK_GROUP
                    }.map { it.name }.toMutableSet()
                    val templateCode = if (subscription.detailFlag) {
                        PipelineNotifyTemplateEnum.PIPELINE_PUBLISH_VERSION_NOTIFY_TEMPLATE_DETAIL.templateCode
                    } else {
                        PipelineNotifyTemplateEnum.PIPELINE_PUBLISH_VERSION_NOTIFY_TEMPLATE.templateCode
                    }
                    if (notifyType.isNotEmpty() && receivers.isEmpty()) {
                        logger.warn(
                            "publish notify skipped, empty receivers|$projectId|$pipelineId|${result.version}" +
                                "|notifyType=${notifyType.joinToString(",")}"
                        )
                    }
                    // 仅勾选企微群时 notifyType 为空集合，且无接收人时主渠道发送无意义，均跳过（企微群走下方单独发送）
                    if (notifyType.isNotEmpty() && receivers.isNotEmpty()) {
                        val sendResult = client.get(ServiceNotifyMessageTemplateResource::class)
                            .sendNotifyMessageByTemplate(
                                SendNotifyMessageTemplateRequest(
                                    templateCode = templateCode,
                                    receivers = receivers,
                                    notifyType = notifyType,
                                    titleParams = params,
                                    bodyParams = params,
                                    markdownContent = notifyMarkdown
                                )
                            )
                        logSendResult(
                            sendResult = sendResult,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            version = result.version,
                            templateCode = templateCode,
                            receiverCount = receivers.size
                        )
                    }
                    // 企业微信群单独发送
                    if (subscription.types.contains(PipelineSubscriptionType.WEWORK_GROUP) && group.isNotBlank()) {
                        val sendResult = client.get(ServiceNotifyMessageTemplateResource::class)
                            .sendNotifyMessageByTemplate(
                                SendNotifyMessageTemplateRequest(
                                    templateCode = templateCode,
                                    receivers = receivers,
                                    notifyType = mutableSetOf(PipelineSubscriptionType.WEWORK_GROUP.name),
                                    titleParams = params,
                                    bodyParams = params,
                                    markdownContent = notifyMarkdown || subscription.wechatGroupMarkdownFlag
                                )
                            )
                        logSendResult(
                            sendResult = sendResult,
                            projectId = projectId,
                            pipelineId = pipelineId,
                            version = result.version,
                            templateCode = templateCode,
                            receiverCount = receivers.size
                        )
                    }
                } catch (e: Throwable) {
                    logger.warn(
                        "send publish version subscription failed|$projectId|$pipelineId|${result.version}",
                        e
                    )
                }
            }
        } catch (ignored: Throwable) {
            logger.warn(
                "send publish version notify failed|$projectId|$pipelineId|${result.version}",
                ignored
            )
        }
    }

    /**
     * 查询流水线的资源级拥有者组。
     *
     * 资源类型按请求渠道确定，与发布权限校验同源；查询失败返回空列表，只影响依赖 owner 的订阅。
     */
    private fun queryPipelineOwners(projectId: String, pipelineId: String): List<String> {
        return try {
            client.get(ServiceResourceMemberResource::class).getResourceGroupMembers(
                token = clientTokenService.getSystemToken(),
                projectCode = projectId,
                resourceType = AuthResourceType.getAuthResourceTypeByChannel(
                    AuthResourceType.PIPELINE_DEFAULT
                ).value,
                resourceCode = pipelineId,
                group = BkAuthGroup.RESOURCE_MANAGER
            ).data.orEmpty()
        } catch (e: Throwable) {
            logger.warn("query pipeline owners failed|$projectId|$pipelineId", e)
            emptyList()
        }
    }

    /**
     * 订阅是否引用了 ci.pipeline_owner。
     *
     * 接收人、企业微信群、通知内容这三个字段会经 EnvUtils.parseEnv 展开，是 owner 变量的消费入口。
     */
    private fun Subscription.requiresPipelineOwners(): Boolean {
        return users.contains(PIPELINE_OWNER_VAR) ||
            wechatGroup.contains(PIPELINE_OWNER_VAR) ||
            content.contains(PIPELINE_OWNER_VAR)
    }

    /**
     * 记录发布通知的发送结果。发送失败只影响本条通知，不影响发布结果。
     *
     * @param sendResult 通知服务返回结果，失败时记录 message
     * @param receiverCount 接收人数量，避免日志输出全量用户 ID
     */
    private fun logSendResult(
        sendResult: Result<Boolean>,
        projectId: String,
        pipelineId: String,
        version: Int,
        templateCode: String,
        receiverCount: Int
    ) {
        if (sendResult.isNotOk()) {
            logger.warn(
                "send publish version notify failed|$projectId|$pipelineId|$version|" +
                    "templateCode=$templateCode|receiverCount=$receiverCount|${sendResult.message}"
            )
        } else {
            logger.info(
                "send publish version notify|$projectId|$pipelineId|$version|" +
                    "templateCode=$templateCode|receiverCount=$receiverCount"
            )
        }
    }

    private fun parseReceiver(receiversStr: String?, variables: Map<String, String>): MutableSet<String> {
        val receivers = mutableSetOf<String>()
        if (!receiversStr.isNullOrBlank()) {
            receiversStr.split(USER_SEPARATOR_REGEX).forEach {
                val userId = EnvUtils.parseEnv(command = it, data = variables, replaceWithEmpty = true)
                if (userId.contains(USER_SEPARATOR_REGEX)) {
                    receivers.addAll(userId.split(USER_SEPARATOR_REGEX))
                } else {
                    receivers.add(userId)
                }
            }
        }
        return receivers.filter { it.isNotBlank() }.toMutableSet()
    }

    private fun getVersionStatusAndName(
        projectId: String,
        pipelineId: String,
        draftVersion: PipelineResourceVersion,
        targetAction: CodeTargetAction,
        repoHashId: String?,
        targetBranch: String?
    ): Pair<VersionStatus, String?> {
        return when (targetAction) {
            // 新建分支创建MR, 创建分支版本
            CodeTargetAction.CHECKOUT_BRANCH_AND_REQUEST_MERGE -> {
                Pair(VersionStatus.BRANCH_RELEASE, getReleaseBranchName(pipelineId, draftVersion.version))
            }

            // 提交到源分支,创建分支版本
            CodeTargetAction.COMMIT_TO_SOURCE_BRANCH,
            CodeTargetAction.COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE -> {
                val baseVersion = draftVersion.baseVersion?.let {
                    pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId, it)
                }
                if (baseVersion == null) {
                    Pair(VersionStatus.DRAFT_RELEASE, null)
                } else {
                    Pair(VersionStatus.BRANCH_RELEASE, baseVersion.versionName)
                }
            }

            // 提交到指定分支,需要判断是否是默认分支,如果是默认分支,则发布成正式版本
            CodeTargetAction.COMMIT_TO_BRANCH -> {
                if (targetBranch.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_COMMIT_BRANCH_IS_NOT_EMPTY
                    )
                }
                if (repoHashId.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_REPO_HASH_ID_IS_NOT_EMPTY
                    )
                }
                val repositoryConfig = RepositoryConfig(
                    repositoryHashId = repoHashId,
                    repositoryName = null,
                    repositoryType = RepositoryType.ID
                )
                val defaultBranch = scmProxyService.getDefaultBranch(
                    projectId = projectId,
                    repositoryConfig = repositoryConfig
                )
                // 提交到默认分支,应该发布成正式版本
                if (defaultBranch == targetBranch) {
                    Pair(VersionStatus.DRAFT_RELEASE, null)
                } else {
                    Pair(VersionStatus.BRANCH_RELEASE, targetBranch)
                }
            }

            else -> {
                Pair(VersionStatus.DRAFT_RELEASE, null)
            }
        }
    }

    /**
     * 从自由模式下创建流水线
     */
    fun createPipelineFromFreedom(
        userId: String,
        projectId: String,
        request: TemplateInstanceCreateRequest
    ): DeployPipelineResult {
        val templateModel = if (request.emptyTemplate == true) {
            Model.defaultModel(request.pipelineName, userId)
        } else {
            val templateResource = if (request.templateVersion != null) {
                pipelineTemplateResourceService.get(
                    projectId = projectId,
                    templateId = request.templateId,
                    version = request.templateVersion!!
                )
            } else {
                pipelineTemplateResourceService.getLatestReleasedResource(
                    projectId = projectId,
                    templateId = request.templateId
                ) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
                )
            }
            templateResource.model as Model
        }
        val channelCode = ChannelCode.getRequestChannelCode()
        request.pipelineDialect?.takeIf { channelCode == ChannelCode.CREATIVE_STREAM }?.let {
            request.pipelineDialect = PipelineDialectType.CONSTRAINED.name
        }
        val pipelineAsCodeSettings = PipelineAsCodeSettings.initDialect(
            inheritedDialect = request.inheritedDialect,
            pipelineDialect = request.pipelineDialect
        )
        val setting = pipelineRepositoryService.createDefaultSetting(
            projectId = projectId,
            pipelineId = "",
            pipelineName = request.pipelineName,
            channelCode = channelCode
        ).copy(
            pipelineAsCodeSettings = pipelineAsCodeSettings,
            labels = request.labels,
            envHashId = request.envHashId
        )

        return pipelineInfoFacadeService.createPipeline(
            userId = userId,
            projectId = projectId,
            model = templateModel.copy(
                name = request.pipelineName,
                templateId = request.templateId,
                instanceFromTemplate = false,
                staticViews = request.staticViews,
                labels = request.labels,
                desc = request.pipelineDesc
            ),
            channelCode = ChannelCode.getRequestChannelCode(),
            setting = setting,
            checkPermission = true,
            instanceType = request.instanceType,
            versionStatus = VersionStatus.COMMITTING,
            useSubscriptionSettings = request.useSubscriptionSettings,
            useConcurrencyGroup = request.useConcurrencyGroup
        )
    }

    fun getVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?,
        archiveFlag: Boolean? = false,
        source: PipelineGetVersionSource? = PipelineGetVersionSource.VIEW,
        draftVersion: Int? = null
    ): PipelineVersionWithModel {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            queryDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode, errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
        )
        val editPermission = pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT
        )
        val resource = if (version != null && draftVersion != null) {
            pipelineRepositoryService.getPipelineResourceByDraftVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                draftVersion = draftVersion
            )
        } else {
            pipelineRepositoryService.getPipelineResourceVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                includeDraft = true,
                encryptedFlag = !editPermission,
                archiveFlag = archiveFlag
            )
        } ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version?.toString() ?: pipelineInfo.version.toString())
        )
        val setting = if (version != null && draftVersion != null) {
            pipelineSettingFacadeService.getPipelineSettingByDraftVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                draftVersion = draftVersion
            )
        } else {
            pipelineSettingFacadeService.userGetSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = resource.settingVersion ?: NUM_ZERO, // 历史没有关联过setting版本应该取正式版本
                archiveFlag = archiveFlag
            )
        }
        val model = pipelineInfoFacadeService.getFixedModel(
            resource = resource,
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            pipelineInfo = pipelineInfo,
            archiveFlag = archiveFlag
        )

        // 在正常查看/对比编排时需要对敏感字段加密，只有编辑场景且有编辑权限时不加密。
        val isEncryptParamsValue = source != PipelineGetVersionSource.EDIT || !editPermission
        if (isEncryptParamsValue) {
            resource.model.encryptParamsValue()
            model.encryptParamsValue()
        }

        /* 兼容存量数据 */
        model.desc = setting.desc
        // 后端主动填充前端展示的标签名称
        val modelAndSetting = PipelineModelAndSetting(
            setting = setting,
            model = model
        )
        val baseResource = resource.baseVersion?.let {
            repositoryVersionService.getPipelineVersionSimple(
                projectId = projectId,
                pipelineId = pipelineId,
                version = it,
                archiveFlag = archiveFlag
            )
        }
        val (yamlSupported, yamlPreview, msg) = try {
            // 如果是查看版本对比,如果是实例化流水线,需要展示完整的yaml内容
            val yamlResource = if (source == PipelineGetVersionSource.COMPARE && resource.model.template != null) {
                resource.copy(model = model.copy(template = null))
            } else {
                resource
            }
            val response = transferService.buildPreview(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                resource = yamlResource,
                editPermission = editPermission,
                archiveFlag = archiveFlag,
                isEncryptParamsValue = isEncryptParamsValue,
                channelCode = pipelineInfo.channelCode
            )
            Triple(true, response, null)
        } catch (e: PipelineTransferException) {
            Triple(
                false, null, I18nUtil.getCodeLanMessage(
                messageCode = e.errorCode,
                params = e.params,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                defaultMessage = e.defaultMessage
            )
            )
        }
        return PipelineVersionWithModel(
            modelAndSetting = modelAndSetting,
            yamlPreview = yamlPreview,
            description = resource.description,
            canDebug = resource.status == VersionStatus.COMMITTING,
            version = resource.version,
            versionName = resource.versionName,
            baseVersion = resource.baseVersion,
            baseVersionName = baseResource?.versionName,
            yamlSupported = yamlSupported,
            yamlInvalidMsg = msg,
            updater = resource.updater ?: resource.creator,
            updateTime = resource.updateTime?.timestampmilli(),
            versionStatus = resource.status,
            latestVersion = pipelineInfo.version,
            envHashId = setting.envHashId,
            draftVersion = resource.draftVersion,
            yamlLocked = pipelineRepositoryService.isYamlLockedOnRun(
                projectId = projectId,
                pipelineId = pipelineId,
                setting = setting
            )
        )
    }

    fun preview(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?
    ): PreviewResponse {
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = projectId, pipelineId = pipelineId
        )
        return transferService.buildPreview(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            resource = resource,
            channelCode = pipelineInfo?.channelCode ?: ChannelCode.BS
        )
    }

    fun savePipelineDraft(
        userId: String,
        projectId: String,
        modelAndYaml: PipelineVersionWithModelRequest
    ): DeployPipelineResult {
        return pipelineVersionManager.deployPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = modelAndYaml.pipelineId,
            request = PipelineDraftSaveReq(modelAndYaml)
        )
    }

    fun listPipelineVersionInfo(
        projectId: String,
        pipelineId: String,
        page: Int,
        pageSize: Int,
        fromVersion: Int?,
        includeDraft: Boolean? = true,
        versionName: String? = null,
        creator: String? = null,
        description: String? = null
    ): Page<PipelineVersionWithInfo> {
        var slqLimit: SQLLimit? = null
        if (pageSize != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)

        val offset = slqLimit?.offset ?: 0
        var limit = slqLimit?.limit ?: -1
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        // 如果有要插队的版本需要提到第一页，则在查询list时排除，单独查出来放在第一页
        val fromResource = if (fromVersion != null && page == 1) {
            limit -= 1
            repositoryVersionService.getPipelineVersionWithInfo(
                pipelineInfo = pipelineInfo,
                projectId = projectId,
                pipelineId = pipelineId,
                version = fromVersion,
                includeDraft = includeDraft
            )
        } else null
        val (size, pipelines) = repositoryVersionService.listPipelineVersionWithInfo(
            pipelineInfo = pipelineInfo,
            projectId = projectId,
            pipelineId = pipelineId,
            creator = creator,
            description = description,
            versionName = versionName,
            includeDraft = includeDraft,
            excludeVersion = fromVersion,
            offset = offset,
            limit = limit
        )
        fromResource?.let { pipelines.add(it) }
        return Page(
            page = page,
            pageSize = pageSize,
            count = size.toLong(),
            records = pipelines
        )
    }

    fun getPipelineVersionInfo(
        projectId: String,
        pipelineId: String,
        version: Int
    ): PipelineVersionWithInfo {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        return repositoryVersionService.getPipelineVersionWithInfo(
            pipelineInfo = pipelineInfo,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
    }

    fun listPipelineVersion(
        projectId: String,
        pipelineId: String,
        page: Int,
        pageSize: Int,
        fromVersion: Int?,
        includeDraft: Boolean? = true,
        versionName: String? = null,
        creator: String? = null,
        description: String? = null,
        buildOnly: Boolean? = false,
        archiveFlag: Boolean? = false
    ): Page<PipelineVersionSimple> {
        var slqLimit: SQLLimit? = null
        if (pageSize != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)

        val offset = slqLimit?.offset ?: 0
        var limit = slqLimit?.limit ?: -1
        val result = mutableListOf<PipelineVersionSimple>()
        // 如果有草稿版本需要提到第一页，单独查出来放在第一页并顶置
        val draftResource = if (includeDraft != false && page == 1) {
            limit -= 1
            pipelineRepositoryService.getDraftVersionResource(
                projectId = projectId,
                pipelineId = pipelineId,
                archiveFlag = archiveFlag
            )?.toSimple()?.apply {
                baseVersionName = baseVersion?.let {
                    repositoryVersionService.getPipelineVersionSimple(
                        projectId = projectId, pipelineId = pipelineId, version = it, archiveFlag = archiveFlag
                    )?.versionName
                }
            }
        } else null
        // 如果有要插队的版本需要提到第一页，则在查询list时排除，单独查出来放在第一页
        val fromResource = if (fromVersion != null && page == 1) {
            limit -= 1
            repositoryVersionService.getPipelineVersionSimple(
                projectId = projectId,
                pipelineId = pipelineId,
                version = fromVersion,
                archiveFlag = archiveFlag
            )
        } else null
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            queryDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
        )
        var (size, pipelines) = repositoryVersionService.listPipelineReleaseVersion(
            pipelineInfo = pipelineInfo,
            projectId = projectId,
            pipelineId = pipelineId,
            creator = creator,
            description = description,
            versionName = versionName,
            excludeVersion = fromVersion,
            offset = offset,
            limit = limit,
            buildOnly = buildOnly,
            archiveFlag = archiveFlag
        )
        draftResource?.let {
            size++
            result.add(it)
        }
        result.addAll(pipelines)
        fromResource?.let {
            size++
            result.add(it)
        }
        return Page(
            page = page,
            pageSize = pageSize,
            count = size.toLong(),
            records = result
        )
    }

    fun getPipelineVersion(
        projectId: String,
        pipelineId: String,
        version: Int,
        archiveFlag: Boolean? = false
    ): PipelineVersionSimple {
        return repositoryVersionService.getPipelineVersionSimple(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            archiveFlag = archiveFlag
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
    }

    fun rollbackDraftFromVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        draftVersion: Int? = null
    ): PipelineVersionSimple {
        val deployPipelineResult = pipelineVersionManager.deployPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            request = PipelineRollbackReq(draftVersion = draftVersion)
        )
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = deployPipelineResult.version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )

        return PipelineVersionSimple(
            pipelineId = pipelineId,
            creator = resource.creator,
            createTime = resource.createTime.timestampmilli(),
            updater = resource.updater,
            updateTime = resource.updateTime?.timestampmilli(),
            version = resource.version,
            versionName = resource.versionName ?: "",
            referFlag = resource.referFlag,
            referCount = resource.referCount,
            versionNum = resource.versionNum,
            pipelineVersion = resource.pipelineVersion,
            triggerVersion = resource.triggerVersion,
            settingVersion = resource.settingVersion,
            status = resource.status,
            debugBuildId = resource.debugBuildId,
            baseVersion = resource.baseVersion,
            description = resource.description,
            yamlVersion = resource.yamlVersion
        )
    }

    fun canRollbackFromVersion(
        projectId: String,
        pipelineId: String,
        version: Int
    ): Boolean {
        // 如果没有关联模版,可以回滚
        pipelineTemplateRelatedService.get(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: return true
        val pipelineResource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
        // 如果是新版的模版,已经保存模版的信息,可以回滚
        if (pipelineResource.model.template != null) {
            return true
        }
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
        // 如果是旧版的模版,没有保存模版的信息,需要流水线是最新版本才能回滚
        return pipelineInfo.version == version
    }

    fun deletePipelineVersion(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int,
        checkPermission: Boolean = true
    ): String {
        repositoryVersionService.deletePipelineVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
        // 清理该版本（含草稿）的公共变量组引用明细：
        // 删除保护会把草稿版本的引用也算进去（存在任意引用即禁止删组），
        // 若删除版本时不清理其引用明细，会残留"幽灵引用"导致变量组永远无法删除。
        publicVarGroupReferManageService.deletePublicGroupRefer(
            userId = userId,
            projectId = projectId,
            referId = pipelineId,
            referType = PublicVarGroupReferenceTypeEnum.PIPELINE,
            referVersion = version
        )
        return pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)?.pipelineName ?: pipelineId
    }

    fun getVersionCreatorInPage(
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Page<String> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        var slqLimit: SQLLimit? = null
        if (pageSizeNotNull != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)

        val offset = slqLimit?.offset ?: 0
        val limit = slqLimit?.limit ?: -1
        // 数据量不多，直接全拉
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
        val (size, pipelines) = repositoryVersionService.getVersionCreatorInPage(
            pipelineInfo = pipelineInfo,
            projectId = projectId,
            pipelineId = pipelineId,
            offset = offset,
            limit = limit
        )
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = size.toLong(),
            records = pipelines
        )
    }

    fun getVersionByBranch(
        userId: String,
        projectId: String,
        pipelineId: String,
        branch: String,
        archiveFlag: Boolean? = false,
        source: PipelineGetVersionSource? = PipelineGetVersionSource.VIEW
    ): PipelineVersionWithModel {
        // 流水线分支版本
        val version = pipelineYamlFacadeService.getPipelineYamlVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            branch = branch
        )!!
        return getVersion(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            archiveFlag = archiveFlag,
            source = source
        ).copy(
            versionName = branch
        )
    }

    fun getVersionByBranch(
        projectId: String,
        pipelineId: String,
        branch: String
    ): PipelineResourceVersion? {
        // 流水线分支版本
        return pipelineYamlFacadeService.getPipelineYamlVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            branch = branch
        )?.let {
            pipelineRepositoryService.getPipelineResourceVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                version = it
            )
        }
    }

    fun listPacVersions(
        userId: String,
        projectId: String,
        pipelineId: String,
        search: String?,
        page: Int?,
        pageSize: Int?
    ): List<PipelineYamlBuildVersion> {
        // 检查PAC信息
        val pipelineYamlInfo = pipelineYamlFacadeService.getPipelineYamlInfo(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_PIPELINE_IS_NOT_PAC,
            params = arrayOf(pipelineId)
        )
        pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW,
            authResourceType = AuthResourceType.PIPELINE_DEFAULT
        )
        val list = mutableListOf<PipelineYamlBuildVersion>()
        val finalPage = page ?: 1
        val finalPageSize = pageSize ?: 20
        // PAC 仓库信息
        val repository = client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryType = RepositoryType.ID,
            repositoryId = pipelineYamlInfo.repoHashId
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.GIT_NOT_FOUND,
            params = arrayOf(pipelineYamlInfo.repoHashId)
        )
        // 仓库默认分支
        val defaultBranchName = pipelineYamlCommonService.getServiceRepository(
            projectId = projectId,
            repository = repository
        ).let {
            if (it !is GitScmServerRepository) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_NOT_SUPPORT_REPOSITORY_TYPE_ENABLE_PAC
                )
            }
            it.defaultBranch
        } ?: ""
        val needBaseVersion = finalPage == 1 && search.isNullOrBlank()
        // 首页需查询正式版本/默认分支
        if (needBaseVersion) {
            // 默认返回当前最新的正式版本（当流水线仅有分支版本时不添加）
            pipelineRepositoryService.getPipelineResourceVersion(
                projectId = projectId,
                pipelineId = pipelineId
            )?.takeIf {
                // 检查对应版本号是否为正式版本
                pipelineRepositoryService.getPipelineResourceVersion(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = it.version
                )?.status == VersionStatus.RELEASED
            }?.let {
                list.add(
                    PipelineYamlBuildVersion(
                        name = it.versionName ?: "",
                        version = it.version,
                        versionStatus = VersionStatus.RELEASED
                    )
                )
            }
            // 追加默认分支
            list.add(
                PipelineYamlBuildVersion(
                    name = defaultBranchName,
                    versionStatus = VersionStatus.BRANCH,
                    defaultBranch = true
                )
            )
        }
        // 仓库分支列表
        pipelineYamlCommonService.getServiceBranch(
            projectId = projectId,
            repository = repository,
            page = finalPage,
            pageSize = finalPageSize,
            search = search?.takeIf { it.isNotBlank() }
        )?.forEach {
            if (it.name == defaultBranchName && needBaseVersion) {
                return@forEach
            }
            list.add(
                PipelineYamlBuildVersion(
                    name = it.name,
                    versionStatus = VersionStatus.BRANCH,
                    defaultBranch = (it.name == defaultBranchName),
                    sha = it.sha
                )
            )
        }
        return list
    }

    /**
     * 获取流水线草稿状态，用于前端进入编辑、保存、发布前判断当前版本是否可继续操作。
     *
     * 本方法的核心职责是校验前端展示的流水线版本与后端是否一致，不一致则返回对应的提示状态。
     *
     * 参数说明：
     * - [version] 前端当前操作的流水线版本，必传。
     * - [versionStatus] 前端持有 [version] 时该版本对应的状态，必传，用于识别前端在编辑草稿还是正式版本。
     * - [releaseVersion] 前端界面当前展示的正式版本号，必传，用于判断前端界面是否落后于最新正式版本。
     * - [baseDraftVersion] 草稿并发保存校验版本号，SAVE / RELEASE 时需要传入。
     */
    fun getPipelineDraftStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        actionType: PipelineDraftActionType,
        version: Int,
        versionStatus: VersionStatus,
        releaseVersion: Int,
        baseDraftVersion: Int?
    ): PipelineDraftStatusResult {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
        )
        val versionResource = pipelineRepositoryService.getPipelineVersionRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        )
        val releaseResource = if (version == releaseVersion) {
            versionResource
        } else {
            pipelineRepositoryService.getPipelineVersionRecord(
                projectId = projectId,
                pipelineId = pipelineId,
                version = releaseVersion,
                includeDraft = false
            )
        }
        val draftResource = pipelineRepositoryService.getDraftVersionResource(
            projectId = projectId,
            pipelineId = pipelineId
        )
        val latestReleaseResource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = pipelineInfo.version
        )

        return when (actionType) {
            PipelineDraftActionType.EDIT -> getPipelineDraftStatusWhenEdit(
                userId = userId,
                releaseVersion = releaseVersion,
                releaseResource = releaseResource,
                draftResource = draftResource,
                latestReleaseResource = latestReleaseResource
            )
            PipelineDraftActionType.SAVE -> getPipelineDraftStatusWhenSave(
                versionStatus = versionStatus,
                releaseVersion = releaseVersion,
                versionResource = versionResource,
                draftResource = draftResource,
                baseDraftVersion = baseDraftVersion,
                latestReleaseResource = latestReleaseResource
            )
            PipelineDraftActionType.RELEASE -> getPipelineDraftStatusWhenRelease(
                versionResource = versionResource,
                latestReleaseResource = latestReleaseResource,
                baseDraftVersion = baseDraftVersion
            )
        }
    }

    /**
     * EDIT 场景状态校验：
     *
     * - 后端草稿存在：按草稿状态判断（草稿基线落后 → OUTDATED；保存人不同或超 7 天 → EXISTS；否则 NORMAL）
     * - 后端草稿不存在：
     *   - releaseVersion 对应资源是分支版本 → BRANCH
     *   - releaseVersion 与当前最新正式版本不一致 → RELEASE_OUTDATED
     *   - 否则 → NORMAL
     *
     * 说明：无论前端展示的是草稿还是正式/分支版本，统一按上述规则处理；
     * 原草稿已被删除或发布时，若仍有新草稿则走草稿校验，否则不单独提示 DELETED/PUBLISHED。
     */
    private fun getPipelineDraftStatusWhenEdit(
        userId: String,
        releaseVersion: Int,
        releaseResource: PipelineResourceVersion?,
        draftResource: PipelineResourceVersion?,
        latestReleaseResource: PipelineResourceVersion?
    ): PipelineDraftStatusResult {
        // 若后端已存在草稿，则先按草稿状态做校验
        if (draftResource != null) {
            return checkDraftForEdit(
                userId = userId,
                draftResource = draftResource,
                latestReleaseResource = latestReleaseResource
            )
        }
        // 前端展示的版本视图对应分支版本
        if (latestReleaseResource?.status == VersionStatus.BRANCH) {
            return PipelineDraftStatusResult(
                status = PipelineDraftStatus.BRANCH,
                release = PipelineVersionSimple(latestReleaseResource)
            )
        }
        // 前端展示的正式版本已不是最新
        return if (latestReleaseResource != null &&
            latestReleaseResource.version != releaseVersion
        ) {
            PipelineDraftStatusResult(
                status = PipelineDraftStatus.RELEASE_OUTDATED,
                draft = releaseResource?.let { PipelineVersionSimple(it) },
                release = PipelineVersionSimple(latestReleaseResource)
            )
        } else {
            PipelineDraftStatusResult(status = PipelineDraftStatus.NORMAL)
        }
    }

    /**
     * SAVE 场景状态校验：
     *
     * 1. versionStatus != 草稿状态（前端展示的不是草稿版本，即基于正式版本 / 分支版本进行保存）
     *    - 后端草稿存在 → CONFLICT（提示"草稿版本冲突"，避免误覆盖）
     *    - 后端草稿不存在：前端正式版与最新不一致 → RELEASE_OUTDATED，否则 → NORMAL
     *
     * 2. versionStatus == 草稿状态（前端展示的是草稿版本）
     *    - version 已发布 → PUBLISHED
     *    - version 已删除：若存在新草稿 → CONFLICT；否则校验正式版 → RELEASE_OUTDATED / NORMAL
     *    - version 仍是草稿态：比对 draftVersion 与 baseDraftVersion，不一致 → CONFLICT，否则 → NORMAL
     */
    private fun getPipelineDraftStatusWhenSave(
        versionStatus: VersionStatus,
        releaseVersion: Int,
        versionResource: PipelineResourceVersion?,
        draftResource: PipelineResourceVersion?,
        baseDraftVersion: Int?,
        latestReleaseResource: PipelineResourceVersion?
    ): PipelineDraftStatusResult {
        if (versionStatus != VersionStatus.COMMITTING) {
            // 前端展示的不是草稿版本进行保存，若后端已存在草稿，则提示草稿冲突
            return if (draftResource != null) {
                PipelineDraftStatusResult(
                    status = PipelineDraftStatus.CONFLICT,
                    draft = PipelineVersionSimple(draftResource)
                )
            } else {
                buildSaveNormalOrReleaseOutdated(
                    releaseVersion = releaseVersion,
                    latestReleaseResource = latestReleaseResource
                )
            }
        }
        // 前端展示的version是草稿版本，但后端version已不是草稿态（可能被发布或删除）
        if (versionResource?.status != VersionStatus.COMMITTING) {
            return when (versionResource?.status) {
                VersionStatus.DELETE -> {
                    if (draftResource != null) {
                        PipelineDraftStatusResult(
                            status = PipelineDraftStatus.CONFLICT,
                            draft = PipelineVersionSimple(draftResource)
                        )
                    } else {
                        buildSaveNormalOrReleaseOutdated(
                            releaseVersion = releaseVersion,
                            latestReleaseResource = latestReleaseResource
                        )
                    }
                }
                else -> PipelineDraftStatusResult(
                    status = PipelineDraftStatus.PUBLISHED,
                    release = latestReleaseResource?.let { PipelineVersionSimple(it) }
                )
            }
        }
        // 前端展示的version是草稿版本，且后端version仍是草稿态,这里versionResource和draftResource应该是相同的
        return if (versionResource.draftVersion != baseDraftVersion) {
            PipelineDraftStatusResult(
                status = PipelineDraftStatus.CONFLICT,
                draft = PipelineVersionSimple(versionResource)
            )
        } else {
            PipelineDraftStatusResult(
                status = PipelineDraftStatus.NORMAL,
                draft = PipelineVersionSimple(versionResource)
            )
        }
    }

    /**
     * RELEASE 场景状态校验：
     *
     * - version 不是草稿态（可能被发布或删除）：按 version 状态返回（PUBLISHED / DELETED）
     * - version 仍是草稿态：
     *   - 比对 draftVersion 与 baseDraftVersion，不一致 → CONFLICT（并发保存冲突）
     *   - 草稿基线版本与当前最新正式版本不一致 → OUTDATED
     *   - 否则 → NORMAL
     */
    private fun getPipelineDraftStatusWhenRelease(
        versionResource: PipelineResourceVersion?,
        latestReleaseResource: PipelineResourceVersion?,
        baseDraftVersion: Int?
    ): PipelineDraftStatusResult {
        // 前端要发布的版本已不是草稿态（可能被发布或删除）
        if (versionResource?.status != VersionStatus.COMMITTING) {
            return if (versionResource?.status == VersionStatus.DELETE) {
                PipelineDraftStatusResult(
                    status = PipelineDraftStatus.DELETED,
                    draft = PipelineVersionSimple(versionResource)
                )
            } else {
                PipelineDraftStatusResult(
                    status = PipelineDraftStatus.PUBLISHED,
                    release = versionResource?.let { PipelineVersionSimple(it) }
                )
            }
        }
        // 检测草稿并发保存冲突
        if (versionResource.draftVersion != baseDraftVersion) {
            return PipelineDraftStatusResult(
                status = PipelineDraftStatus.CONFLICT,
                draft = PipelineVersionSimple(versionResource)
            )
        }
        // 检查当前待发布草稿的基线版本，是否与当前最新正式版本一致
        return if (latestReleaseResource != null &&
            versionResource.baseVersion != null &&
            latestReleaseResource.version != versionResource.baseVersion
        ) {
            val baseResource = versionResource.baseVersion?.let { baseVersion ->
                pipelineRepositoryService.getPipelineResourceVersion(
                    projectId = versionResource.projectId,
                    pipelineId = versionResource.pipelineId,
                    version = baseVersion
                )
            }
            // 若基线是分支版本，则提示草稿基于分支版本,否则提示版本落后
            val status = if (baseResource?.status == VersionStatus.BRANCH) {
                PipelineDraftStatus.BASE_BRANCH
            } else {
                PipelineDraftStatus.BASE_OUTDATED
            }
            PipelineDraftStatusResult(
                status = status,
                draft = PipelineVersionSimple(versionResource).copy(
                    baseVersionName = baseResource?.versionName
                ),
                release = PipelineVersionSimple(latestReleaseResource)
            )
        } else {
            PipelineDraftStatusResult(
                status = PipelineDraftStatus.NORMAL,
                draft = PipelineVersionSimple(versionResource)
            )
        }
    }

    private fun buildSaveNormalOrReleaseOutdated(
        releaseVersion: Int,
        latestReleaseResource: PipelineResourceVersion?
    ): PipelineDraftStatusResult {
        // 保存草稿时,如果没有草稿,但是有最新正式版本,则提示正式版本落后
        // 场景: 模版实例化流水线,tabA在流水线保存页面,tabB模版实例化更新,tabA去保存流水线时,提示正式版本落后
        return if (latestReleaseResource != null &&
            latestReleaseResource.version != releaseVersion
        ) {
            PipelineDraftStatusResult(
                status = PipelineDraftStatus.RELEASE_OUTDATED,
                release = PipelineVersionSimple(latestReleaseResource)
            )
        } else {
            PipelineDraftStatusResult(status = PipelineDraftStatus.NORMAL)
        }
    }

    private fun checkDraftForEdit(
        userId: String,
        draftResource: PipelineResourceVersion,
        latestReleaseResource: PipelineResourceVersion?
    ): PipelineDraftStatusResult {
        val baseResource = draftResource.baseVersion?.let { baseVersion ->
            pipelineRepositoryService.getPipelineResourceVersion(
                projectId = draftResource.projectId,
                pipelineId = draftResource.pipelineId,
                version = baseVersion
            )
        }
        val updateTime = draftResource.updateTime?.timestampmilli()
            ?: draftResource.createTime.timestampmilli()
        val updater = draftResource.updater ?: draftResource.creator
        val releaseTime = latestReleaseResource?.releaseTime
        val baseReleaseTime = baseResource?.releaseTime
        return when {
            // 若草稿基线版本早于当前最新版本,则提示草稿版本落后
            releaseTime != null && baseReleaseTime != null && releaseTime > baseReleaseTime -> {
                // 如果草稿基线基于分支版本,则提示草稿基于分支版本
                val status = if (baseResource.status == VersionStatus.BRANCH) {
                    PipelineDraftStatus.BASE_BRANCH
                } else {
                    PipelineDraftStatus.BASE_OUTDATED
                }
                PipelineDraftStatusResult(
                    status = status,
                    draft = PipelineVersionSimple(draftResource).copy(
                        baseVersionName = baseResource.versionName
                    ),
                    release = PipelineVersionSimple(latestReleaseResource)
                )
            }

            // 1. 若当前操作人和原草稿的保存人不相同
            // 2. 当前操作人和原草稿的保存人相同，则检查最近保存时间是否超过 7 天，若未超过则不重复提醒
            updater != userId ||
                    System.currentTimeMillis() - updateTime > TimeUnit.DAYS.toMillis(maxReminderDays) -> {
                PipelineDraftStatusResult(
                    status = PipelineDraftStatus.EXISTS,
                    draft = PipelineVersionSimple(draftResource)
                )
            }

            else -> PipelineDraftStatusResult(
                status = PipelineDraftStatus.NORMAL,
                draft = PipelineVersionSimple(draftResource)
            )
        }
    }

    fun listPipelineDraftVersions(
        projectId: String,
        pipelineId: String,
        version: Int,
        page: Int?,
        pageSize: Int?
    ): Page<PipelineDraftVersionSimple> {
        return repositoryVersionService.listPipelineDraftVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            page = page,
            pageSize = pageSize
        )
    }
}
