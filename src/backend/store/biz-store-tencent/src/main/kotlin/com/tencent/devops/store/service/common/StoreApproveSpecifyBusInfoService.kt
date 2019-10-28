package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreApproveRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

/**
 * store组件具体审批业务的审批逻辑类
 * author: carlyin
 * since: 2019-08-20
 */
abstract class StoreApproveSpecifyBusInfoService {

    /**
     * store组件具体审批业务的审批逻辑处理
     */
    abstract fun approveStoreSpecifyBusInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        approveId: String,
        storeApproveRequest: StoreApproveRequest
    ): Result<Boolean>

    /**
     * 获取store组件具体审批业务的参数
     */
    abstract fun getBusAdditionalParams(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        approveId: String
    ): Map<String, String>?
}
