package com.tencent.devops.remotedev.service.expert

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.remotedev.common.Constansts.ADMIN_NAME
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.ExpertSupportDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.expert.CreateExpertSupportConfigData
import com.tencent.devops.remotedev.pojo.expert.CreateSupportData
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportStatus
import com.tencent.devops.remotedev.pojo.expert.FetchExpertSupResp
import com.tencent.devops.remotedev.pojo.expert.UpdateSupportData
import com.tencent.devops.remotedev.resources.op.AssignWorkspacePipelineInfo
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

@Service
class ExpertSupportService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val expertSupportDao: ExpertSupportDao,
    private val workspaceCommon: WorkspaceCommon,
    private val workspaceSharedDao: WorkspaceSharedDao,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao
) {
    @Value("\${expertsupport.rtxtemplate:#{null}}")
    val rtxTemplate: String? = null

    @Value("\${expertsupport.rtxAssignTemplate:#{null}}")
    val rtxAssignTemplate: String? = null

    @Value("\${expertsupport.jumpurl:#{null}}")
    val jumpUrl: String? = null

    @Value("\${expertsupport.weworkGroupId:#{null}}")
    val weworkGroupId: String? = null

    private val executor = Executors.newCachedThreadPool()

    fun createSupport(
        data: CreateSupportData
    ) {
        // 校验机器在不在
        val record = workspaceDao.fetchAnyWorkspace(
            dslContext = dslContext,
            workspaceName = data.workspaceName,
            mountType = WorkspaceMountType.START
        )
        if (record == null || record.status == WorkspaceStatus.DELETED) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_RUNNING.errorCode,
                params = arrayOf(data.workspaceName)
            )
        }

        val fetchExpertSupportData = expertSupportDao.fetchSupports(
            dslContext = dslContext,
            projectId = data.projectId,
            hostIp = data.hostIp,
            creator = data.creator,
            status = ExpertSupportStatus.CREATE,
            content = data.content,
            internalTime = DEFAULT_WAIT_TIME
        )
        if (fetchExpertSupportData.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REAPPLY_EXPERT_SUPPORT_ERROR.errorCode,
                params = arrayOf(data.content)
            )
        }

        val id = expertSupportDao.addSupport(
            dslContext = dslContext,
            projectId = data.projectId,
            hostIp = data.hostIp,
            workspaceName = data.workspaceName,
            creator = data.creator,
            status = ExpertSupportStatus.CREATE,
            content = data.content,
            city = data.city,
            machineType = data.machineType
        )
        // 发送企业微信群消息
        client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
            SendNotifyMessageTemplateRequest(
                templateCode = rtxTemplate ?: return,
                notifyType = mutableSetOf(NotifyType.WEWORK_GROUP.name),
                titleParams = null,
                bodyParams = mapOf(
                    NotifyUtils.WEWORK_GROUP_KEY to weworkGroupId!!,
                    "id" to id.toString(),
                    "projectId" to data.projectId,
                    "workspaceName" to data.workspaceName,
                    "hostIp" to data.hostIp,
                    "userId" to data.creator,
                    "content" to data.content,
                    "url" to jumpUrl.toString(),
                    "machineType" to data.machineType,
                    "city" to data.city
                ),
                markdownContent = true
            )
        )
        // 异步执行流水线完成其他动作
        executor.execute {
            try {
                val infoS = redisOperation.get(PIPELINE_EXPORT_CONFIG_INFO) ?: return@execute
                val info = JsonUtil.to(infoS, AssignWorkspacePipelineInfo::class.java)

                val newParam = mutableMapOf<String, String>()
                val hostIdSub = data.hostIp.split(".")
                val ip = hostIdSub.subList(1, hostIdSub.size).joinToString(separator = ".")
                info.buildParam.forEach { (k, v) ->
                    when (v) {
                        "ip" -> newParam[k] = ip
                        "projectId" -> newParam[k] = data.projectId
                        else -> newParam[k] = v
                    }
                }

                client.get(ServiceBuildResource::class).manualStartupNew(
                    userId = info.userId ?: "",
                    projectId = info.projectId,
                    pipelineId = info.pipelineId,
                    values = newParam,
                    channelCode = ChannelCode.BS,
                    buildNo = null,
                    startType = StartType.SERVICE
                )
            } catch (e: Exception) {
                logger.warn("execute createSupport pipeline error", e)
            }
        }
    }

    fun updateSupportStatus(
        data: UpdateSupportData
    ) {
        expertSupportDao.updateSupport(
            dslContext = dslContext,
            id = data.id,
            status = data.status,
            supporter = if (data.supporter == null) {
                null
            } else {
                setOf(data.supporter!!)
            }
        )
    }

    fun fetchSupportConfig(
        type: ExpertSupportConfigType
    ): List<FetchExpertSupResp> {
        return expertSupportDao.fetchExpertSupportConfig(dslContext, type).map {
            FetchExpertSupResp(
                id = it.id,
                content = it.content
            )
        }
    }

    fun addSupportConfig(
        data: CreateExpertSupportConfigData
    ) {
        expertSupportDao.addExpertSupportConfig(dslContext, data.type, data.content)
    }

    fun deleteSupportConfig(
        id: Long
    ) {
        expertSupportDao.deleteExpertSupportConfig(dslContext, id)
    }

    fun assignExpSup(userId: String, id: Long, workspaceName: String): Pair<Boolean, String?> {
        // 校验这个人是不是可以分配的运维
        if (!expertSupportDao.fetchExpertSupportConfig(dslContext, ExpertSupportConfigType.SUPPORTER)
            .map { it.content.trim() }.toSet().contains(userId.trim())
        ) {
            return Pair(false, "${userId}不是云研发运维，不可认领")
        }

        // 校验 1 小时之内是否分配过
        if (workspaceSharedDao.checkAlreadyExpireShare(
                dslContext = dslContext,
                workspaceName = workspaceName,
                operator = "system",
                sharedUser = userId,
                assignType = WorkspaceShared.AssignType.VIEWER
            )
        ) {
            return Pair(false, "${userId}已认领该工单")
        }

        // 校验机器在不在
        val record = workspaceDao.fetchAnyWorkspace(
            dslContext = dslContext,
            workspaceName = workspaceName,
            mountType = WorkspaceMountType.START
        )
        if (record == null || record.status == WorkspaceStatus.DELETED) {
            return Pair(false, "云桌面${workspaceName}不存在或者已销毁")
        }

        // 分配
        workspaceCommon.shareWorkspace(
            workspaceName = workspaceName,
            projectId = record.projectId,
            operator = ADMIN_NAME,
            assigns = listOf(
                ProjectWorkspaceAssign(
                    userId = userId,
                    type = WorkspaceShared.AssignType.VIEWER,
                    expiration = LocalDateTime.now().plusHours(1)
                )
            ),
            mountType = WorkspaceMountType.START
        )

        // 添加认领人信息
        expertSupportDao.getSup(dslContext, id)?.let {
            val sups = mutableSetOf(userId)
            if (it.supporter != null) {
                sups.addAll(JsonUtil.to<List<String>>(it.supporter))
            }

            expertSupportDao.updateSupport(
                dslContext = dslContext,
                id = id,
                status = ExpertSupportStatus.RUNNING,
                supporter = sups
            )
        }

        // 发送认领通知
        client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
            SendNotifyMessageTemplateRequest(
                templateCode = rtxAssignTemplate ?: return Pair(true, "通知模板为空"),
                notifyType = mutableSetOf(NotifyType.WEWORK_GROUP.name),
                titleParams = mapOf(
                    NotifyUtils.WEWORK_GROUP_KEY to weworkGroupId!!,
                    "id" to id.toString(),
                    "userId" to userId,
                    "time" to LocalDateTime.now().format(dateTimeFormatter)
                ),
                bodyParams = mapOf(
                    NotifyUtils.WEWORK_GROUP_KEY to weworkGroupId!!
                ),
                markdownContent = true
            )
        )

        return Pair(true, null)
    }

    fun queryCgsPwd(userId: String, cgsId: String): Pair<Boolean, String?> {
        // 校验这个人是不是可以分配的运维
        if (!expertSupportDao.fetchExpertSupportConfig(dslContext, ExpertSupportConfigType.SUPPORTER)
                .map { it.content.trim() }.toSet().contains(userId.trim())
        ) {
            return Pair(false, "${userId}不是云研发运维，不可查询")
        }
        try {
            val infoS = redisOperation.get(PIPELINE_QUERY_CGS_PWD) ?: return Pair(false, null)
            val info = JsonUtil.to(infoS, AssignWorkspacePipelineInfo::class.java)

            val newParam = mutableMapOf<String, String>()
            val hostIdSub = cgsId.split(".")
            val ip = hostIdSub.subList(1, hostIdSub.size).joinToString(separator = ".")
            info.buildParam.forEach { (k, v) ->
                when (v) {
                    "ip" -> newParam[k] = ip
                    "user" -> newParam[k] = userId
                    else -> newParam[k] = v
                }
            }

            client.get(ServiceBuildResource::class).manualStartupNew(
                userId = info.userId ?: "",
                projectId = info.projectId,
                pipelineId = info.pipelineId,
                values = newParam,
                channelCode = ChannelCode.BS,
                buildNo = null,
                startType = StartType.SERVICE
            )
        } catch (e: Exception) {
            logger.warn("execute createSupport pipeline error", e)
        }

        return Pair(true, "已发起查询，稍后通知密码")
    }
    companion object {
        private val logger = LoggerFactory.getLogger(ExpertSupportService::class.java)
        private const val DEFAULT_WAIT_TIME = 3600
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日HH:mm:ss")
        private const val PIPELINE_EXPORT_CONFIG_INFO = "remotedev:createExpSupport.pipelineinfo"
        private const val PIPELINE_QUERY_CGS_PWD = "remotedev:queryCgsPwd.pipelineinfo"
    }
}
