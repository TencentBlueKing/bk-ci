package com.tencent.devops.store.resources.common

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.OpStoreAuditResource
import com.tencent.devops.store.pojo.common.StoreApproveRequest
import com.tencent.devops.store.pojo.common.VisibleAuditInfo
import com.tencent.devops.store.service.atom.OpStoreAuditConfService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpStoreAuditResourceImpl @Autowired constructor(
    private val opStoreAuditConfService: OpStoreAuditConfService
) : OpStoreAuditResource {

    override fun approveVisibleDept(userId: String, id: String, storeApproveRequest: StoreApproveRequest): Result<Boolean> {
        return opStoreAuditConfService.approveVisibleDept(userId, id, storeApproveRequest)
    }

    override fun getAllAuditConf(
        userId: String,
        storeName: String?,
        storeType: Byte?,
        status: Byte?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<VisibleAuditInfo>> {
        return opStoreAuditConfService.getAllAuditConf(storeName, storeType, status, page, pageSize)
    }

    override fun deleteAuditConf(userId: String, id: String): Result<Boolean> {
        return opStoreAuditConfService.deleteAuditConf(id)
    }
}