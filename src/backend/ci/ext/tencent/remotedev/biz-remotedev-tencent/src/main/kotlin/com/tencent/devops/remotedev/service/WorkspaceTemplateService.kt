package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceTemplateDao
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WorkspaceTemplateService  @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val workspaceTemplateDao: WorkspaceTemplateDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceTemplateService::class.java)
    }

    // 新增模板
    fun addWorkspaceTemplate(userId: String, workspaceTemplate: WorkspaceTemplate): Boolean {
        logger.info("WorkspaceTemplateService|addWorkspaceTemplate|userId" +
            "|${userId}|workspaceTemplate|${workspaceTemplate}")
        // 校验 user信息是否存在
        checkCommonUser(userId)

        // 模板信息写入DB
        workspaceTemplateDao.createWorkspaceTemplate(
            userId = userId,
            workspaceTemplate = workspaceTemplate,
            dslContext = dslContext
        )

        return true
    }

    // 修改模板
    fun updateWorkspaceTemplate(
        userId: String,
        wsTemplateId: Long,
        workspaceTemplate: WorkspaceTemplate
    ): Boolean {
        logger.info("WorkspaceTemplateService|updateWorkspaceTemplate|userId|${userId}|" +
            "workspaceTemplateId|${wsTemplateId}|workspaceTemplate|${workspaceTemplate}")
        // 校验 user信息是否存在
        checkCommonUser(userId)

        // 更新模板信息
        workspaceTemplateDao.updateWorkspaceTemplate(
            wsTemplateId = wsTemplateId,
            workspaceTemplate = workspaceTemplate,
            dslContext = dslContext
        )

        return true
    }

    // 删除模板
    fun deleteWorkspaceTemplate(
        userId: String,
        wsTemplateId: Long
    ): Boolean {
        logger.info("WorkspaceTemplateService|deleteWorkspaceTemplate|userId|${userId}|wsTemplateId|${wsTemplateId}")
        // 校验 user信息是否存在
        checkCommonUser(userId)
        // 删除模板信息
        workspaceTemplateDao.deleteWorkspaceTemplate(
            wsTemplateId = wsTemplateId,
            dslContext = dslContext
        )

        return true
    }

    // 获取工作空间模板
    fun getWorkspaceTemplateList(
        userId: String
    ): List<WorkspaceTemplate> {
        logger.info("WorkspaceTemplateService|getWorkspaceTemplateList|userId|${userId}")
        checkCommonUser(userId)
        val result = mutableListOf<WorkspaceTemplate>()
         workspaceTemplateDao.queryWorkspaceTemplate(
             wsTemplateId = null,
             dslContext = dslContext
        ).forEach {
             result.add(
                 WorkspaceTemplate(
                     wsTemplateId = it.id.toInt(),
                     image = it.image,
                     name = it.name,
                     source = it.source,
                     logo = it.logo,
                     description = it.description
                 )
             )
         }
        return result
    }

    // 校验用户是否存在
    fun checkCommonUser(userId: String) {
        // get接口先查本地，再查tof
        val userResult = client.get(ServiceTxUserResource::class).get(userId)
        if (userResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.USER_NOT_EXISTS.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.USER_NOT_EXISTS.formatErrorMessage.format(userId)
            )
        }
    }

}
