package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.auth.pojo.vo.UserAndDeptInfoVo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwUserManagementResourceV4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwUserManagementResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwUserManagementResourceV4 {

    override fun getUserInfo(
        appCode: String?,
        apigwType: String?,
        userId: String
    ): Result<UserAndDeptInfoVo?> {
        logger.info("OPENAPI_GET_USER_INFO_V4|$appCode|$userId")
        return client.get(ServiceDeptResource::class).getUserInfo(
            userId = userId,
            name = userId
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ApigwUserManagementResourceV4Impl::class.java)
    }
}
