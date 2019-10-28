package com.tencent.devops.openapi.resources

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.ApigwProjectResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.ServiceTxProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwProjectResourceImpl @Autowired constructor(private val client: Client) : ApigwProjectResource {
    override fun getProjectByGroup(
        userId: String,
        bgName: String?,
        deptName: String?,
        centerName: String
    ): com.tencent.devops.project.pojo.Result<List<ProjectVO>> {
        logger.info("Get  projects info by group ,userId:$userId,bgName:$bgName,deptName:$deptName,centerName:$centerName")
        return client.get(ServiceTxProjectResource::class).getProjectByGroup(
            userId = userId,
            bgName = bgName,
            deptName = deptName,
            centerName = centerName
        )
    }

    override fun getProject(userId: String, projectId: String): com.tencent.devops.project.pojo.Result<ProjectVO?> {
        logger.info("Get a project info ,projectId:$projectId")
        return client.get(ServiceProjectResource::class).get(projectId)
    }

    override fun getProjectByUser(userId: String): com.tencent.devops.project.pojo.Result<List<ProjectVO>> {
        logger.info("Get user's project info ,userId:$userId")
        return client.get(ServiceProjectResource::class).getProjectByUser(userId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwProjectResourceImpl::class.java)
    }
}