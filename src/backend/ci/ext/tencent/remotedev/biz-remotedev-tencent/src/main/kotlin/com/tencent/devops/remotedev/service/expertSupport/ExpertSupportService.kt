package com.tencent.devops.remotedev.service.expertSupport

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.notify.utils.NotifyUtils
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.remotedev.dao.ExpertSupportDao
import com.tencent.devops.remotedev.pojo.expertSupport.CreateExpertSupportConfigData
import com.tencent.devops.remotedev.pojo.expertSupport.CreateSupportData
import com.tencent.devops.remotedev.pojo.expertSupport.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expertSupport.ExpertSupportStatus
import com.tencent.devops.remotedev.pojo.expertSupport.FetchExpertSupResp
import com.tencent.devops.remotedev.pojo.expertSupport.UpdateSupportData
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

//    fun fetchSupport(
//        projectId: String,
//        hostIp: String,
//        status: ExpertSupportStatus
//    ): List<FetchSupportResp> {
//        return expertSupportDao.fetchSupports(dslContext, projectId, hostIp, status).map {
//            FetchSupportResp(
//                id = it.id,
//                creator = it.creator,
//                content = it.content,
//                createTime = it.createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"))
//            )
//        }
//    }

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
    }
}
