package com.tencent.devops.auth.resources.user

import com.tencent.devops.auth.api.user.UserAuthHandoverResource
import com.tencent.devops.auth.pojo.request.HandoverDetailsQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewUpdateReq
import com.tencent.devops.auth.pojo.vo.HandoverAuthorizationDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverGroupDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverOverviewVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.service.PermissionAuthorizationService
import com.tencent.devops.auth.service.iam.PermissionHandoverService
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverConditionRequest
import com.tencent.devops.common.web.RestResource

@RestResource
class UserAuthHandoverResourceImpl(
    private val permissionAuthorizationService: PermissionAuthorizationService,
    private val permissionManageFacadeService: PermissionManageFacadeService,
    private val permissionHandoverService: PermissionHandoverService
) : UserAuthHandoverResource {
    override fun handoverAuthorizationsApplication(
        userId: String,
        projectId: String,
        condition: ResourceAuthorizationHandoverConditionRequest
    ): Result<Boolean> {
        return Result(
            permissionAuthorizationService.handoverAuthorizationsApplication(
                operator = userId,
                projectCode = projectId,
                condition = condition
            )
        )
    }

    override fun listHandoverOverviews(
        userId: String,
        queryRequest: HandoverOverviewQueryReq
    ): Result<SQLPage<HandoverOverviewVo>> {
        return Result(permissionHandoverService.listHandoverOverviews(queryRequest = queryRequest))
    }

    override fun getResourceType2CountOfHandover(
        userId: String,
        flowNo: String
    ): Result<List<ResourceType2CountVo>> {
        return Result(permissionHandoverService.getResourceType2CountOfHandover(flowNo = flowNo))
    }

    override fun listAuthorizationsOfHandover(
        userId: String,
        queryReq: HandoverDetailsQueryReq
    ): Result<SQLPage<HandoverAuthorizationDetailVo>> {
        return Result(permissionHandoverService.listAuthorizationsOfHandover(queryReq = queryReq))
    }

    override fun listGroupsOfHandover(
        userId: String,
        queryReq: HandoverDetailsQueryReq
    ): Result<SQLPage<HandoverGroupDetailVo>> {
        return Result(permissionHandoverService.listGroupsOfHandover(queryReq = queryReq))
    }

    override fun handleHanoverApplication(userId: String, request: HandoverOverviewUpdateReq): Result<Boolean> {
        return Result(permissionManageFacadeService.handleHanoverApplication(request = request))
    }
}
