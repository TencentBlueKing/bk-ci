package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.dto.HandoverDetailDTO
import com.tencent.devops.auth.pojo.dto.HandoverOverviewCreateDTO
import com.tencent.devops.auth.pojo.enum.HandoverType
import com.tencent.devops.auth.pojo.request.HandoverDetailsQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewUpdateReq
import com.tencent.devops.auth.pojo.vo.HandoverAuthorizationDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverGroupDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverOverviewVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.common.api.model.SQLPage

interface PermissionHandoverApplicationService {
    /**
     * 创建权限交接申请单
     * */
    fun createHandoverApplication(
        overview: HandoverOverviewCreateDTO,
        details: List<HandoverDetailDTO>
    ): String

    /**
     * 生成流程单号
     * */
    fun generateFlowNo(): String

    /**
     * 更新权限交接申请单
     * */
    fun updateHandoverApplication(overview: HandoverOverviewUpdateReq)

    /**
     * 根据流程单号获取权限交接总览
     * */
    fun getHandoverOverview(flowNo: String): HandoverOverviewVo

    /**
     * 权限交接总览列表
     * */
    fun listHandoverOverviews(queryRequest: HandoverOverviewQueryReq): SQLPage<HandoverOverviewVo>

    /**
     * 获取交接单详情
     * */
    fun listHandoverDetails(
        projectCode: String,
        flowNo: String,
        resourceType: String? = null,
        handoverType: HandoverType? = null
    ): List<HandoverDetailDTO>

    /**
     * 获取用户在项目下正在交接的组/授权
     * */
    fun listMemberHandoverDetails(
        projectCode: String,
        memberId: String,
        handoverType: HandoverType,
        resourceType: String? = null
    ): List<HandoverDetailDTO>

    /**
     * 获取交接单中授权相关
     * */
    fun listAuthorizationsOfHandoverApplication(
        queryReq: HandoverDetailsQueryReq
    ): SQLPage<HandoverAuthorizationDetailVo>

    /**
     * 获取交接单中用户组相关
     * */
    fun listGroupsOfHandoverApplication(queryReq: HandoverDetailsQueryReq): SQLPage<HandoverGroupDetailVo>

    /**
     * 根据资源类型进行分类
     * */
    fun getResourceType2CountOfHandoverApplication(flowNo: String): List<ResourceType2CountVo>
}
