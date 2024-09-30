package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.api.service.ServiceResourceGroupResource
import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.ProjectPermissionInfoVO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAuthProjectResourceV4
import com.tencent.devops.project.pojo.ProjectCreateUserInfo
import com.tencent.devops.project.pojo.ProjectDeleteUserInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthProjectResourceV4Impl @Autowired constructor(
    val tokenService: ClientTokenService,
    val client: Client
) : ApigwAuthProjectResourceV4 {
    companion object {
        val logger = LoggerFactory.getLogger(ApigwAuthProjectResourceV4Impl::class.java)
    }

    override fun getProjectPermissionInfo(
        appCode: String?,
        apigwType: String?,
        projectId: String
    ): Result<ProjectPermissionInfoVO> {
        logger.info("OPENAPI_AUTH_PROJECT_RESOURCE_V4 getProjectPermissionInfo|$appCode|$projectId")
        return client.get(ServiceProjectAuthResource::class).getProjectPermissionInfo(
            token = tokenService.getSystemToken(),
            projectCode = projectId
        )
    }

    override fun getResourceGroupUsers(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        resourceType: AuthResourceType,
        resourceCode: String,
        group: BkAuthGroup?
    ): Result<List<String>> {
        logger.info("OPENAPI_AUTH_PROJECT_RESOURCE_V4 getResourceGroupUsers|$appCode|$projectId")
        return client.get(ServiceResourceMemberResource::class).getResourceGroupMembers(
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            resourceType = resourceType.value,
            resourceCode = resourceCode,
            group = group
        )
    }

    override fun getProjectGroupAndUserList(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String
    ): Result<List<BkAuthGroupAndUserList>> {
        logger.info("OPENAPI_AUTH_PROJECT_RESOURCE_V4 getResourceGroupUsers|$appCode|$userId|$projectId")
        return client.get(ServiceProjectAuthResource::class).getProjectGroupAndUserList(
            token = tokenService.getSystemToken(),
            projectCode = projectId
        )
    }

    override fun getGroupPermissionDetail(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        groupId: Int
    ): Result<Map<String, List<GroupPermissionDetailVo>>> {
        logger.info("OPENAPI_AUTH_PROJECT_RESOURCE_V4 getGroupPermissionDetail|$appCode|$userId|$projectId")
        return client.get(ServiceResourceGroupResource::class).getGroupPermissionDetail(
            projectCode = projectId,
            groupId = groupId
        )
    }

    override fun batchAddResourceGroupMembers(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        createInfo: ProjectCreateUserInfo
    ): Result<Boolean> {
        logger.info(
            "OPENAPI_AUTH_PROJECT_RESOURCE_V4 batchAddResourceGroupMembers " +
                " |$appCode|$userId|$projectId|$createInfo"
        )
        return client.get(ServiceResourceMemberResource::class).batchAddResourceGroupMembers(
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            projectCreateUserInfo = createInfo
        )
    }

    override fun batchDeleteResourceGroupMembers(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        deleteInfo: ProjectDeleteUserInfo
    ): Result<Boolean> {
        logger.info(
            "OPENAPI_AUTH_PROJECT_RESOURCE_V4 batchDeleteResourceGroupMembers" +
                "|$appCode|$userId|$projectId|$deleteInfo"
        )
        return client.get(ServiceResourceMemberResource::class).batchDeleteResourceGroupMembers(
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            projectDeleteUserInfo = deleteInfo
        )
    }

    override fun createGroupByGroupCode(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        resourceType: String,
        groupCode: BkAuthGroup
    ): Result<Int> {
        logger.info(
            "OPENAPI_AUTH_PROJECT_RESOURCE_V4 createGroupByGroupCode " +
                " |$appCode|$userId|$projectId|$resourceType|$groupCode"
        )
        return client.get(ServiceResourceGroupResource::class).createGroupByGroupCode(
            projectCode = projectId,
            resourceType = resourceType,
            groupCode = groupCode
        )
    }

    override fun createGroup(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        groupAddDTO: GroupAddDTO
    ): Result<Int> {
        logger.info("OPENAPI_AUTH_PROJECT_RESOURCE_V4 createGroup|$appCode|$userId|$projectId|$groupAddDTO")
        return client.get(ServiceResourceGroupResource::class).createGroup(
            projectCode = projectId,
            groupAddDTO = groupAddDTO
        )
    }

    override fun deleteGroup(
        appCode: String?,
        apigwType: String?,
        userId: String?,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_PROJECT_RESOURCE_V4 deleteGroup|$appCode|$userId|$projectId|$resourceType|$groupId")
        return client.get(ServiceResourceGroupResource::class).deleteGroup(
            projectCode = projectId,
            resourceType = resourceType,
            groupId = groupId
        )
    }
}
