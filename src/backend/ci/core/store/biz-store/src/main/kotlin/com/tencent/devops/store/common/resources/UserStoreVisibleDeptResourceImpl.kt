package com.tencent.devops.store.common.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.UserStoreVisibleDeptResource
import com.tencent.devops.store.common.service.StoreVisibleDeptService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.visible.StoreVisibleDeptReq
import com.tencent.devops.store.pojo.common.visible.StoreVisibleDeptResp
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserStoreVisibleDeptResourceImpl @Autowired constructor(
    private val storeVisibleDeptService: StoreVisibleDeptService
) : UserStoreVisibleDeptResource {

    override fun addVisibleDept(userId: String, storeVisibleDeptReq: StoreVisibleDeptReq): Result<Boolean> {
        return storeVisibleDeptService.addVisibleDept(
            userId = userId,
            storeCode = storeVisibleDeptReq.storeCode,
            storeType = StoreTypeEnum.valueOf(storeVisibleDeptReq.storeType),
            deptInfos = storeVisibleDeptReq.deptInfos
        )
    }

    override fun getVisibleDept(
        userId: String,
        storeType: String,
        storeCode: String,
        deptStatusInfos: String?
    ): Result<StoreVisibleDeptResp?> {
        return storeVisibleDeptService.getVisibleDept(
            storeCode = storeCode,
            storeType = StoreTypeEnum.valueOf(storeType),
            deptStatusInfos = deptStatusInfos
        )
    }

    override fun deleteVisibleDept(
        userId: String,
        storeType: String,
        storeCode: String,
        deptIds: String
    ): Result<Boolean> {
        return storeVisibleDeptService.deleteVisibleDept(
            userId = userId,
            storeCode = storeCode,
            storeType = StoreTypeEnum.valueOf(storeType),
            deptIds = deptIds
        )
    }
}
