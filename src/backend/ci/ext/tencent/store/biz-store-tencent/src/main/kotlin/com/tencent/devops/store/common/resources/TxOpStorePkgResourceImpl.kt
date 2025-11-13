package com.tencent.devops.store.common.resources

import TxOpStorePkgResource
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.common.service.TxStorePkgService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.util.ThreadPoolUtil

@RestResource
class TxOpStorePkgResourceImpl @Autowired constructor(
    private val txStorePkgService: TxStorePkgService
) : TxOpStorePkgResource {

    override fun updatePackageSha256(userId: String, storeType: StoreTypeEnum, pageSize: Int): Result<Boolean> {
        val executor = ThreadPoolUtil.getThreadPoolExecutor(
            corePoolSize = 1,
            maximumPoolSize = 1,
            threadNamePrefix = "op-updatePackageSha256-%d"
        )
        ThreadPoolUtil.submitAction(
            executor = executor,
            actionTitle = "updatePackageSha256",
            action = {
                try {
                    txStorePkgService.updatePackageSha256(userId, storeType, pageSize)
                } finally {
                    executor.shutdown()
                }
            }
        )
        return Result(true)
    }
}