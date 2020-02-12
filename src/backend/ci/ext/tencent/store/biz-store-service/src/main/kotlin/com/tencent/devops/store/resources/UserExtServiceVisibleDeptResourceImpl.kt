package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceVisibleDeptResource
import com.tencent.devops.store.pojo.ExtsionServiceVisibleDeptReq
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceVisibleDeptResourceImpl @Autowired constructor(
    val storeVisibleDeptService: StoreVisibleDeptService
): UserExtServiceVisibleDeptResource {

    override fun addVisibleDept(userId: String, serviceVisibleDeptRequest: ExtsionServiceVisibleDeptReq): Result<Boolean> {
        return storeVisibleDeptService.addVisibleDept(userId, serviceVisibleDeptRequest.serviceCode, serviceVisibleDeptRequest.deptInfos, StoreTypeEnum.SERVICE)
    }

    override fun deleteVisibleDept(userId: String, atomCode: String, deptIds: String): Result<Boolean> {
        return storeVisibleDeptService.deleteVisibleDept(userId, atomCode, deptIds, StoreTypeEnum.SERVICE)
    }

    override fun getVisibleDept(atomCode: String): Result<StoreVisibleDeptResp?> {
        return storeVisibleDeptService.getVisibleDept(atomCode, StoreTypeEnum.SERVICE, null)
    }
}