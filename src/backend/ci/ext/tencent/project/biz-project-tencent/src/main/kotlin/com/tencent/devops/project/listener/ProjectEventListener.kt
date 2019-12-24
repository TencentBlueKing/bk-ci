package com.tencent.devops.project.listener

import com.tencent.devops.common.auth.api.BSAuthTokenApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.event.listener.Listener
import com.tencent.devops.project.pojo.ProjectUpdateLogoInfo
import com.tencent.devops.project.pojo.mq.ProjectBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent
import com.tencent.devops.project.service.ProjectPaasCCService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * deng
 * 2019-12-17
 */
@Component
class ProjectEventListener @Autowired constructor(
    val projectPaasCCService: ProjectPaasCCService,
    val bsAuthTokenApi: BSAuthTokenApi,
    val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode
) : Listener<ProjectBroadCastEvent> {

    override fun execute(event: ProjectBroadCastEvent) {
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
    }

    fun onReceiveProjectCreate(event: ProjectCreateBroadCastEvent) {
        val accessToken = bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
        projectPaasCCService.createPaasCCProject(
            userId = event.userId,
            projectId = event.projectId,
            accessToken = accessToken,
            projectCreateInfo = event.projectInfo
        )
    }

    fun onReceiveProjectUpdate(event: ProjectUpdateBroadCastEvent) {
        val accessToken = bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
        projectPaasCCService.updatePaasCCProject(
            userId = event.userId,
            projectId = event.projectId,
            projectUpdateInfo = event.projectInfo,
            accessToken = accessToken
        )
    }

    fun onReceiveProjectUpdateLogo(event: ProjectUpdateLogoBroadCastEvent) {
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