package com.tencent.devops.remotedev.service.expert

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.ExpertSupportDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.expert.CreateExpertSupportConfigData
import com.tencent.devops.remotedev.pojo.expert.CreateSupportData
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportStatus
import com.tencent.devops.remotedev.pojo.expert.FetchExpertSupResp
import com.tencent.devops.remotedev.pojo.expert.UpdateSupportData
import com.tencent.devops.remotedev.service.workspace.WorkspaceCommon
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ExpertSupportService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val expertSupportDao: ExpertSupportDao,
    private val workspaceCommon: WorkspaceCommon,
    private val workspaceSharedDao: WorkspaceSharedDao
) {
    @Value("\${expertsupport.rtxtemplate:#{null}}")
    val rtxTemplate: String? = null

    @Value("\${expertsupport.rtxAssignTemplate:#{null}}")
    val rtxAssignTemplate: String? = null

    @Value("\${expertsupport.jumpurl:#{null}}")
    val jumpUrl: String? = null

    @Value("\${expertsupport.weworkGroupId:#{null}}")
    val weworkGroupId: String? = null

    fun createSupport(
        data: CreateSupportData
    ) {
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
    }

    fun updateSupportStatus(
        data: UpdateSupportData
    ) {
        expertSupportDao.updateSupport(
            dslContext = dslContext,
            id = data.id,
            status = data.status,
            supporter = data.supporter
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
            return Pair(false, "当前用户${userId}不是可分配运维人员")
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
            return Pair(false, "${userId}已被分配")
        }

        // 分配
        workspaceCommon.shareWorkspace(
            workspaceName = workspaceName,
            operator = "system",
            assigns = listOf(
                ProjectWorkspaceAssign(
                    userId = userId,
                    type = WorkspaceShared.AssignType.VIEWER,
                    expiration = LocalDateTime.now().plusHours(1)
                )
            ),
            mountType = WorkspaceMountType.START
        )

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

    companion object {
        private val logger = LoggerFactory.getLogger(ExpertSupportService::class.java)
        private const val DEFAULT_WAIT_TIME = 3600
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日HH:mm:ss")
    }
}
