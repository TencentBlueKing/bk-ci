package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceRepositoryResource
import com.tencent.devops.store.service.ServiceRepositoryService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceRepositoryResourceImpl @Autowired constructor(
    val serviceRepositoryService: ServiceRepositoryService
) : UserExtServiceRepositoryResource {

    override fun changeServiceRepositoryUserInfo(
        userId: String,
        projectCode: String,
        serviceCode: String
    ): Result<Boolean> {
        return serviceRepositoryService.updateServiceRepositoryUserInfo(userId, projectCode, serviceCode)
    }

    override fun getReadme(userId: String, serviceCode: String): Result<String?> {
        return serviceRepositoryService.getReadMeFile(userId, serviceCode)
    }
}