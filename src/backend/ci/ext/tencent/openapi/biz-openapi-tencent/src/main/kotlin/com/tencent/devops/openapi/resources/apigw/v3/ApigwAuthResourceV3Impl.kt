package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceUserBlackListResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.pojo.BlackListInfo
import com.tencent.devops.openapi.api.apigw.pojo.WesecResult
import com.tencent.devops.openapi.api.apigw.v3.ApigwAuthResourceV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthResourceV3Impl @Autowired constructor(
    val client: Client,
    val tokenCheckService: ClientTokenService
) : ApigwAuthResourceV3 {

    override fun validateUserResourcePermission(
        appCode: String?,
        apigwType: String?,
        userId: String,
        action: String,
        projectId: String,
        resourceCode: String,
        resourceType: String
    ): Result<Boolean> {
        return Result(client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenCheckService.getSystemToken()!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = resourceCode,
            resourceType = resourceType,
            relationResourceType = null,
            action = action
        ).data ?: false
        )
    }

    override fun blackListUser(appCode: String?, apigwType: String?, blackList: BlackListInfo): WesecResult {
        logger.info("blackListUser $blackList")
        val method = blackList.method
        try {
            val result = when (method) {
                "disable" -> {
                    client.get(ServiceUserBlackListResource::class)
                        .createBlackListUser(userId = blackList.username, remark = "iegwesec execute")
                }
                "enable" -> {
                    client.get(ServiceUserBlackListResource::class).removeBlackListUser(blackList.username)
                }
                else -> return WesecResult(
                    result = false,
                    code = -1,
                    message = "invalid method $method"
                )
            }

            return if (result.isOk()) {
                WesecResult(true, 0, null)
            } else {
                WesecResult(false, -1, "server execute not OK")
            }
        } catch (e: Exception) {
            logger.warn("blackList execute fail: $e")
            return WesecResult(
                result = false,
                code = -1,
                message = "server fail: ${e.message}"
            )
        }
    }

    override fun blackListUser(appCode: String?, apigwType: String?): WesecResult {
        try {
            val blackList = client.get(ServiceUserBlackListResource::class).blackList()
            return WesecResult(
                result = true,
                code = 0,
                message = "",
                data = blackList.data
            )
        } catch (e: Exception) {
            logger.warn("blackList list fail: $e")
            return WesecResult(
                result = false,
                code = -1,
                message = "server fail: ${e.message}"
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(ApigwAuthResourceV3Impl::class.java)
    }
}
