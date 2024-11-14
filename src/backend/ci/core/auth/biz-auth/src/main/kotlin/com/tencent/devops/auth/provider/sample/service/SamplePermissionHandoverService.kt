package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.pojo.dto.HandoverDetailDTO
import com.tencent.devops.auth.pojo.dto.HandoverOverviewCreateDTO
import com.tencent.devops.auth.pojo.request.HandoverOverviewUpdateReq
import com.tencent.devops.auth.pojo.enum.HandoverType
import com.tencent.devops.auth.pojo.request.HandoverDetailsQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewQueryReq
import com.tencent.devops.auth.pojo.vo.HandoverAuthorizationDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverGroupDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverOverviewVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.service.iam.PermissionHandoverService
import com.tencent.devops.common.api.model.SQLPage

class SamplePermissionHandoverService : PermissionHandoverService {
    override fun createHandoverApplication(
        overview: HandoverOverviewCreateDTO,
        details: List<HandoverDetailDTO>
    ) {
        return
    }

    override fun generateTitle(groupCount: Int, authorizationCount: Int): String = ""

    override fun generateFlowNo(): String = ""

    override fun updateHandoverApplication(overview: HandoverOverviewUpdateReq) {
        return
    }

    override fun getHandoverOverview(flowNo: String): HandoverOverviewVo {
        TODO("Not yet implemented")
    }

    override fun listHandoverOverviews(queryRequest: HandoverOverviewQueryReq): SQLPage<HandoverOverviewVo> {
        return SQLPage(0, emptyList())
    }

    override fun listAuthorizationsOfHandover(
        queryReq: HandoverDetailsQueryReq
    ): SQLPage<HandoverAuthorizationDetailVo> {
        return SQLPage(0, emptyList())
    }

    override fun listGroupsOfHandover(queryReq: HandoverDetailsQueryReq): SQLPage<HandoverGroupDetailVo> {
        return SQLPage(0, emptyList())
    }

    override fun getResourceType2CountOfHandover(flowNo: String): List<ResourceType2CountVo> {
        return emptyList()
    }

    override fun listHandoverDetails(
        projectCode: String,
        flowNo: String,
        resourceType: String?,
        handoverType: HandoverType?
    ): List<HandoverDetailDTO> {
        return emptyList()
    }
}
