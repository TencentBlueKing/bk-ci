package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverResult
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus

interface AuthAuthorizationApi {
    /**
     * 批量重置资源授权人
     * @param projectId 项目ID
     * @param resourceAuthorizationHandoverList 资源授权交接列表
     */
    fun batchModifyHandoverFrom(
        projectId: String,
        resourceAuthorizationHandoverList: List<ResourceAuthorizationHandoverDTO>
    )

    /**
     * 新增资源授权
     * @param projectId 项目ID
     * @param resourceAuthorizationList 资源授权列表
     */
    fun addResourceAuthorization(
        projectId: String,
        resourceAuthorizationList: List<ResourceAuthorizationDTO>
    )

    /**
     * 重置资源授权
     * @param projectId 项目ID
     * @param preCheck 是否为预检查，若是则只做检查，不做交接
     * @param resourceAuthorizationHandoverDTOs 数据
     * @param handoverResourceAuthorization 业务方交接授权逻辑
     */
    fun resetResourceAuthorization(
        projectId: String,
        preCheck: Boolean,
        resourceAuthorizationHandoverDTOs: List<ResourceAuthorizationHandoverDTO>,
        handoverResourceAuthorization: (
            preCheck: Boolean,
            resourceAuthorizationHandoverDTO: ResourceAuthorizationHandoverDTO
        ) -> ResourceAuthorizationHandoverResult
    ): Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>>
}
