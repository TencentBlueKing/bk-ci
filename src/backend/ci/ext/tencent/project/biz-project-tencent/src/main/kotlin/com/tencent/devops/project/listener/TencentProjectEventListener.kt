package com.tencent.devops.project.listener

import com.tencent.devops.common.auth.api.BSAuthTokenApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.project.pojo.ProjectUpdateLogoInfo
import com.tencent.devops.project.pojo.mq.ProjectBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent
import com.tencent.devops.project.service.ProjectPaasCCService
import com.tencent.devops.project.service.impl.AbsOpProjectServiceImpl.Companion.logger
import org.springframework.beans.factory.annotation.Autowired

/**
 * deng
 * 2019-12-17
 */
@Suppress("UNUSED")
class TencentProjectEventListener @Autowired constructor(
    val projectPaasCCService: ProjectPaasCCService,
    val bsAuthTokenApi: BSAuthTokenApi,
    val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode
) : ProjectEventListener {

    override fun execute(event: ProjectBroadCastEvent) {
        try {
            when (event) {
                is ProjectCreateBroadCastEvent -> {
                    onReceiveProjectCreate(event)
                }
                is ProjectUpdateBroadCastEvent -> {
                    onReceiveProjectUpdate(event)
                }
                is ProjectUpdateLogoBroadCastEvent -> {
                    onReceiveProjectUpdateLogo(event)
                }
            }
        } catch (ex: Exception) {
            logger.error("project listener execute error", ex)
        }
    }

    override fun onReceiveProjectCreate(event: ProjectCreateBroadCastEvent) {
        val accessToken = bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
        // 过渡期间让新建项目直接设置为灰度v2
//        opProjectService.setGrayProject(projectCodeList = listOf(event.projectInfo.englishName), operateFlag = 1)
        projectPaasCCService.createPaasCCProject(
            userId = event.userId,
            projectId = event.projectId,
            accessToken = accessToken,
            projectCreateInfo = event.projectInfo
        )
    }

    override fun onReceiveProjectUpdate(event: ProjectUpdateBroadCastEvent) {
        val accessToken = bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
        projectPaasCCService.updatePaasCCProject(
            userId = event.userId,
            projectId = event.projectId,
            projectUpdateInfo = event.projectInfo,
            accessToken = accessToken
        )
    }

    override fun onReceiveProjectUpdateLogo(event: ProjectUpdateLogoBroadCastEvent) {
        val accessToken = bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)

        val projectUpdateLogoInfo = ProjectUpdateLogoInfo(
            logo_addr = event.logoAddr,
            updator = event.userId
        )
        projectPaasCCService.updatePaasCCProjectLogo(
            userId = event.userId,
            projectId = event.projectId,
            accessToken = accessToken,
            projectUpdateLogoInfo = projectUpdateLogoInfo
        )
    }
}