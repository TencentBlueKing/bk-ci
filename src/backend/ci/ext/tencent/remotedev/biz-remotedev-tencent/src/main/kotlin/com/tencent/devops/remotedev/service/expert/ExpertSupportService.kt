package com.tencent.devops.remotedev.service.expert

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.ExpertSupportDao
import com.tencent.devops.remotedev.pojo.expert.CreateExpertSupportConfigData
import com.tencent.devops.remotedev.pojo.expert.CreateSupportData
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportStatus
import com.tencent.devops.remotedev.pojo.expert.FetchExpertSupResp
import com.tencent.devops.remotedev.pojo.expert.UpdateSupportData
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
@Service
class ExpertSupportService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val expertSupportDao: ExpertSupportDao
) {
    @Value("\${expertsupport.rtxtemplate:#{null}}")
    val rtxTemplate: String? = null

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

    companion object {
        private val logger = LoggerFactory.getLogger(ExpertSupportService::class.java)
        private const val DEFAULT_WAIT_TIME = 3600
    }
}
