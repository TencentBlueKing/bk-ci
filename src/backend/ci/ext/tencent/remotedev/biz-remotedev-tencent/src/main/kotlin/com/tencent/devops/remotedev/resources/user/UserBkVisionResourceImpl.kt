package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserBkVisionResource
import com.tencent.devops.remotedev.pojo.bkvision.BkVisionDatasetQueryBody
import com.tencent.devops.remotedev.pojo.bkvision.BkVisionResp
import com.tencent.devops.remotedev.pojo.bkvision.QueryFieldDataBody
import com.tencent.devops.remotedev.pojo.bkvision.QueryVariableDataBody
import com.tencent.devops.remotedev.service.BKVisionService
import com.tencent.devops.remotedev.service.PermissionService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserBkVisionResourceImpl @Autowired constructor(
    private val permissionService: PermissionService,
    private val bkVisionService: BKVisionService
) : UserBkVisionResource {

    override fun metaQuery(userId: String, projectId: String, shareId: String, type: String): BkVisionResp {
        permissionService.checkUserManager(userId, projectId)
        return bkVisionService.metaQuery(shareId, type)
    }

    override fun datasetQuery(userId: String, projectId: String, data: BkVisionDatasetQueryBody): BkVisionResp {
        permissionService.checkUserManager(userId, projectId)
        return bkVisionService.queryDataset(data)
    }

    override fun queryFieldData(
        userId: String,
        projectId: String,
        uid: String,
        data: QueryFieldDataBody
    ): BkVisionResp {
        permissionService.checkUserManager(userId, projectId)
        return bkVisionService.queryFieldData(uid, data)
    }

    override fun queryVariableData(userId: String, projectId: String, data: QueryVariableDataBody): BkVisionResp {
        permissionService.checkUserManager(userId, projectId)
        return bkVisionService.queryVariableData(data)
    }
}
