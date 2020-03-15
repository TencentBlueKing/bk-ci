package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.service.ServiceProjectServiceResource
import com.tencent.devops.project.pojo.service.ServiceVO
import com.tencent.devops.project.service.ServiceProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceProjectServiceResourceImpl @Autowired constructor(
    private val serviceProjectService: ServiceProjectService
) : ServiceProjectServiceResource {
    override fun getServiceList(userId: String, projectId: String?): Result<List<ServiceVO>> {
        return serviceProjectService.getServiceList();
    }
}