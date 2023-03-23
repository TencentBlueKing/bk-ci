package com.tencent.devops.project.service.impl

import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.auth.api.BSAuthTokenApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.service.ProjectExtService
import com.tencent.devops.project.service.ProjectPaasCCService
import com.tencent.devops.project.service.ProjectTagService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList")
class TxProjectExtServiceImpl(
    private val bkRepoClient: BkRepoClient,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val projectPaasCCService: ProjectPaasCCService,
    private val bsAuthTokenApi: BSAuthTokenApi,
    private val projectDispatcher: ProjectDispatcher,
    private val projectTagService: ProjectTagService,
    val bkTag: BkTag
) : ProjectExtService {

    companion object {
        private val logger = LoggerFactory.getLogger(TxProjectExtServiceImpl::class.java)
    }

    @Value("\${tag.rbac:#{null}}")
    private val rbacTag: String? = null

    override fun createExtProjectInfo(
        userId: String,
        projectId: String,
        accessToken: String?,
        projectCreateInfo: ProjectCreateInfo,
        createExtInfo: ProjectCreateExtInfo,
        logoAddress: String?
    ) {
        // 添加repo项目
        val createSuccess = bkRepoClient.createBkRepoResource(userId, projectCreateInfo.englishName)
        logger.info("create bkrepo project ${projectCreateInfo.englishName} success: $createSuccess")

        if (createExtInfo.needAuth!!) {
            val newAccessToken = if (accessToken.isNullOrBlank()) {
                bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
            } else accessToken
            // 添加paas项目
            projectPaasCCService.createPaasCCProject(
                userId = userId,
                projectId = projectId,
                accessToken = newAccessToken,
                projectCreateInfo = projectCreateInfo
            )
        }
        val tag = bkTag.getLocalTag()
        // rbac环境创建的项目,需要指定路由
        if (rbacTag != null && tag.contains(rbacTag)) {
            projectTagService.updateTagByProject(projectCode = projectId, tag = bkTag.getLocalTag())
        }
        // 工蜂CI项目不会添加paas项目，但也需要广播
        projectDispatcher.dispatch(
            ProjectCreateBroadCastEvent(
                userId = userId,
                projectId = projectId,
                projectInfo = projectCreateInfo
            )
        )
    }
}
