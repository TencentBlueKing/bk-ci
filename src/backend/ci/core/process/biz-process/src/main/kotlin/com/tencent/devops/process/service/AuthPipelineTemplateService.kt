package com.tencent.devops.process.service

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.model.process.tables.TTemplate
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthPipelineTemplateService @Autowired constructor(
    val authTokenApi: AuthTokenApi,
    val pipelineListFacadeService: PipelineListFacadeService,
    val templateDao: TemplateDao,
    val dslContext: DSLContext,
    val pipelineSettingDao: PipelineSettingDao
) {
    fun pipelineTemplateInfo(
        callBackInfo: CallbackRequestDTO,
        token: String
    ): CallbackBaseResponseDTO? {
        logger.info("iam call back info :$callBackInfo")
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent?.id ?: ""
        when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> {
                return getPipelineTemplate(
                    projectId = projectId,
                    offset = page.offset.toInt(),
                    limit = page.limit.toInt(),
                    token = token
                )
            }

            CallbackMethodEnum.FETCH_INSTANCE_INFO -> {
                val ids = callBackInfo.filter.idList.map { it.toString() }
                return getPipelineTemplateInfo(
                    projectId = projectId,
                    ids.toSet(),
                    token
                )
            }

            else -> {}
        }
        return null
    }

    private fun getPipelineTemplate(
        projectId: String,
        offset: Int,
        limit: Int,
        token: String
    ): ListInstanceResponseDTO {
        authTokenApi.checkToken(token)
        val templatesRecords = templateDao.listTemplate(
            dslContext = dslContext,
            projectId = projectId,
            includePublicFlag = null,
            templateType = null,
            templateIdList = null,
            storeFlag = null,
            offset = offset,
            limit = limit,
            queryModelFlag = true
        )
        val result = ListInstanceInfo()
        if (templatesRecords == null || templatesRecords.isEmpty()) {
            logger.info("There is no pipeline template under the project $projectId ")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        val tTemplate = TTemplate.T_TEMPLATE
        val templateId2Name = pipelineSettingDao.listPipelineNames(
            dslContext = dslContext,
            pipelineIds = templatesRecords.map { it[tTemplate.ID] },
            projectId = projectId
        )
        templatesRecords.map { record ->
            val templateId = record[tTemplate.ID]
            val name = templateId2Name[templateId] ?: return@map
            val entity = InstanceInfoDTO()
            entity.id = templateId
            entity.displayName = name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${entityInfo.size}")
        return result.buildListInstanceResult(entityInfo, entityInfo.size.toLong())
    }

    private fun getPipelineTemplateInfo(
        projectId: String?,
        ids: Set<String>,
        token: String
    ): FetchInstanceInfoResponseDTO {
        authTokenApi.checkToken(token)
        val pipelineTemplateList = templateDao.listTemplate(
            dslContext = dslContext,
            projectId = projectId,
            includePublicFlag = null,
            templateType = null,
            templateIdList = ids,
            storeFlag = null,
            offset = null,
            limit = null,
            queryModelFlag = true
        )
        val result = FetchInstanceInfo()
        if (pipelineTemplateList == null || pipelineTemplateList.isEmpty()) {
            logger.info("$ids does not match to pipeline template")
            return result.buildFetchInstanceFailResult()
        }

        val entityInfo = mutableListOf<InstanceInfoDTO>()
        val tTemplate = TTemplate.T_TEMPLATE
        val templateId2Name = pipelineSettingDao.listPipelineNames(
            dslContext = dslContext,
            pipelineIds = pipelineTemplateList.map { it[tTemplate.ID] },
            projectId = projectId
        )
        pipelineTemplateList.map {
            val entity = InstanceInfoDTO()
            val templateId = it[tTemplate.ID]
            val name = templateId2Name[templateId] ?: return@map
            entity.id = templateId
            entity.iamApprover = arrayListOf(it[tTemplate.CREATOR])
            entity.displayName = name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${entityInfo.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthPipelineGroupService::class.java)
    }
}
